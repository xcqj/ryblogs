package org.dromara.blog.domain;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serial;

/**
 * 博客标签历史别名对象 sd_blog_tag_alias
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sd_blog_tag_alias")
public class SdBlogTagAlias extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 标签别名ID
     */
    @TableId(value = "tag_alias_id")
    private Long tagAliasId;

    /**
     * 当前有效canonical标签ID
     */
    private Long tagId;

    /**
     * 历史标签slug，全局唯一
     */
    private String aliasSlug;

    /**
     * 历史标签名称
     */
    private String aliasName;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private Boolean delFlag;


}
