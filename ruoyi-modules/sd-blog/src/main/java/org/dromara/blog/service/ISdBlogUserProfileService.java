package org.dromara.blog.service;

import org.dromara.blog.domain.vo.BlogUserProfileVo;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;


/**
 * 博客用户资料Service接口
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
public interface ISdBlogUserProfileService {

    /**
     * 查询用户博客资料（聚合 sys_user 实时昵称头像，契约 §8.1 / §9.1）
     *
     * @param userId 用户ID
     * @return 博客资料视图；用户不存在或已停用时返回 null
     */
    BlogUserProfileVo queryProfile(Long userId);

    /**
     * 保存用户博客资料：首次填写时创建记录，之后更新简介（契约 §8.2）
     *
     * @param userId 用户ID（必须由调用方从登录上下文取得）
     * @param bio    个人简介，允许为空字符串（清空）
     * @return 是否保存成功
     */
    Boolean saveProfile(Long userId, String bio);
}
