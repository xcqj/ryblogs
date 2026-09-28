package org.dromara.blog.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dromara.common.core.utils.StringUtils;

/**
 * 博客文章可见性枚举（对应 sd_blog_article.visibility，CHAR(1)）
 * <p>
 * 契约 blog-api.md §1.3：用户隐藏与管理员下线是两个独立业务状态；
 * 公共可见必须满足 status=PUBLISHED 且 visibility=PUBLIC 且未逻辑删除。
 *
 * @author Xu
 */
@Getter
@AllArgsConstructor
public enum BlogVisibility {

    /**
     * 公开
     */
    PUBLIC("0", "公开"),

    /**
     * 用户主动隐藏（作者可自行恢复）
     */
    USER_HIDDEN("1", "用户隐藏"),

    /**
     * 管理员治理下线（作者不可自行恢复、不可编辑）
     */
    ADMIN_HIDDEN("2", "管理员下线");

    /**
     * 可见性编码（数据库存储值）
     */
    private final String code;

    /**
     * 可见性描述
     */
    private final String text;

    /**
     * 按编码查找枚举
     *
     * @param code 可见性编码
     * @return 匹配的枚举；未匹配返回 null
     */
    public static BlogVisibility fromCode(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        for (BlogVisibility visibility : values()) {
            if (visibility.getCode().equals(code)) {
                return visibility;
            }
        }
        return null;
    }
}
