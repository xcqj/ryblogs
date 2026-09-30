package org.dromara.blog.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.blog.domain.bo.BlogPublicArticleQueryBo;
import org.dromara.blog.domain.vo.BlogPublicArticleDetailVo;
import org.dromara.blog.domain.vo.BlogPublicArticleListItemVo;
import org.dromara.blog.service.ISdBlogArticleService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公共文章 API
 * <p>
 * 契约 §5.1/§5.2：匿名接口（/blog/public/** 白名单）；公共可见三条件由查询底座强制内建，
 * 本层不提供任何状态/可见性过滤参数，杜绝绕过（AGENTS.md §5 红线）。
 *
 * @author Xu
 * @date 2026-09-30
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/blog/public/articles")
public class BlogPublicArticleController extends BaseController {

    private final ISdBlogArticleService sdBlogArticleService;

    /**
     * 公共文章列表（§5.1）：tagSlug/authorId 可选过滤，published_at 倒序分页
     *
     * @param bo        过滤条件（tagSlug/authorId）
     * @param pageQuery 分页参数
     * @return 公共文章分页
     */
    @GetMapping
    public R<PageResult<BlogPublicArticleListItemVo>> list(BlogPublicArticleQueryBo bo, PageQuery pageQuery) {
        return R.ok(sdBlogArticleService.queryPublicPageList(bo, pageQuery));
    }

    /**
     * 公共文章详情（§5.2）：成功打开时 view_count +1（D8 同步原子 UPDATE）；
     * 草稿/隐藏/下线/已删一律“文章不存在”
     *
     * @param articleId 文章ID
     * @return 公共详情（含正文、标签、最新阅读量）
     */
    @GetMapping("/{articleId}")
    public R<BlogPublicArticleDetailVo> detail(@NotNull(message = "文章ID不能为空")
                                               @PathVariable("articleId") Long articleId) {
        return R.ok(sdBlogArticleService.queryPublicDetail(articleId));
    }

}
