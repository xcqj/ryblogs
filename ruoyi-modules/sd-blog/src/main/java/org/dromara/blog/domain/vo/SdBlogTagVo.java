package org.dromara.blog.domain.vo;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.dromara.blog.domain.SdBlogTag;
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
 * 博客标签视图对象 sd_blog_tag
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SdBlogTag.class)
public class SdBlogTagVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 标签ID
     */
    @ExcelProperty(value = "标签ID")
    private Long tagId;

    /**
     * 当前标签名称
     */
    @ExcelProperty(value = "当前标签名称")
    private String name;

    /**
     * 当前规范化标签slug，全局唯一
     */
    @ExcelProperty(value = "当前规范化标签slug，全局唯一")
    private String slug;

    /**
     * 标签状态（0正常 1已合并）
     */
    @ExcelProperty(value = "标签状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=正常,1=已合并")
    private Boolean status;

    /**
     * 合并目标标签ID，正常状态时为空
     */
    @ExcelProperty(value = "合并目标标签ID，正常状态时为空")
    private Long mergedInto;

    /**
     * 是否精选（0否 1是）
     */
    @ExcelProperty(value = "是否精选", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=否,1=是")
    private Boolean isFeatured;

    /**
     * 精选标签展示顺序
     */
    @ExcelProperty(value = "精选标签展示顺序")
    private Integer sortOrder;

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
