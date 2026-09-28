package org.dromara.blog.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.blog.domain.bo.BlogProfileBo;
import org.dromara.blog.domain.vo.BlogUserProfileVo;
import org.dromara.blog.service.ISdBlogUserProfileService;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.redis.annotation.RepeatSubmit;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前用户博客资料
 * <p>
 * 契约 §8：作者自助接口，仅要求登录，不设按钮级权限；
 * userId 一律取登录上下文，不接收前端传入（AGENTS.md §5 红线）。
 *
 * @author Xu
 * @date 2026-09-28
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/blog/user")
public class BlogUserProfileController extends BaseController {

    private final ISdBlogUserProfileService sdBlogUserProfileService;

    /**
     * 查询自己的博客资料
     *
     * @return 昵称、头像（实时取自 sys_user）与个人简介
     */
    @GetMapping("/profile")
    public R<BlogUserProfileVo> profile() {
        return R.ok(sdBlogUserProfileService.queryProfile(LoginHelper.getUserId()));
    }

    /**
     * 修改自己的博客资料（首次填写时自动创建记录）
     *
     * @param bo 个人资料参数
     * @return 操作结果
     */
    @RepeatSubmit
    @Log(title = "博客个人资料", businessType = BusinessType.UPDATE)
    @PutMapping("/profile")
    public R<Void> updateProfile(@Validated @RequestBody BlogProfileBo bo) {
        return toAjax(sdBlogUserProfileService.saveProfile(LoginHelper.getUserId(), bo.getBio()));
    }

}
