package org.dromara.blog.domain;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serial;

/**
 * 博客标签对象 sd_blog_tag
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sd_blog_tag")
public class SdBlogTag extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 标签ID
     */
    @TableId(value = "tag_id")
    private Long tagId;

    /**
     * 当前标签名称
     */
    private String name;

    /**
     * 当前规范化标签slug，全局唯一
     */
    private String slug;

    /**
     * 标签状态（0正常 1已合并）
     */
    private Boolean status;

    /**
     * 合并目标标签ID，正常状态时为空
     */
    private Long mergedInto;

    /**
     * 是否精选（0否 1是）
     */
    private Boolean isFeatured;

    /**
     * 精选标签展示顺序
     */
    private Integer sortOrder;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private Boolean delFlag;


}
