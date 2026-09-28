package org.dromara.blog.domain.vo;

import lombok.Data;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;

import java.io.Serial;
import java.io.Serializable;

/**
 * 博客用户资料视图对象（契约 §8.1 / §9.1 共用）
 * <p>
 * 跨表聚合 VO：昵称、头像实时取自 sys_user（不复制），bio 来自 sd_blog_user_profile。
 * 不使用 @AutoMapper：字段并非来自单一实体。
 *
 * @author Xu
 * @date 2026-09-28
 */
@Data
public class BlogUserProfileVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户昵称（实时取自 sys_user）
     */
    private String nickName;

    /**
     * 头像 OSS ID（实时取自 sys_user）
     */
    private Long avatar;

    /**
     * 头像地址
     */
    @Translation(type = TransConstant.OSS_ID_TO_URL, mapper = "avatar")
    private String avatarUrl;

    /**
     * 个人简介（允许为空）
     */
    private String bio;

}
