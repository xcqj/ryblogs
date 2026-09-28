package org.dromara.blog.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.blog.domain.vo.BlogUserProfileVo;
import org.dromara.blog.service.ISdBlogUserProfileService;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公共用户资料
 * <p>
 * 契约 §9.1：匿名可访问，路由前缀 /blog/public/** 已在 security.excludes 白名单中放行。
 *
 * @author Xu
 * @date 2026-09-28
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/blog/public/users")
public class BlogPublicUserController extends BaseController {

    private final ISdBlogUserProfileService sdBlogUserProfileService;

    /**
     * 查询用户公开资料（昵称、头像实时取自 sys_user）
     *
     * @param userId 用户ID
     * @return 公开资料；用户不存在或已停用时返回业务失败
     */
    @GetMapping("/{userId}")
    public R<BlogUserProfileVo> getInfo(@NotNull(message = "用户ID不能为空")
                                        @PathVariable("userId") Long userId) {
        BlogUserProfileVo vo = sdBlogUserProfileService.queryProfile(userId);
        if (vo == null) {
            return R.fail("用户不存在");
        }
        return R.ok(vo);
    }

}
