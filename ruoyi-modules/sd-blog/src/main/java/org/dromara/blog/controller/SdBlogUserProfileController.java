package org.dromara.blog.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.*;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.dromara.common.redis.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.web.core.BaseController;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.excel.utils.ExcelBuilder;
import org.dromara.blog.domain.vo.SdBlogUserProfileVo;
import org.dromara.blog.domain.bo.SdBlogUserProfileBo;
import org.dromara.blog.service.ISdBlogUserProfileService;
import org.dromara.common.core.domain.PageResult;

/**
 * 博客用户资料
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/blog/blogUserProfile")
public class SdBlogUserProfileController extends BaseController {

    private final ISdBlogUserProfileService sdBlogUserProfileService;

    /**
     * 查询博客用户资料列表
     */
    @SaCheckPermission("blog:blogUserProfile:list")
    @GetMapping("/list")
    public R<PageResult<SdBlogUserProfileVo>> list(SdBlogUserProfileBo bo, PageQuery pageQuery) {
        return R.ok(sdBlogUserProfileService.queryPageList(bo, pageQuery));
    }

    /**
     * 导出博客用户资料列表
     */
    @SaCheckPermission("blog:blogUserProfile:export")
    @Log(title = "博客用户资料", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SdBlogUserProfileBo bo, HttpServletResponse response) {
        List<SdBlogUserProfileVo> list = sdBlogUserProfileService.queryList(bo);
        ExcelBuilder.of(list, SdBlogUserProfileVo.class).sheetName("博客用户资料").toResponse(response);
    }

    /**
     * 获取博客用户资料详细信息
     *
     * @param userId 主键
     */
    @SaCheckPermission("blog:blogUserProfile:query")
    @GetMapping("/{userId}")
    public R<SdBlogUserProfileVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long userId) {
        return R.ok(sdBlogUserProfileService.queryById(userId));
    }

    /**
     * 新增博客用户资料
     */
    @SaCheckPermission("blog:blogUserProfile:add")
    @Log(title = "博客用户资料", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SdBlogUserProfileBo bo) {
        return toAjax(sdBlogUserProfileService.insertByBo(bo));
    }

    /**
     * 修改博客用户资料
     */
    @SaCheckPermission("blog:blogUserProfile:edit")
    @Log(title = "博客用户资料", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SdBlogUserProfileBo bo) {
        return toAjax(sdBlogUserProfileService.updateByBo(bo));
    }



    /**
     * 删除博客用户资料
     *
     * @param userIds 主键串
     */
    @SaCheckPermission("blog:blogUserProfile:remove")
    @Log(title = "博客用户资料", businessType = BusinessType.DELETE)
    @DeleteMapping("/{userIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] userIds) {
        return toAjax(sdBlogUserProfileService.deleteWithValidByIds(List.of(userIds), true));
    }
}
