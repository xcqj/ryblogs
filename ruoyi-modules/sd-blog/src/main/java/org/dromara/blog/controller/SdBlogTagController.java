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
import org.dromara.blog.domain.vo.SdBlogTagVo;
import org.dromara.blog.domain.bo.SdBlogTagBo;
import org.dromara.blog.service.ISdBlogTagService;
import org.dromara.common.core.domain.PageResult;

/**
 * 博客标签
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
@Validated
@RequiredArgsConstructor
// 按 docs/ROADMAP.md §6 D12 决议（S6）：生成器裸 CRUD 直改 name/slug/status、裸批量删除会留孤儿关系，
// 已被契约接口取代——公共 §7（/blog/public/tags/**）+ 治理 §13（/blog/admin/tags/**）；
// Service 写方法已加护栏，本 Controller 停用，Service/Mapper 查询能力保留复用；菜单随 blog_s6_menu.sql 清理。
//@RestController
//@RequestMapping("/blog/blogTag")
public class SdBlogTagController extends BaseController {

    private final ISdBlogTagService sdBlogTagService;

    /**
     * 查询博客标签列表
     */
    @SaCheckPermission("blog:blogTag:list")
    @GetMapping("/list")
    public R<PageResult<SdBlogTagVo>> list(SdBlogTagBo bo, PageQuery pageQuery) {
        return R.ok(sdBlogTagService.queryPageList(bo, pageQuery));
    }

    /**
     * 导出博客标签列表
     */
    @SaCheckPermission("blog:blogTag:export")
    @Log(title = "博客标签", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SdBlogTagBo bo, HttpServletResponse response) {
        List<SdBlogTagVo> list = sdBlogTagService.queryList(bo);
        ExcelBuilder.of(list, SdBlogTagVo.class).sheetName("博客标签").toResponse(response);
    }

    /**
     * 获取博客标签详细信息
     *
     * @param tagId 主键
     */
    @SaCheckPermission("blog:blogTag:query")
    @GetMapping("/{tagId}")
    public R<SdBlogTagVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long tagId) {
        return R.ok(sdBlogTagService.queryById(tagId));
    }

    /**
     * 新增博客标签
     */
    @SaCheckPermission("blog:blogTag:add")
    @Log(title = "博客标签", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SdBlogTagBo bo) {
        return toAjax(sdBlogTagService.insertByBo(bo));
    }

    /**
     * 修改博客标签
     */
    @SaCheckPermission("blog:blogTag:edit")
    @Log(title = "博客标签", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SdBlogTagBo bo) {
        return toAjax(sdBlogTagService.updateByBo(bo));
    }



    /**
     * 删除博客标签
     *
     * @param tagIds 主键串
     */
    @SaCheckPermission("blog:blogTag:remove")
    @Log(title = "博客标签", businessType = BusinessType.DELETE)
    @DeleteMapping("/{tagIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] tagIds) {
        return toAjax(sdBlogTagService.deleteWithValidByIds(List.of(tagIds), true));
    }
}
