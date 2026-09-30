package org.dromara.blog.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.blog.domain.bo.BlogAdminArticleHideBo;
import org.dromara.blog.domain.bo.BlogAdminArticleQueryBo;
import org.dromara.blog.domain.vo.BlogAdminArticleListItemVo;
import org.dromara.blog.domain.vo.BlogArticleDetailVo;
import org.dromara.blog.service.ISdBlogArticleService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.redis.annotation.RepeatSubmit;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.web.core.BaseController;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员文章治理
 * <p>
 * 契约 §11：管理员可见平台全部状态文章；治理手段仅有下线（§11.3）与恢复（§11.4），
 * 不可编辑、不可代发布、不可删除文章（v0.2 决议，AGENTS.md §5）；
 * 权限标识 blog:资源:动作（契约 §17），按钮权限走 sys_menu，非超管角色可授权。
 *
 * @author Xu
 * @date 2026-09-30
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/blog/admin/articles")
public class BlogAdminArticleController extends BaseController {

    private final ISdBlogArticleService sdBlogArticleService;

    /**
     * 管理员文章列表（可见 DRAFT/PUBLISHED/USER_HIDDEN/ADMIN_HIDDEN 全部状态）
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页结果（含治理信息）
     */
    @SaCheckPermission("blog:article:list")
    @GetMapping
    public R<PageResult<BlogAdminArticleListItemVo>> list(BlogAdminArticleQueryBo bo, PageQuery pageQuery) {
        return R.ok(sdBlogArticleService.queryAdminPageList(bo, pageQuery));
    }

    /**
     * 管理员文章详情（完整当前状态 + 治理信息，契约 §11.2）
     *
     * @param articleId 文章ID
     * @return 文章详情
     */
    @SaCheckPermission("blog:article:query")
    @GetMapping("/{articleId}")
    public R<BlogArticleDetailVo> getInfo(@NotNull(message = "文章ID不能为空")
                                          @PathVariable("articleId") Long articleId) {
        return R.ok(sdBlogArticleService.queryDetail(articleId, LoginHelper.getUserId(), true));
    }

    /**
     * 下线文章（PUBLISHED+PUBLIC → ADMIN_HIDDEN，写治理字段，同态幂等）
     *
     * @param articleId 文章ID
     * @param bo        下线原因
     * @return 操作结果
     */
    @RepeatSubmit
    @SaCheckPermission("blog:article:adminHide")
    @Log(title = "博客文章治理", businessType = BusinessType.UPDATE)
    @PostMapping("/{articleId}/hide")
    public R<Void> adminHide(@NotNull(message = "文章ID不能为空") @PathVariable("articleId") Long articleId,
                             @Validated @RequestBody BlogAdminArticleHideBo bo) {
        return toAjax(sdBlogArticleService.adminHide(articleId, bo.getReason(), LoginHelper.getUserId()));
    }

    /**
     * 恢复文章（ADMIN_HIDDEN → PUBLIC，status 不变，清空治理字段，同态幂等）
     *
     * @param articleId 文章ID
     * @return 操作结果
     */
    @RepeatSubmit
    @SaCheckPermission("blog:article:adminRestore")
    @Log(title = "博客文章治理", businessType = BusinessType.UPDATE)
    @PostMapping("/{articleId}/restore")
    public R<Void> adminRestore(@NotNull(message = "文章ID不能为空") @PathVariable("articleId") Long articleId) {
        return toAjax(sdBlogArticleService.adminRestore(articleId));
    }

}
