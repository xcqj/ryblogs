package org.dromara.blog.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.blog.domain.vo.BlogCommentListItemVo;
import org.dromara.blog.service.ISdBlogCommentService;
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
 * 公共评论 API
 * <p>
 * 契约 §10.1：匿名接口（/blog/public/** 白名单）；仅公共可见文章的评论可查，
 * 只返回未逻辑删除评论，一级评论（parent_id 恒 NULL 不暴露）。
 *
 * @author Xu
 * @date 2026-10-01
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/blog/public/articles/{articleId}/comments")
public class BlogPublicCommentController extends BaseController {

    private final ISdBlogCommentService sdBlogCommentService;

    /**
     * 公共评论列表（时间升序分页）
     *
     * @param articleId 文章ID
     * @param pageQuery 分页参数
     * @return 评论分页
     */
    @GetMapping
    public R<PageResult<BlogCommentListItemVo>> list(@NotNull(message = "文章ID不能为空")
                                                     @PathVariable("articleId") Long articleId,
                                                     PageQuery pageQuery) {
        return R.ok(sdBlogCommentService.queryPublicPageList(articleId, pageQuery));
    }

}
