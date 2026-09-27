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
import org.dromara.blog.domain.vo.SdBlogTagAliasVo;
import org.dromara.blog.domain.bo.SdBlogTagAliasBo;
import org.dromara.blog.service.ISdBlogTagAliasService;
import org.dromara.common.core.domain.PageResult;

/**
 * 博客标签历史别名
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/blog/blogTagAlias")
public class SdBlogTagAliasController extends BaseController {

    private final ISdBlogTagAliasService sdBlogTagAliasService;

    /**
     * 查询博客标签历史别名列表
     */
    @SaCheckPermission("blog:blogTagAlias:list")
    @GetMapping("/list")
    public R<PageResult<SdBlogTagAliasVo>> list(SdBlogTagAliasBo bo, PageQuery pageQuery) {
        return R.ok(sdBlogTagAliasService.queryPageList(bo, pageQuery));
    }

    /**
     * 导出博客标签历史别名列表
     */
    @SaCheckPermission("blog:blogTagAlias:export")
    @Log(title = "博客标签历史别名", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SdBlogTagAliasBo bo, HttpServletResponse response) {
        List<SdBlogTagAliasVo> list = sdBlogTagAliasService.queryList(bo);
        ExcelBuilder.of(list, SdBlogTagAliasVo.class).sheetName("博客标签历史别名").toResponse(response);
    }

    /**
     * 获取博客标签历史别名详细信息
     *
     * @param tagAliasId 主键
     */
    @SaCheckPermission("blog:blogTagAlias:query")
    @GetMapping("/{tagAliasId}")
    public R<SdBlogTagAliasVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long tagAliasId) {
        return R.ok(sdBlogTagAliasService.queryById(tagAliasId));
    }

    /**
     * 新增博客标签历史别名
     */
    @SaCheckPermission("blog:blogTagAlias:add")
    @Log(title = "博客标签历史别名", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SdBlogTagAliasBo bo) {
        return toAjax(sdBlogTagAliasService.insertByBo(bo));
    }

    /**
     * 修改博客标签历史别名
     */
    @SaCheckPermission("blog:blogTagAlias:edit")
    @Log(title = "博客标签历史别名", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SdBlogTagAliasBo bo) {
        return toAjax(sdBlogTagAliasService.updateByBo(bo));
    }



    /**
     * 删除博客标签历史别名
     *
     * @param tagAliasIds 主键串
     */
    @SaCheckPermission("blog:blogTagAlias:remove")
    @Log(title = "博客标签历史别名", businessType = BusinessType.DELETE)
    @DeleteMapping("/{tagAliasIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] tagAliasIds) {
        return toAjax(sdBlogTagAliasService.deleteWithValidByIds(List.of(tagAliasIds), true));
    }
}
