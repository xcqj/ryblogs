package org.dromara.blog.domain.bo;

import org.dromara.blog.domain.SdBlogTag;
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
 * 博客标签业务对象 sd_blog_tag
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
@Data
@AutoMapper(target = SdBlogTag.class, reverseConvertGenerate = false)
public class SdBlogTagBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 标签ID
     */
    @NotNull(message = "标签ID不能为空", groups = { EditGroup.class })
    private Long tagId;

    /**
     * 当前标签名称
     */
    @NotBlank(message = "当前标签名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String name;

    /**
     * 当前规范化标签slug，全局唯一
     */
    @NotBlank(message = "当前规范化标签slug，全局唯一不能为空", groups = { AddGroup.class, EditGroup.class })
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
    @NotNull(message = "是否精选（0否 1是）不能为空", groups = { AddGroup.class, EditGroup.class })
    private Boolean isFeatured;

    /**
     * 精选标签展示顺序
     */
    @NotNull(message = "精选标签展示顺序不能为空", groups = { AddGroup.class, EditGroup.class })
    private Integer sortOrder;

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
