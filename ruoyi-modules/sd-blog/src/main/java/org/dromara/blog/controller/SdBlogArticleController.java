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
import org.dromara.blog.domain.vo.SdBlogArticleVo;
import org.dromara.blog.domain.bo.SdBlogArticleBo;
import org.dromara.blog.service.ISdBlogArticleService;
import org.dromara.common.core.domain.PageResult;

/**
 * 博客文章
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/blog/blogArticle")
public class SdBlogArticleController extends BaseController {

    private final ISdBlogArticleService sdBlogArticleService;

    /**
     * 查询博客文章列表
     */
    @SaCheckPermission("blog:blogArticle:list")
    @GetMapping("/list")
    public R<PageResult<SdBlogArticleVo>> list(SdBlogArticleBo bo, PageQuery pageQuery) {
        return R.ok(sdBlogArticleService.queryPageList(bo, pageQuery));
    }

    /**
     * 导出博客文章列表
     */
    @SaCheckPermission("blog:blogArticle:export")
    @Log(title = "博客文章", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SdBlogArticleBo bo, HttpServletResponse response) {
        List<SdBlogArticleVo> list = sdBlogArticleService.queryList(bo);
        ExcelBuilder.of(list, SdBlogArticleVo.class).sheetName("博客文章").toResponse(response);
    }

    /**
     * 获取博客文章详细信息
     *
     * @param articleId 主键
     */
    @SaCheckPermission("blog:blogArticle:query")
    @GetMapping("/{articleId}")
    public R<SdBlogArticleVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long articleId) {
        return R.ok(sdBlogArticleService.queryById(articleId));
    }

    /**
     * 新增博客文章
     */
    @SaCheckPermission("blog:blogArticle:add")
    @Log(title = "博客文章", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SdBlogArticleBo bo) {
        return toAjax(sdBlogArticleService.insertByBo(bo));
    }

    /**
     * 修改博客文章
     */
    @SaCheckPermission("blog:blogArticle:edit")
    @Log(title = "博客文章", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SdBlogArticleBo bo) {
        return toAjax(sdBlogArticleService.updateByBo(bo));
    }



    /**
     * 删除博客文章
     *
     * @param articleIds 主键串
     */
    @SaCheckPermission("blog:blogArticle:remove")
    @Log(title = "博客文章", businessType = BusinessType.DELETE)
    @DeleteMapping("/{articleIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] articleIds) {
        return toAjax(sdBlogArticleService.deleteWithValidByIds(List.of(articleIds), true));
    }
}
