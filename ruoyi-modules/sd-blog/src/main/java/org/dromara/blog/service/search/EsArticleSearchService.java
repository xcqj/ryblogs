package org.dromara.blog.service.search;

import cn.hutool.core.collection.CollUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HighlighterEncoder;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.blog.config.BlogEsConfig;
import org.dromara.blog.domain.vo.BlogPublicArticleListItemVo;
import org.dromara.blog.domain.vo.BlogSearchPageResult;
import org.dromara.blog.domain.vo.SdBlogTagVo;
import org.dromara.blog.es.ArticleIndexManager;
import org.dromara.blog.es.BlogArticleDoc;
import org.dromara.blog.mapper.SdBlogArticleMapper;
import org.dromara.blog.service.support.ArticleTagFiller;
import org.dromara.blog.service.support.EsSearchSupport;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ES 搜索实现（ROADMAP S16，契约 §6，替换 S8 的 MySQL LIKE）。
 * <p>
 * 查询链路：ES multi_match（title^3 / summary / content，ik_smart）+ 服务端高亮 →
 * 命中 id 按打分序回表 MySQL（复用 {@code selectPublicPage} 三条件底座，防 ES→DB
 * 竞态窗口内泄露刚被隐藏的文章）→ {@link EsSearchSupport} 按 ES 序重排 + 合并高亮 →
 * {@link ArticleTagFiller} 两段式填 tags。深翻页：pageNum 越窗用 {@code after} 游标
 * （search_after），游标由上一页响应 {@code nextAfter} 透传。
 * <p>
 * 降级：ES 异常（不可达/超时/查询失败）→ 回退 MySQL LIKE 实现（同样强制三条件，
 * 红线不破；响应无 highlight / nextAfter），日志限频防洪水。
 *
 * @author Xu
 * @date 2026-10-14
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Primary
@ConditionalOnProperty(prefix = "blog.search", name = "engine", havingValue = "es")
public class EsArticleSearchService implements ArticleSearchService {

    /** 标题相关度加权（演示可调；契约不承诺具体分值语义） */
    private static final int TITLE_BOOST = 3;

    /** 查询侧分析器（与 mapping 的 searchAnalyzer 一致，显式声明防默认 standard 分词） */
    private static final String QUERY_ANALYZER = "ik_smart";

    /** pageSize 缺省/越界兜底：默认 10，上限 100（防 from+size 打爆深页窗口） */
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    /** 高亮预/后标签（D36：命中词 <em> 包裹） */
    private static final String HL_PRE_TAG = "<em>";
    private static final String HL_POST_TAG = "</em>";

    /** 降级告警限频：同一 JVM 内最多每 10s 一条（毫秒时间戳） */
    private static final long FALLBACK_WARN_INTERVAL_MS = 10_000L;
    private final AtomicLong lastFallbackWarnAt = new AtomicLong(0);

    private final ElasticsearchClient blogElasticsearchClient;
    private final SdBlogArticleMapper sdBlogArticleMapper;
    private final ArticleTagFiller articleTagFiller;
    private final MysqlArticleSearchService mysqlArticleSearchService;

    /**
     * {@inheritDoc}
     */
    @Override
    public BlogSearchPageResult<BlogPublicArticleListItemVo> search(String keyword, PageQuery pageQuery, String after) {
        try {
            return doEsSearch(keyword, pageQuery, after);
        } catch (Exception e) {
            warnFallbackThrottled(e);
            // 降级 MySQL LIKE：三条件同样内建；after 游标无意义，按 pageNum 翻页
            return mysqlArticleSearchService.search(keyword, pageQuery, null);
        }
    }

