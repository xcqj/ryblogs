package org.dromara.blog.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.blog.domain.bo.BlogArticleMyQueryBo;
import org.dromara.blog.domain.bo.BlogArticleSaveBo;
import org.dromara.blog.domain.vo.BlogArticleDetailVo;
import org.dromara.blog.domain.vo.BlogArticleListItemVo;
import org.dromara.blog.domain.vo.SdBlogArticleVersionVo;
import org.dromara.blog.service.ISdBlogArticleService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.redis.annotation.RepeatSubmit;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 作者文章管理
 * <p>
 * 契约 §3 / §4：作者自助接口，仅要求登录，不设按钮级权限；
 * 归属与操作权限全部由服务端按当前登录用户校验（AGENTS.md §5 红线）；
 * 管理员治理接口在 /blog/admin/**（S5），公共接口在 /blog/public/**（S8/S9），互不混用。
 *
 * @author Xu
 * @date 2026-09-28
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/blog/article")
public class BlogArticleController extends BaseController {

    private final ISdBlogArticleService sdBlogArticleService;

    /**
     * 创建文章（草稿）
     *
     * @param bo 保存参数
     * @return 新文章ID
     */
    @RepeatSubmit
    @Log(title = "博客文章", businessType = BusinessType.INSERT)
    @PostMapping()
    public R<Long> add(@Validated @RequestBody BlogArticleSaveBo bo) {
        return R.ok(sdBlogArticleService.createArticle(bo, LoginHelper.getUserId()));
    }

    /**
     * 查询我的文章列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 我的文章分页
     */
    @GetMapping("/my")
    public R<PageResult<BlogArticleListItemVo>> my(BlogArticleMyQueryBo bo, PageQuery pageQuery) {
        return R.ok(sdBlogArticleService.queryMyPageList(bo, pageQuery, LoginHelper.getUserId()));
    }

    /**
     * 查询我的文章详情（作者本人或管理员）
     *
     * @param articleId 文章ID
     * @return 文章详情
     */
    @GetMapping("/{articleId}")
    public R<BlogArticleDetailVo> getInfo(@NotNull(message = "文章ID不能为空")
                                          @PathVariable("articleId") Long articleId) {
        return R.ok(sdBlogArticleService.queryDetail(articleId, LoginHelper.getUserId(), LoginHelper.isSuperAdmin()));
    }

    /**
     * 保存文章（内容实际变化时创建新版本并立即生效）
     *
     * @param articleId 文章ID
     * @param bo        保存参数
     * @return 操作结果
     */
    @RepeatSubmit
    @Log(title = "博客文章", businessType = BusinessType.UPDATE)
    @PutMapping("/{articleId}")
    public R<Void> save(@NotNull(message = "文章ID不能为空") @PathVariable("articleId") Long articleId,
                        @Validated @RequestBody BlogArticleSaveBo bo) {
        return toAjax(sdBlogArticleService.updateArticle(articleId, bo, LoginHelper.getUserId()));
    }

    /**
     * 发布文章（DRAFT → PUBLISHED，立即公开）
     *
     * @param articleId 文章ID
     * @return 操作结果
     */
    @Log(title = "博客文章", businessType = BusinessType.UPDATE)
    @PostMapping("/{articleId}/publish")
    public R<Void> publish(@NotNull(message = "文章ID不能为空") @PathVariable("articleId") Long articleId) {
        return toAjax(sdBlogArticleService.publishArticle(articleId, LoginHelper.getUserId()));
    }

    /**
     * 隐藏文章（PUBLISHED 且非 ADMIN_HIDDEN，PUBLIC → USER_HIDDEN，同态幂等）
     *
     * @param articleId 文章ID
     * @return 操作结果
     */
    @Log(title = "博客文章", businessType = BusinessType.UPDATE)
    @PostMapping("/{articleId}/hide")
    public R<Void> hide(@NotNull(message = "文章ID不能为空") @PathVariable("articleId") Long articleId) {
        return toAjax(sdBlogArticleService.hideArticle(articleId, LoginHelper.getUserId()));
    }

    /**
     * 重新公开文章（USER_HIDDEN → PUBLIC，不可恢复管理员下线，同态幂等）
     *
     * @param articleId 文章ID
     * @return 操作结果
     */
    @Log(title = "博客文章", businessType = BusinessType.UPDATE)
    @PostMapping("/{articleId}/show")
    public R<Void> show(@NotNull(message = "文章ID不能为空") @PathVariable("articleId") Long articleId) {
        return toAjax(sdBlogArticleService.showArticle(articleId, LoginHelper.getUserId()));
    }

    /**
     * 删除文章（逻辑删除，四种状态均可，仅作者本人；管理员治理仅有下线/恢复）
     *
     * @param articleId 文章ID
     * @return 操作结果
     */
    @Log(title = "博客文章", businessType = BusinessType.DELETE)
    @DeleteMapping("/{articleId}")
    public R<Void> remove(@NotNull(message = "文章ID不能为空") @PathVariable("articleId") Long articleId) {
        return toAjax(sdBlogArticleService.deleteByAuthor(articleId, LoginHelper.getUserId()));
    }

    /**
     * 查询文章版本列表（只读）
     *
     * @param articleId 文章ID
     * @return 版本列表（版本号倒序）
     */
    @GetMapping("/{articleId}/versions")
    public R<List<SdBlogArticleVersionVo>> versions(@NotNull(message = "文章ID不能为空")
                                                    @PathVariable("articleId") Long articleId) {
        return R.ok(sdBlogArticleService.queryVersions(articleId, LoginHelper.getUserId(), LoginHelper.isSuperAdmin()));
    }

    /**
     * 查询历史版本详情（只读，含完整快照）
     *
     * @param articleId 文章ID
     * @param versionId 版本ID
     * @return 版本详情
     */
    @GetMapping("/{articleId}/versions/{versionId}")
    public R<SdBlogArticleVersionVo> version(@NotNull(message = "文章ID不能为空") @PathVariable("articleId") Long articleId,
                                             @NotNull(message = "版本ID不能为空") @PathVariable("versionId") Long versionId) {
        return R.ok(sdBlogArticleService.queryVersionDetail(articleId, versionId, LoginHelper.getUserId(), LoginHelper.isSuperAdmin()));
    }

}
