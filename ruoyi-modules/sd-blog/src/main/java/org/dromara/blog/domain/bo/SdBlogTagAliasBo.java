package org.dromara.blog.domain.bo;

import org.dromara.blog.domain.SdBlogTagAlias;
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
 * 博客标签历史别名业务对象 sd_blog_tag_alias
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
@Data
@AutoMapper(target = SdBlogTagAlias.class, reverseConvertGenerate = false)
public class SdBlogTagAliasBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 标签别名ID
     */
    @NotNull(message = "标签别名ID不能为空", groups = { EditGroup.class })
    private Long tagAliasId;

    /**
     * 当前有效canonical标签ID
     */
    @NotNull(message = "当前有效canonical标签ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long tagId;

    /**
     * 历史标签slug，全局唯一
     */
    @NotBlank(message = "历史标签slug，全局唯一不能为空", groups = { AddGroup.class, EditGroup.class })
    private String aliasSlug;

    /**
     * 历史标签名称
     */
    @NotBlank(message = "历史标签名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String aliasName;

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
