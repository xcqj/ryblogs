package org.dromara.blog.domain;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serial;

/**
 * 博客评论对象 sd_blog_comment
 *
 * @author Xu
 * @date 2026-09-28 02:23:30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sd_blog_comment")
public class SdBlogComment extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 评论ID
     */
    @TableId(value = "comment_id")
    private Long commentId;

    /**
     * 文章ID
     */
    private Long articleId;

    /**
     * 评论用户ID
     */
    private Long userId;

    /**
     * 父评论ID，MVP必须为空，预留未来回复功能
     */
    private Long parentId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;


}
