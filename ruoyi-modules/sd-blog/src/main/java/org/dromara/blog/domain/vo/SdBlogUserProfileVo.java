package org.dromara.blog.domain.vo;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.dromara.blog.domain.SdBlogUserProfile;
import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 博客用户资料视图对象 sd_blog_user_profile
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SdBlogUserProfile.class)
public class SdBlogUserProfileVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID，RuoYi sys_user.user_id，同时作为主键
     */
    @ExcelProperty(value = "用户ID，RuoYi sys_user.user_id，同时作为主键")
    private Long userId;

    /**
     * 个人简介
     */
    @ExcelProperty(value = "个人简介")
    private String bio;

    /**
     * 创建者
     */
    @ExcelProperty(value = "创建者")
    private Long createBy;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private LocalDateTime createTime;


}
