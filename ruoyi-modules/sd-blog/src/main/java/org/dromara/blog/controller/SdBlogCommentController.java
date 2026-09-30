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
import org.dromara.blog.domain.vo.SdBlogCommentVo;
import org.dromara.blog.domain.bo.SdBlogCommentBo;
import org.dromara.blog.service.ISdBlogCommentService;
import org.dromara.common.core.domain.PageResult;

/**
 * 博客评论
 *
 * @author Xu
 * @date 2026-09-28 02:23:30
 */
@Validated
@RequiredArgsConstructor
// 按 docs/ROADMAP.md §6 D12 决议（S7）：生成器裸 CRUD 违反契约 §10.2（评论不可修改、用户不可自删、
// 裸 insert 可伪造归属），已被契约接口取代——发表/公共列表 /blog/article|public/**/comments + 治理 /blog/admin/comments。
// Controller 停用，Service/Mapper 保留复用；菜单随 blog_s7_menu.sql 清理。
//@RestController
//@RequestMapping("/blog/blogComment")
public class SdBlogCommentController extends BaseController {

    private final ISdBlogCommentService sdBlogCommentService;

    /**
     * 查询博客评论列表
     */
    @SaCheckPermission("blog:blogComment:list")
    @GetMapping("/list")
    public R<PageResult<SdBlogCommentVo>> list(SdBlogCommentBo bo, PageQuery pageQuery) {
        return R.ok(sdBlogCommentService.queryPageList(bo, pageQuery));
    }

    /**
     * 导出博客评论列表
     */
    @SaCheckPermission("blog:blogComment:export")
    @Log(title = "博客评论", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SdBlogCommentBo bo, HttpServletResponse response) {
        List<SdBlogCommentVo> list = sdBlogCommentService.queryList(bo);
        ExcelBuilder.of(list, SdBlogCommentVo.class).sheetName("博客评论").toResponse(response);
    }

    /**
     * 获取博客评论详细信息
     *
     * @param commentId 主键
     */
    @SaCheckPermission("blog:blogComment:query")
    @GetMapping("/{commentId}")
    public R<SdBlogCommentVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long commentId) {
        return R.ok(sdBlogCommentService.queryById(commentId));
    }

    /**
     * 新增博客评论
     */
    @SaCheckPermission("blog:blogComment:add")
    @Log(title = "博客评论", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SdBlogCommentBo bo) {
        return toAjax(sdBlogCommentService.insertByBo(bo));
    }

    /**
     * 修改博客评论
     */
    @SaCheckPermission("blog:blogComment:edit")
    @Log(title = "博客评论", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SdBlogCommentBo bo) {
        return toAjax(sdBlogCommentService.updateByBo(bo));
    }



    /**
     * 删除博客评论
     *
     * @param commentIds 主键串
     */
    @SaCheckPermission("blog:blogComment:remove")
    @Log(title = "博客评论", businessType = BusinessType.DELETE)
    @DeleteMapping("/{commentIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] commentIds) {
        return toAjax(sdBlogCommentService.deleteWithValidByIds(List.of(commentIds), true));
    }
}
