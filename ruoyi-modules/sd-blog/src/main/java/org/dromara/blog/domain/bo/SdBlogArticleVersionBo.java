package org.dromara.blog.domain.bo;

import org.dromara.blog.domain.SdBlogArticleVersion;
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
 * 博客文章版本历史业务对象 sd_blog_article_version
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
@Data
@AutoMapper(target = SdBlogArticleVersion.class, reverseConvertGenerate = false)
public class SdBlogArticleVersionBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文章版本ID
     */
    @NotNull(message = "文章版本ID不能为空", groups = { EditGroup.class })
    private Long versionId;

    /**
     * 所属文章ID
     */
    @NotNull(message = "所属文章ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long articleId;

    /**
     * 文章版本号，从1开始递增
     */
    @NotNull(message = "文章版本号，从1开始递增不能为空", groups = { AddGroup.class, EditGroup.class })
    private Integer versionNo;

    /**
     * 文章标题快照
     */
    private String title;

    /**
     * 文章摘要快照
     */
    private String summary;

    /**
     * 文章封面URL快照
     */
    private String coverUrl;

    /**
     * 文章正文HTML快照（WangEditor内容，经安全处理后保存）
     */
    private String content;

    /**
     * 该版本创建者
     */
    private Long createBy;

    /**
     * 版本创建时间
     */
    private LocalDateTime createTime;

    /**
     * 查询参数
     */
    private Map<String, Object> params = new HashMap<>();

}
