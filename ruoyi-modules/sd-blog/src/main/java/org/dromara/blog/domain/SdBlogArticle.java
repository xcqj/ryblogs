package org.dromara.blog.domain;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serial;

/**
 * 博客文章对象 sd_blog_article
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sd_blog_article")
public class SdBlogArticle extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文章ID
     */
    @TableId(value = "article_id")
    private Long articleId;

    /**
     * 作者用户ID
     */
    private Long authorId;

    /**
     * 文章URL标识，MVP预留，全局唯一
     */
    private String slug;

    /**
     * 文章状态（0草稿 1已发布），取值见 BlogArticleStatus
     */
    private String status;

    /**
     * 可见性（0公开 1用户隐藏 2管理员下线）
     */
    private String visibility;

    /**
     * 当前生效版本ID
     */
    private Long currentVersionId;

    /**
     * 阅读量
     */
    private Long viewCount;

    /**
     * 首次发布时间
     */
    private LocalDateTime publishedAt;

    /**
     * 管理员下线原因
     */
    private String adminHiddenReason;

    /**
     * 执行管理员下线的用户ID
     */
    private Long adminHiddenBy;

    /**
     * 管理员下线时间
     */
    private LocalDateTime adminHiddenTime;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;


}
