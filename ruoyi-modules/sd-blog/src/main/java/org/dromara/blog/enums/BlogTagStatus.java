package org.dromara.blog.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dromara.common.core.utils.StringUtils;

/**
 * 博客标签状态枚举（对应 sd_blog_tag.status，CHAR(1)）
 * <p>
 * 契约 blog-api.md §13.5：合并后源标签保持 MERGED + merged_into 指向最终有效标签，
 * 合并链必须展平，不允许长期存在 A → B → C。
 *
 * @author Xu
 */
@Getter
@AllArgsConstructor
public enum BlogTagStatus {

    /**
     * 有效标签
     */
    ACTIVE("0", "有效"),

    /**
     * 已被合并（merged_into 指向目标标签，不参与候选与精选）
     */
    MERGED("1", "已合并");

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
    public static BlogTagStatus fromCode(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        for (BlogTagStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
