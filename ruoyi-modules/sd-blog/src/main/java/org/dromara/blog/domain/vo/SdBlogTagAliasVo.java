package org.dromara.blog.domain.vo;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.dromara.blog.domain.SdBlogTagAlias;
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
 * 博客标签历史别名视图对象 sd_blog_tag_alias
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SdBlogTagAlias.class)
public class SdBlogTagAliasVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 标签别名ID
     */
    @ExcelProperty(value = "标签别名ID")
    private Long tagAliasId;

    /**
     * 当前有效canonical标签ID
     */
    @ExcelProperty(value = "当前有效canonical标签ID")
    private Long tagId;

    /**
     * 历史标签slug，全局唯一
     */
    @ExcelProperty(value = "历史标签slug，全局唯一")
    private String aliasSlug;

    /**
     * 历史标签名称
     */
    @ExcelProperty(value = "历史标签名称")
    private String aliasName;

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
