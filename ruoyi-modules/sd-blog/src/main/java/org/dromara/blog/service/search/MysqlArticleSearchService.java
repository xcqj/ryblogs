package org.dromara.blog.service.search;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.blog.domain.SdBlogArticle;
import org.dromara.blog.domain.SdBlogArticleVersion;
import org.dromara.blog.domain.vo.BlogPublicArticleListItemVo;
import org.dromara.blog.domain.vo.BlogSearchPageResult;
import org.dromara.blog.domain.vo.SdBlogTagVo;
import org.dromara.blog.enums.BlogArticleStatus;
import org.dromara.blog.enums.BlogVisibility;
import org.dromara.blog.mapper.SdBlogArticleMapper;
import org.dromara.blog.service.support.ArticleTagFiller;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.query.QueryBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * MySQL LIKE 搜索实现（契约 §6 / ROADMAP S8）。
 * <p>
 * 搜索字段：仅当前生效版本（join 条件即 version_id = current_version_id）的
 * title / summary / content 三字段 OR 模糊（D19 自决：技术博客正文命中最有价值，
 * MVP 数据量下 LIKE 全扫可接受，不上全文索引）；公共可见三条件与 §5.1 底座同口径强制内建。
 * <p>
 * S16：ES 成为默认引擎（{@code blog.search.engine=es}），本实现降级为两处角色——
 * ① {@code engine=mysql} 时的主实现；② ES 实现运行期异常时的兜底（{@link EsArticleSearchService} 直注本类）。
 * 无高亮能力（响应无 highlight）与深翻页游标（忽略 after，按 pageNum 翻页），属实现层差异，契约已登记。
 *
 * @author Xu
 * @date 2026-10-01
 */
@RequiredArgsConstructor
@Service
public class MysqlArticleSearchService implements ArticleSearchService {

    private final SdBlogArticleMapper sdBlogArticleMapper;
    private final ArticleTagFiller articleTagFiller;

    /**
     * 搜索公开文章：三字段 OR LIKE，published_at 倒序（{@code after} 游标仅 ES 实现生效，此处忽略）
     */
    @Override
    public BlogSearchPageResult<BlogPublicArticleListItemVo> search(String keyword, PageQuery pageQuery, String after) {
        Page<BlogPublicArticleListItemVo> page = QueryBuilder.lambdaJoin("a", SdBlogArticle.class)
            .select("a", SdBlogArticle::getArticleId, SdBlogArticle::getAuthorId,
                SdBlogArticle::getPublishedAt, SdBlogArticle::getViewCount)
            .select("v", SdBlogArticleVersion::getTitle, SdBlogArticleVersion::getSummary, SdBlogArticleVersion::getCoverUrl)
            .leftJoin(SdBlogArticleVersion.class, "v", SdBlogArticleVersion::getVersionId, SdBlogArticle::getCurrentVersionId)
            .eq("a", SdBlogArticle::getStatus, BlogArticleStatus.PUBLISHED.getCode())
            .eq("a", SdBlogArticle::getVisibility, BlogVisibility.PUBLIC.getCode())
            // OR 分组经 apply(Consumer) 下钻底层 wrapper，字符串列名带别名，keyword 走参数绑定
            .apply(w -> w.and(x -> x.like("v.title", keyword)
                .or().like("v.summary", keyword)
                .or().like("v.content", keyword)))
            .orderByDesc("a", SdBlogArticle::getPublishedAt)
            .orderByDesc("a", SdBlogArticle::getArticleId)
            .page(pageQuery.build(), BlogPublicArticleListItemVo.class);
        List<BlogPublicArticleListItemVo> records = page.getRecords();
        if (CollUtil.isNotEmpty(records)) {
            Map<Long, List<SdBlogTagVo>> tagMap = articleTagFiller.queryTagsByArticleIds(
                records.stream().map(BlogPublicArticleListItemVo::getArticleId).toList());
            records.forEach(item -> item.setTags(tagMap.getOrDefault(item.getArticleId(), List.of())));
        }
        return new BlogSearchPageResult<>(records, page.getTotal(), null);
    }

}