    private BlogSearchPageResult<BlogPublicArticleListItemVo> doEsSearch(String keyword, PageQuery pageQuery,
                                                                        String after) throws Exception {
        int pageSize = normalizePageSize(pageQuery);
        EsSearchSupport.CursorValues cursor = EsSearchSupport.decodeCursor(after);
        boolean useCursor = cursor != null;
        int from = useCursor ? 0 : (normalizePageNum(pageQuery) - 1) * pageSize;
        if (!useCursor && (long) from + pageSize > BlogEsConfig.MAX_RESULT_WINDOW) {
            // 深页窗口之外且无游标：返回空页（契约 §6 深翻页条款：应改用 nextAfter）
            log.warn("[ES] 页码超出深翻页窗口（from={}, size={}, window={}），返回空页，建议改用 after 游标",
                from, pageSize, BlogEsConfig.MAX_RESULT_WINDOW);
            return new BlogSearchPageResult<>(List.of(), 0, null);
        }

        SearchResponse<BlogArticleDoc> resp = blogElasticsearchClient.search(s -> {
            s.index(BlogEsConfig.INDEX_ALIAS)
                .size(pageSize)
                .query(q -> q.multiMatch(mm -> mm
                    .query(keyword)
                    .fields(ArticleIndexManager.FIELD_TITLE + "^" + TITLE_BOOST,
                        ArticleIndexManager.FIELD_SUMMARY, ArticleIndexManager.FIELD_CONTENT)
                    .analyzer(QUERY_ANALYZER)
                    .type(TextQueryType.BestFields)))
                .sort(so -> so.score(sc -> sc.order(SortOrder.Desc)))
                .sort(so -> so.field(f -> f.field(ArticleIndexManager.FIELD_PUBLISHED_AT).order(SortOrder.Desc)))
                .sort(so -> so.field(f -> f.field(ArticleIndexManager.FIELD_ARTICLE_ID).order(SortOrder.Desc)))
                .highlight(h -> h
                    .encoder(HighlighterEncoder.Html)
                    .fields(ArticleIndexManager.FIELD_TITLE, hf -> hf
                        .preTags(HL_PRE_TAG).postTags(HL_POST_TAG).numberOfFragments(0))
                    .fields(ArticleIndexManager.FIELD_SUMMARY, hf -> hf
                        .preTags(HL_PRE_TAG).postTags(HL_POST_TAG).numberOfFragments(0))
                    .fields(ArticleIndexManager.FIELD_CONTENT, hf -> hf
                        .preTags(HL_PRE_TAG).postTags(HL_POST_TAG)
                        .numberOfFragments(3).fragmentSize(200)));
            if (useCursor) {
                s.searchAfter(List.of(FieldValue.of(cursor.score()),
                    FieldValue.of(cursor.publishedAtEpochMilli()),
                    FieldValue.of(cursor.articleId())));
            } else {
                s.from(from);
            }
            return s;
        }, BlogArticleDoc.class);

        List<Hit<BlogArticleDoc>> hits = resp.hits().hits();
        long total = resp.hits().total().value();
        if (CollUtil.isEmpty(hits)) {
            return new BlogSearchPageResult<>(List.of(), total, null);
        }

        // 命中序 + 高亮 + 下一页游标（最后一条的排序值）
        List<Long> hitOrderIds = new ArrayList<>(hits.size());
        Map<Long, Map<String, List<String>>> highlightMap = new HashMap<>();
        for (Hit<BlogArticleDoc> hit : hits) {
            Long articleId = Long.valueOf(hit.id());
            hitOrderIds.add(articleId);
            if (CollUtil.isNotEmpty(hit.highlight())) {
                highlightMap.put(articleId, hit.highlight());
            }
        }
        String nextAfter = buildNextCursor(hits.getLast());

        // 回表：selectPublicPage 三条件内建（ES→DB 竞态窗口内刚隐藏/删除的文章不会出现）
        PageResult<BlogPublicArticleListItemVo> page = sdBlogArticleMapper.selectPublicPage(
            new PageQuery(hitOrderIds.size(), 1).build(), hitOrderIds, null);
        List<BlogPublicArticleListItemVo> rows = EsSearchSupport.reorderByHitOrder(
            page.getRecords(), hitOrderIds, BlogPublicArticleListItemVo::getArticleId);
        rows.forEach(row -> EsSearchSupport.applyHighlight(row, highlightMap.get(row.getArticleId())));

        // tags 两段式批量填充（与 §5.1/搜索同源）
        if (CollUtil.isNotEmpty(rows)) {
            Map<Long, List<SdBlogTagVo>> tagMap = articleTagFiller.queryTagsByArticleIds(hitOrderIds);
            rows.forEach(row -> row.setTags(tagMap.getOrDefault(row.getArticleId(), List.of())));
        }
        return new BlogSearchPageResult<>(rows, total, nextAfter);
    }

    /**
     * 由最后一条命中构造下一页游标：score（hit.score）+ publishedAt（doc ISO → epochMilli）
     * + articleId——与排序 [_score desc, publishedAt desc, articleId desc] 的 sort 值一一对应。
     */
    private String buildNextCursor(Hit<BlogArticleDoc> lastHit) {
        BlogArticleDoc doc = lastHit.source();
        if (doc == null || doc.getArticleId() == null || lastHit.score() == null
            || doc.getPublishedAt() == null || doc.getPublishedAt().isBlank()) {
            return null;
        }
        long epochMilli = LocalDateTime.parse(doc.getPublishedAt())
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return EsSearchSupport.encodeCursor(lastHit.score(), epochMilli, doc.getArticleId());
    }

    private int normalizePageNum(PageQuery pageQuery) {
        Integer pageNum = pageQuery.getPageNum();
        return pageNum == null || pageNum <= 0 ? PageQuery.DEFAULT_PAGE_NUM : pageNum;
    }

    private int normalizePageSize(PageQuery pageQuery) {
        Integer pageSize = pageQuery.getPageSize();
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private void warnFallbackThrottled(Exception e) {
        long now = System.currentTimeMillis();
        long last = lastFallbackWarnAt.get();
        if (now - last >= FALLBACK_WARN_INTERVAL_MS && lastFallbackWarnAt.compareAndSet(last, now)) {
            log.warn("[ES] 搜索异常，降级 MySQL LIKE（限频告警）", e);
        }
    }

}
