package org.dromara.blog.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dromara.common.core.utils.StringUtils;

/**
 * 博客文章发布状态枚举（对应 sd_blog_article.status，CHAR(1)）
 * <p>
 * 契约 blog-api.md §1.3：DRAFT 仅作者可见；PUBLISHED 表示已发布过。
 *
 * @author Xu
 */
@Getter
@AllArgsConstructor
public enum BlogArticleStatus {

    /**
     * 草稿
     */
    DRAFT("0", "草稿"),

    /**
     * 已发布
     */
    PUBLISHED("1", "已发布");

    /**
     * 状态编码（数据库存储值）
     */
    private final String code;

    /**
     * 状态描述
     */
    private final String text;

    /**
     * 按编码查找枚举
     *
     * @param code 状态编码
     * @return 匹配的枚举；未匹配返回 null
     */
    public static BlogArticleStatus fromCode(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        for (BlogArticleStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
