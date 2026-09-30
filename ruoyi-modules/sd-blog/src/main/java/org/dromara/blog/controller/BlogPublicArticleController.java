package org.dromara.blog.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.blog.domain.vo.BlogPublicArticleListItemVo;
import org.dromara.blog.service.ISdBlogArticleService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公共文章 API
 * <p>
 * 契约 §5.1：匿名接口（/blog/public/** 白名单）；公共可见三条件由查询底座强制内建，
 * 本层不提供任何状态/可见性过滤参数，杜绝绕过（AGENTS.md §5 红线）。
 * §5.2 公共详情与阅读量在 S8 落地。
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
     * 公共文章列表（§5.1）：published_at 倒序分页
     *
     * @param pageQuery 分页参数
     * @return 公共文章分页
     */
    @GetMapping
    public R<PageResult<BlogPublicArticleListItemVo>> list(PageQuery pageQuery) {
        return R.ok(sdBlogArticleService.queryPublicPageList(pageQuery, null));
    }

}
