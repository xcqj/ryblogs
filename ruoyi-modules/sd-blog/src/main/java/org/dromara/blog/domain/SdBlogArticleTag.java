package org.dromara.blog.domain;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serial;

/**
 * 博客文章标签关系对象 sd_blog_article_tag
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sd_blog_article_tag")
public class SdBlogArticleTag extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文章标签关系ID
     */
    @TableId(value = "article_tag_id")
    private Long articleTagId;

    /**
     * 文章ID
     */
    private Long articleId;

    /**
     * 标签ID
     */
    private Long tagId;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private Boolean delFlag;


}
