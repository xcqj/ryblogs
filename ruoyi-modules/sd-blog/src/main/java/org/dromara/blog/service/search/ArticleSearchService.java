package org.dromara.blog.service.search;

import org.dromara.blog.domain.vo.BlogPublicArticleListItemVo;
import org.dromara.blog.domain.vo.BlogSearchPageResult;
import org.dromara.common.mybatis.core.page.PageQuery;

/**
 * 文章搜索抽象（契约 §6，ROADMAP S8；S16 起支持 ES 实现与深翻页游标）
 * <p>
 * 契约 §6.1：搜索字段与底层实现属实现层问题；引入 Elasticsearch 时替换本接口实现即可，
 * 不改变 /blog/public/search 的业务契约。所有实现必须满足公共文章可见三条件（AGENTS.md §9），
 * 且不得泄露内部状态字段。
 * <p>
 * S16：返回类型细化为 {@link BlogSearchPageResult}（框架 PageResult 子类，仅多一个可选
 * {@code nextAfter} 深翻页游标，JSON 向后兼容）；入参加可选 {@code after} 游标
 * （mysql 实现忽略，ES 实现用于越过深页窗口）。
 *
 * @author Xu
 * @date 2026-10-01
 */
public interface ArticleSearchService {

    /**
     * 搜索公开文章
     *
     * @param keyword   关键字（调用方保证非空）
     * @param pageQuery 分页参数
     * @param after     深翻页游标（可选，opaque；传值时忽略 pageNum。mysql 实现忽略本参数）
     * @return 公共文章分页（与 §5.1 列表同构；ES 实现命中行带可选 highlight）
     */
    BlogSearchPageResult<BlogPublicArticleListItemVo> search(String keyword, PageQuery pageQuery, String after);

}
