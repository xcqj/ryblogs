package org.dromara.blog.domain.bo;

import org.dromara.blog.domain.SdBlogArticle;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import io.github.linpeilie.annotations.AutoMapper;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 博客文章业务对象 sd_blog_article
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
@Data
@AutoMapper(target = SdBlogArticle.class, reverseConvertGenerate = false)
public class SdBlogArticleBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文章ID
     */
    @NotNull(message = "文章ID不能为空", groups = { EditGroup.class })
    private Long articleId;

    /**
     * 作者用户ID
     */
    @NotNull(message = "作者用户ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long authorId;

    /**
     * 文章URL标识，MVP预留，全局唯一
     */
    private String slug;

    /**
     * 文章状态（0草稿 1已发布）
     */
    @NotNull(message = "文章状态（0草稿 1已发布）不能为空", groups = { AddGroup.class, EditGroup.class })
    private String status;

    /**
     * 可见性（0公开 1用户隐藏 2管理员下线）
     */
    @NotBlank(message = "可见性（0公开 1用户隐藏 2管理员下线）不能为空", groups = { AddGroup.class, EditGroup.class })
    private String visibility;

    /**
     * 当前生效版本ID
     */
    @NotNull(message = "当前生效版本ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long currentVersionId;

    /**
     * 阅读量
     */
    @NotNull(message = "阅读量不能为空", groups = { AddGroup.class, EditGroup.class })
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
     * 创建者
     */
    private Long createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    private Long updateBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 查询参数
     */
    private Map<String, Object> params = new HashMap<>();

}
