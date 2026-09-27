package org.dromara.blog.domain.bo;

import org.dromara.blog.domain.SdBlogComment;
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
 * 博客评论业务对象 sd_blog_comment
 *
 * @author Xu
 * @date 2026-09-28 02:23:30
 */
@Data
@AutoMapper(target = SdBlogComment.class, reverseConvertGenerate = false)
public class SdBlogCommentBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 评论ID
     */
    @NotNull(message = "评论ID不能为空", groups = { EditGroup.class })
    private Long commentId;

    /**
     * 文章ID
     */
    @NotNull(message = "文章ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long articleId;

    /**
     * 评论用户ID
     */
    @NotNull(message = "评论用户ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long userId;

    /**
     * 父评论ID，MVP必须为空，预留未来回复功能
     */
    private Long parentId;

    /**
     * 评论内容
     */
    @NotBlank(message = "评论内容不能为空", groups = { AddGroup.class, EditGroup.class })
    private String content;

    /**
     * 创建者
     */
    private Long createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 查询参数
     */
    private Map<String, Object> params = new HashMap<>();

}
