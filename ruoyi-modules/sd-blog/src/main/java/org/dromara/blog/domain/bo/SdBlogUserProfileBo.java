package org.dromara.blog.domain.bo;

import org.dromara.blog.domain.SdBlogUserProfile;
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
 * 博客用户资料业务对象 sd_blog_user_profile
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
@Data
@AutoMapper(target = SdBlogUserProfile.class, reverseConvertGenerate = false)
public class SdBlogUserProfileBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID，RuoYi sys_user.user_id，同时作为主键
     */
    @NotNull(message = "用户ID，RuoYi sys_user.user_id，同时作为主键不能为空", groups = { EditGroup.class })
    private Long userId;

    /**
     * 个人简介
     */
    private String bio;

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
