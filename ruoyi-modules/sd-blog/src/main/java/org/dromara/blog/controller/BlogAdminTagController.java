package org.dromara.blog.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.blog.domain.bo.BlogTagMergeBo;
import org.dromara.blog.domain.bo.BlogTagOpsBo;
import org.dromara.blog.domain.bo.BlogTagRenameBo;
import org.dromara.blog.domain.bo.SdBlogTagBo;
import org.dromara.blog.domain.vo.SdBlogTagVo;
import org.dromara.blog.service.ISdBlogTagService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.redis.annotation.RepeatSubmit;
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

/**
 * 管理员标签治理
 * <p>
 * 契约 §13：标签管理列表/详情/运营属性/重命名/合并/删除；
 * name/slug/status/mergedInto 禁止普通更新，重命名与合并必须走专用动作接口（§13.3）；
 * 权限标识 blog:tag:动作（契约 §17），生成器裸 CRUD Controller 已按 D12 停用。
 *
 * @author Xu
 * @date 2026-09-30
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/blog/admin/tags")
public class BlogAdminTagController extends BaseController {

    private final ISdBlogTagService sdBlogTagService;

    /**
     * 标签管理列表（§13.1）：可见 ACTIVE/MERGED 及治理信息
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    @SaCheckPermission("blog:tag:list")
    @GetMapping
    public R<PageResult<SdBlogTagVo>> list(SdBlogTagBo bo, PageQuery pageQuery) {
        return R.ok(sdBlogTagService.queryAdminPageList(bo, pageQuery));
    }

    /**
     * 标签详情（§13.2）
     *
     * @param tagId 标签ID
     * @return 标签详情
     */
    @SaCheckPermission("blog:tag:query")
    @GetMapping("/{tagId}")
    public R<SdBlogTagVo> getInfo(@NotNull(message = "标签ID不能为空") @PathVariable("tagId") Long tagId) {
        SdBlogTagVo vo = sdBlogTagService.queryById(tagId);
        if (vo == null) {
            throw new ServiceException("标签不存在");
        }
        return R.ok(vo);
    }

    /**
     * 更新运营属性（§13.3）：仅 isFeatured/sortOrder
     *
     * @param tagId 标签ID
     * @param bo    运营字段
     * @return 操作结果
     */
    @SaCheckPermission("blog:tag:edit")
    @Log(title = "博客标签治理", businessType = BusinessType.UPDATE)
    @PutMapping("/{tagId}")
    public R<Void> updateOps(@NotNull(message = "标签ID不能为空") @PathVariable("tagId") Long tagId,
                             @Validated @RequestBody BlogTagOpsBo bo) {
        return toAjax(sdBlogTagService.updateOps(tagId, bo.getIsFeatured(), bo.getSortOrder()));
    }

    /**
     * 重命名标签（§13.4）：旧 name/slug 保留为 alias，tag_id 不变
     *
     * @param tagId 标签ID
     * @param bo    新名称
     * @return 操作结果
     */
    @RepeatSubmit
    @SaCheckPermission("blog:tag:rename")
    @Log(title = "博客标签治理", businessType = BusinessType.UPDATE)
    @PutMapping("/{tagId}/rename")
    public R<Void> rename(@NotNull(message = "标签ID不能为空") @PathVariable("tagId") Long tagId,
                          @Validated @RequestBody BlogTagRenameBo bo) {
        return toAjax(sdBlogTagService.renameTag(tagId, bo.getName()));
    }

    /**
     * 合并标签（§13.5）：事务六步迁移+去重+alias+展平，不保留长期合并链
     *
     * @param tagId 源标签ID
     * @param bo    目标标签
     * @return 操作结果
     */
    @RepeatSubmit
    @SaCheckPermission("blog:tag:merge")
    @Log(title = "博客标签治理", businessType = BusinessType.UPDATE)
    @PostMapping("/{tagId}/merge")
    public R<Void> merge(@NotNull(message = "标签ID不能为空") @PathVariable("tagId") Long tagId,
                         @Validated @RequestBody BlogTagMergeBo bo) {
        return toAjax(sdBlogTagService.mergeTag(tagId, bo.getTargetTagId()));
    }

    /**
     * 删除垃圾标签（§13.6）：解除关系+逻辑删除，不建 alias
     *
     * @param tagId 标签ID
     * @return 操作结果
     */
    @SaCheckPermission("blog:tag:remove")
    @Log(title = "博客标签治理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{tagId}")
    public R<Void> remove(@NotNull(message = "标签ID不能为空") @PathVariable("tagId") Long tagId) {
        return toAjax(sdBlogTagService.adminDelete(tagId));
    }

}
