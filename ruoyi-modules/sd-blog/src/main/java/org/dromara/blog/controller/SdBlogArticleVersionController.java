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
import org.dromara.blog.domain.vo.SdBlogArticleVersionVo;
import org.dromara.blog.domain.bo.SdBlogArticleVersionBo;
import org.dromara.blog.service.ISdBlogArticleVersionService;
import org.dromara.common.core.domain.PageResult;

/**
 * 博客文章版本历史
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
@Validated
@RequiredArgsConstructor
// 按 docs/blog-api.md §14：Version 为 append-only 历史快照，不作为普通 CRUD 资源暴露。
// 生成器产物 Controller 停用，Service/Mapper 保留供领域业务操作复用；后续按契约实现只读 Version 查询接口。
//@RestController
//@RequestMapping("/blog/blogArticleVersion")
public class SdBlogArticleVersionController extends BaseController {

    private final ISdBlogArticleVersionService sdBlogArticleVersionService;

    /**
     * 查询博客文章版本历史列表
     */
    @SaCheckPermission("blog:blogArticleVersion:list")
    @GetMapping("/list")
    public R<PageResult<SdBlogArticleVersionVo>> list(SdBlogArticleVersionBo bo, PageQuery pageQuery) {
        return R.ok(sdBlogArticleVersionService.queryPageList(bo, pageQuery));
    }

    /**
     * 导出博客文章版本历史列表
     */
    @SaCheckPermission("blog:blogArticleVersion:export")
    @Log(title = "博客文章版本历史", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SdBlogArticleVersionBo bo, HttpServletResponse response) {
        List<SdBlogArticleVersionVo> list = sdBlogArticleVersionService.queryList(bo);
        ExcelBuilder.of(list, SdBlogArticleVersionVo.class).sheetName("博客文章版本历史").toResponse(response);
    }

    /**
     * 获取博客文章版本历史详细信息
     *
     * @param versionId 主键
     */
    @SaCheckPermission("blog:blogArticleVersion:query")
    @GetMapping("/{versionId}")
    public R<SdBlogArticleVersionVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long versionId) {
        return R.ok(sdBlogArticleVersionService.queryById(versionId));
    }

    /**
     * 新增博客文章版本历史
     */
    @SaCheckPermission("blog:blogArticleVersion:add")
    @Log(title = "博客文章版本历史", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SdBlogArticleVersionBo bo) {
        return toAjax(sdBlogArticleVersionService.insertByBo(bo));
    }

    /**
     * 修改博客文章版本历史
     */
    @SaCheckPermission("blog:blogArticleVersion:edit")
    @Log(title = "博客文章版本历史", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SdBlogArticleVersionBo bo) {
        return toAjax(sdBlogArticleVersionService.updateByBo(bo));
    }



    /**
     * 删除博客文章版本历史
     *
     * @param versionIds 主键串
     */
    @SaCheckPermission("blog:blogArticleVersion:remove")
    @Log(title = "博客文章版本历史", businessType = BusinessType.DELETE)
    @DeleteMapping("/{versionIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] versionIds) {
        return toAjax(sdBlogArticleVersionService.deleteWithValidByIds(List.of(versionIds), true));
    }
}
