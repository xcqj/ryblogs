package org.dromara.blog.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.blog.config.BlogEsConfig;
import org.dromara.blog.domain.SdBlogArticle;
import org.dromara.blog.domain.SdBlogArticleVersion;
import org.dromara.blog.domain.vo.SdBlogTagVo;
import org.dromara.blog.enums.BlogArticleStatus;
import org.dromara.blog.enums.BlogVisibility;
import org.dromara.blog.es.ArticleIndexDocBuilder;
import org.dromara.blog.es.ArticleIndexManager;
import org.dromara.blog.es.BlogArticleDoc;
import org.dromara.blog.mapper.SdBlogArticleMapper;
import org.dromara.blog.mapper.SdBlogArticleVersionMapper;
import org.dromara.blog.service.ArticleIndexService;
import org.dromara.blog.service.support.ArticleGuards;
import org.dromara.blog.service.support.ArticleTagFiller;
import org.dromara.blog.service.support.IndexReconcileSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文章 ES 索引同步实现（ROADMAP S16）。
 * <p>
 * 同步策略：文章变更事件（AFTER_COMMIT）驱动增量同步；失败/遗漏由定时对账兜底——
 * 最终一致，与 S15 阅读量增量丢失取舍同哲学：索引故障不阻断主业务。
 * <p>
 * 三口径同源（人工复核点）：增量 {@link #indexOrRemove} 的可见性判定走
 * {@link ArticleGuards#isPublicVisible}，初始全量与对账的 MySQL 侧查询用同一组
 * status/visibility 常量拼条件——任一处漏条件都会让非公共文章泄入搜索结果。
 *
 * @author Xu
 * @date 2026-10-14
 */
@Slf4j
@RequiredArgsConstructor
@Service
@ConditionalOnProperty(prefix = "blog.search", name = "engine", havingValue = "es")
public class ArticleIndexServiceImpl implements ArticleIndexService {

    /** keyset 分页批量（主表 + 版本 + 标签都按批走，防大 content 撑爆内存） */
    private static final int SYNC_BATCH_SIZE = 200;

    private final SdBlogArticleMapper sdBlogArticleMapper;
    private final SdBlogArticleVersionMapper sdBlogArticleVersionMapper;
    private final ArticleIndexManager articleIndexManager;
    private final ArticleTagFiller articleTagFiller;
    private final ElasticsearchClient blogElasticsearchClient;

    /**
     * 写入后立即刷新（默认 false：≤1s 自然刷新足够演示；true 会同步等 refresh 拖慢批量同步
     * 与事件监听所在线程，联调实测 20040 篇被拖到 20 分钟级）
     */
    @Value("${blog.es.refresh-immediate:false}")
    private boolean refreshImmediate;

    /**
     * 对账拉取 ES 全量文档 id 的每批条数
     */
    @Value("${blog.index.reconcile-batch-size:1000}")
    private int reconcileBatchSize;

    /**
     * {@inheritDoc}
     */
    @Override
    public void indexOrRemove(Long articleId) {
        try {
            // @TableLogic 天然过滤已软删文章：selectById 返回 null 即"应删文档"
            SdBlogArticle article = sdBlogArticleMapper.selectById(articleId);
            if (Objects.isNull(article) || !ArticleGuards.isPublicVisible(article)
                || Objects.isNull(article.getCurrentVersionId())) {
                articleIndexManager.deleteDoc(articleId, refreshImmediate);
                return;
            }
            SdBlogArticleVersion version = sdBlogArticleVersionMapper.selectById(article.getCurrentVersionId());
            if (Objects.isNull(version)) {
                // 无当前版本（理论不出现：发布要求有版本）——防御性删文档，不留脏数据
                articleIndexManager.deleteDoc(articleId, refreshImmediate);
                return;
            }
            Map<Long, List<SdBlogTagVo>> tagMap = articleTagFiller.queryTagsByArticleIds(List.of(articleId));
            List<String> tagNames = tagMap.getOrDefault(articleId, List.of()).stream()
                .map(SdBlogTagVo::getName).toList();
            BlogArticleDoc doc = ArticleIndexDocBuilder.from(article, version, tagNames);
            articleIndexManager.indexDoc(doc, refreshImmediate);
        } catch (Exception e) {
            // 索引失败不阻断主业务（事件在 AFTER_COMMIT 阶段，异常也不回传调用方）；对账任务兜底
            log.error("[ES] 索引同步失败，等待对账任务兜底修复, articleId={}", articleId, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialFullSync() {
        log.info("[ES] 初始全量同步开始（公共可见文章灌入索引 {}）", BlogEsConfig.INDEX_ALIAS);
        long lastId = 0;
        long total = 0;
        while (true) {
            List<SdBlogArticle> batch = publicArticleBatch(lastId);
            if (batch.isEmpty()) {
                break;
            }
            indexBatch(batch);
            total += batch.size();
            lastId = batch.getLast().getArticleId();
        }
        log.info("[ES] 初始全量同步完成，共 {} 篇", total);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Scheduled(cron = "${blog.index.reconcile-cron:0 0/30 * * * ?}")
    public void reconcile() {
        Set<Long> mysqlIds = allPublicArticleIds();
        Set<String> esIds = allEsDocIds();
        IndexReconcileSupport.Diff diff = IndexReconcileSupport.diff(mysqlIds, esIds);
        if (diff.isEmpty()) {
            log.debug("[ES] 对账无差异（MySQL {} 篇 / ES {} 篇）", mysqlIds.size(), esIds.size());
            return;
        }
        log.warn("[ES] 对账发现差异：待补索引 {} 篇，待删文档 {} 个", diff.toIndex().size(), diff.toRemove().size());
        // 补索引走批量（2 万级差异 bulk 秒级完成；逐条 indexOrRemove 会被单文档 RTT 拖到分钟级）
        List<Long> toIndex = diff.toIndex();
        for (int from = 0; from < toIndex.size(); from += SYNC_BATCH_SIZE) {
            List<Long> chunk = toIndex.subList(from, Math.min(from + SYNC_BATCH_SIZE, toIndex.size()));
            try {
                indexBatch(sdBlogArticleMapper.selectByIds(chunk));
            } catch (Exception e) {
                log.error("[ES] 对账补索引批次失败（下周期重试）, articleIds={}", chunk, e);
            }
        }
        for (String docId : diff.toRemove()) {
            try {
                articleIndexManager.deleteDoc(Long.valueOf(docId), refreshImmediate);
            } catch (NumberFormatException e) {
                log.warn("[ES] 对账删除跳过无法解析的文档 id: {}", docId);
            } catch (Exception e) {
                log.error("[ES] 对账删除文档失败, docId={}", docId, e);
            }
        }
    }

    /**
     * 公共可见文章主表批查（keyset 分页：articleId 游标，只取索引必需列）
     */
    private List<SdBlogArticle> publicArticleBatch(long afterArticleId) {
        return sdBlogArticleMapper.lambda()
            .select(SdBlogArticle::getArticleId, SdBlogArticle::getCurrentVersionId, SdBlogArticle::getPublishedAt)
            .eq(SdBlogArticle::getStatus, BlogArticleStatus.PUBLISHED.getCode())
            .eq(SdBlogArticle::getVisibility, BlogVisibility.PUBLIC.getCode())
            .gt(SdBlogArticle::getArticleId, afterArticleId)
            .orderByAsc(SdBlogArticle::getArticleId)
            .last("LIMIT " + SYNC_BATCH_SIZE)
            .list();
    }

    /**
     * 全量公共可见文章 id（keyset 分页，只查 id 列）
     */
    private Set<Long> allPublicArticleIds() {
        Set<Long> ids = new HashSet<>();
        long lastId = 0;
        while (true) {
            Set<Long> batch = sdBlogArticleMapper.lambda()
                .select(SdBlogArticle::getArticleId)
                .eq(SdBlogArticle::getStatus, BlogArticleStatus.PUBLISHED.getCode())
                .eq(SdBlogArticle::getVisibility, BlogVisibility.PUBLIC.getCode())
                .gt(SdBlogArticle::getArticleId, lastId)
                .orderByAsc(SdBlogArticle::getArticleId)
                .last("LIMIT " + SYNC_BATCH_SIZE)
                .list().stream().map(SdBlogArticle::getArticleId).collect(Collectors.toSet());
            if (batch.isEmpty()) {
                return ids;
            }
            ids.addAll(batch);
            lastId = batch.stream().max(Long::compareTo).orElse(lastId);
        }
    }

    /**
     * 批量入索引（版本、标签两段式批量查杜绝 N+1；写 ES 走 _bulk 单请求一批，
     * 批间不 refresh——2 万篇从 20 分钟级降到秒级，联调实测教训）
     */
    private void indexBatch(List<SdBlogArticle> articles) {
        if (CollUtil.isEmpty(articles)) {
            return;
        }
        List<Long> versionIds = articles.stream()
            .map(SdBlogArticle::getCurrentVersionId).filter(Objects::nonNull).distinct().toList();
        Map<Long, SdBlogArticleVersion> versionMap = new HashMap<>();
        if (!versionIds.isEmpty()) {
            for (SdBlogArticleVersion v : sdBlogArticleVersionMapper.lambda()
                .in(SdBlogArticleVersion::getVersionId, versionIds).list()) {
                versionMap.put(v.getVersionId(), v);
            }
        }
        List<Long> articleIds = articles.stream().map(SdBlogArticle::getArticleId).toList();
        Map<Long, List<SdBlogTagVo>> tagMap = articleTagFiller.queryTagsByArticleIds(articleIds);
        List<BlogArticleDoc> docs = new ArrayList<>(articles.size());
        for (SdBlogArticle article : articles) {
            SdBlogArticleVersion version = versionMap.get(article.getCurrentVersionId());
            if (Objects.isNull(version)) {
                continue;
            }
            List<String> tagNames = tagMap.getOrDefault(article.getArticleId(), List.of()).stream()
                .map(SdBlogTagVo::getName).toList();
            docs.add(ArticleIndexDocBuilder.from(article, version, tagNames));
        }
        try {
            articleIndexManager.bulkIndexDocs(docs, refreshImmediate);
        } catch (Exception e) {
            log.error("[ES] 批量同步失败（对账任务将兜底重试）, batchSize={}", docs.size(), e);
        }
    }

    /**
     * 全量 ES 文档 _id（按 articleId 升序 search_after 分页，不取 _source）
     */
    private Set<String> allEsDocIds() {
        Set<String> ids = new HashSet<>();
        List<FieldValue> searchAfter = null;
        while (true) {
            final List<FieldValue> after = searchAfter;
            SearchResponse<BlogArticleDoc> resp;
            try {
                resp = blogElasticsearchClient.search(s -> {
                    s.index(BlogEsConfig.INDEX_ALIAS)
                        .size(reconcileBatchSize)
                        .source(sc -> sc.fetch(false))
                        .sort(so -> so.field(f -> f
                            .field(ArticleIndexManager.FIELD_ARTICLE_ID)
                            .order(SortOrder.Asc)));
                    if (after != null) {
                        s.searchAfter(after);
                    }
                    return s;
                }, BlogArticleDoc.class);
            } catch (Exception e) {
                log.error("[ES] 对账拉取文档 id 失败（本轮跳过，下周期重试）", e);
                return ids;
            }
            List<Hit<BlogArticleDoc>> hits = resp.hits().hits();
            for (Hit<BlogArticleDoc> hit : hits) {
                ids.add(hit.id());
            }
            if (hits.size() < reconcileBatchSize) {
                return ids;
            }
            searchAfter = hits.getLast().sort();
        }
    }

}
