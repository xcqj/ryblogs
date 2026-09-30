package org.dromara.blog.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.blog.domain.bo.BlogAdminCommentQueryBo;
import org.dromara.blog.domain.vo.BlogCommentListItemVo;
import org.dromara.blog.service.ISdBlogCommentService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.redis.annotation.RepeatSubmit;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员评论治理
 * <p>
 * 契约 §12：评论列表（articleId/userId/keyword 过滤）与单条逻辑删除；
 * 评论不可修改、用户不可自删（§10.2），删除是唯一治理手段；
 * 权限标识 blog:comment:动作（契约 §17）。
 *
 * @author Xu
 * @date 2026-10-01
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/blog/admin/comments")
public class BlogAdminCommentController extends BaseController {

    private final ISdBlogCommentService sdBlogCommentService;

    /**
     * 评论管理列表（§12.1）
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 评论分页
     */
    @SaCheckPermission("blog:comment:list")
    @GetMapping
    public R<PageResult<BlogCommentListItemVo>> list(BlogAdminCommentQueryBo bo, PageQuery pageQuery) {
        return R.ok(sdBlogCommentService.queryAdminPageList(bo, pageQuery));
    }

    /**
     * 删除违规评论（§12.2）：逻辑删除，不物理删
     *
     * @param commentId 评论ID
     * @return 操作结果
     */
    @RepeatSubmit
    @SaCheckPermission("blog:comment:remove")
    @Log(title = "博客评论治理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{commentId}")
    public R<Void> remove(@NotNull(message = "评论ID不能为空") @PathVariable("commentId") Long commentId) {
        return toAjax(sdBlogCommentService.adminDelete(commentId));
    }

}
