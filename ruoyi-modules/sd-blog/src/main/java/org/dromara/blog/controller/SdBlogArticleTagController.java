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
import org.dromara.blog.domain.vo.SdBlogArticleTagVo;
import org.dromara.blog.domain.bo.SdBlogArticleTagBo;
import org.dromara.blog.service.ISdBlogArticleTagService;
import org.dromara.common.core.domain.PageResult;

/**
 * 博客文章标签关系
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
@Validated
@RequiredArgsConstructor
// 按 docs/blog-api.md §14：ArticleTag 关系由保存文章/标签合并等领域操作维护，不作为普通 CRUD 资源暴露。
// 生成器产物 Controller 停用，Service/Mapper 保留供领域业务操作复用。
//@RestController
//@RequestMapping("/blog/blogArticleTag")
public class SdBlogArticleTagController extends BaseController {

    private final ISdBlogArticleTagService sdBlogArticleTagService;

    /**
     * 查询博客文章标签关系列表
     */
    @SaCheckPermission("blog:blogArticleTag:list")
    @GetMapping("/list")
    public R<PageResult<SdBlogArticleTagVo>> list(SdBlogArticleTagBo bo, PageQuery pageQuery) {
        return R.ok(sdBlogArticleTagService.queryPageList(bo, pageQuery));
    }

    /**
     * 导出博客文章标签关系列表
     */
    @SaCheckPermission("blog:blogArticleTag:export")
    @Log(title = "博客文章标签关系", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SdBlogArticleTagBo bo, HttpServletResponse response) {
        List<SdBlogArticleTagVo> list = sdBlogArticleTagService.queryList(bo);
        ExcelBuilder.of(list, SdBlogArticleTagVo.class).sheetName("博客文章标签关系").toResponse(response);
    }

    /**
     * 获取博客文章标签关系详细信息
     *
     * @param articleTagId 主键
     */
    @SaCheckPermission("blog:blogArticleTag:query")
    @GetMapping("/{articleTagId}")
    public R<SdBlogArticleTagVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long articleTagId) {
        return R.ok(sdBlogArticleTagService.queryById(articleTagId));
    }

    /**
     * 新增博客文章标签关系
     */
    @SaCheckPermission("blog:blogArticleTag:add")
    @Log(title = "博客文章标签关系", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SdBlogArticleTagBo bo) {
        return toAjax(sdBlogArticleTagService.insertByBo(bo));
    }

    /**
     * 修改博客文章标签关系
     */
    @SaCheckPermission("blog:blogArticleTag:edit")
    @Log(title = "博客文章标签关系", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SdBlogArticleTagBo bo) {
        return toAjax(sdBlogArticleTagService.updateByBo(bo));
    }



    /**
     * 删除博客文章标签关系
     *
     * @param articleTagIds 主键串
     */
    @SaCheckPermission("blog:blogArticleTag:remove")
    @Log(title = "博客文章标签关系", businessType = BusinessType.DELETE)
    @DeleteMapping("/{articleTagIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] articleTagIds) {
        return toAjax(sdBlogArticleTagService.deleteWithValidByIds(List.of(articleTagIds), true));
    }
}
