package org.dromara.blog.domain.bo;

import org.dromara.blog.domain.SdBlogArticleTag;
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
 * 博客文章标签关系业务对象 sd_blog_article_tag
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
@Data
@AutoMapper(target = SdBlogArticleTag.class, reverseConvertGenerate = false)
public class SdBlogArticleTagBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文章标签关系ID
     */
    @NotNull(message = "文章标签关系ID不能为空", groups = { EditGroup.class })
    private Long articleTagId;

    /**
     * 文章ID
     */
    @NotNull(message = "文章ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long articleId;

    /**
     * 标签ID
     */
    @NotNull(message = "标签ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long tagId;

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
