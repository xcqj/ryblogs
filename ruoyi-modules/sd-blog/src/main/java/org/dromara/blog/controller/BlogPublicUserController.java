package org.dromara.blog.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.blog.domain.bo.BlogPublicArticleQueryBo;
import org.dromara.blog.domain.vo.BlogPublicArticleListItemVo;
import org.dromara.blog.domain.vo.BlogUserProfileVo;
import org.dromara.blog.service.ISdBlogArticleService;
import org.dromara.blog.service.ISdBlogUserProfileService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公共用户资料与用户公开文章
 * <p>
 * 契约 §9.1/§9.2：匿名可访问，路由前缀 /blog/public/** 已在 security.excludes 白名单中放行。
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
    private final ISdBlogArticleService sdBlogArticleService;

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

    /**
     * 用户公开文章列表（契约 §9.2，S8/D18 并入）：只返回 PUBLISHED+PUBLIC+未删；
     * 列表类接口，用户不存在时自然空列表
     *
     * @param userId    用户ID
     * @param pageQuery 分页参数
     * @return 公共文章分页
     */
    @GetMapping("/{userId}/articles")
    public R<PageResult<BlogPublicArticleListItemVo>> articles(@NotNull(message = "用户ID不能为空")
                                                               @PathVariable("userId") Long userId,
                                                               PageQuery pageQuery) {
        BlogPublicArticleQueryBo bo = new BlogPublicArticleQueryBo();
        bo.setAuthorId(userId);
        return R.ok(sdBlogArticleService.queryPublicPageList(bo, pageQuery));
    }

}
