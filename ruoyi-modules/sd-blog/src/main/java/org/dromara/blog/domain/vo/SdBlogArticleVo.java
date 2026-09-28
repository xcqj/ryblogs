package org.dromara.blog.domain.vo;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.dromara.blog.domain.SdBlogArticle;
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
 * 博客文章视图对象 sd_blog_article
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SdBlogArticle.class)
public class SdBlogArticleVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文章ID
     */
    @ExcelProperty(value = "文章ID")
    private Long articleId;

    /**
     * 作者用户ID
     */
    @ExcelProperty(value = "作者用户ID")
    private Long authorId;

    /**
     * 文章URL标识，MVP预留，全局唯一
     */
    @ExcelProperty(value = "文章URL标识，MVP预留，全局唯一")
    private String slug;

    /**
     * 文章状态（0草稿 1已发布）
     */
    @ExcelProperty(value = "文章状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=草稿,1=已发布")
    private String status;

    /**
     * 可见性（0公开 1用户隐藏 2管理员下线）
     */
    @ExcelProperty(value = "可见性", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=公开,1=用户隐藏,2=管理员下线")
    private String visibility;

    /**
     * 当前生效版本ID
     */
    @ExcelProperty(value = "当前生效版本ID")
    private Long currentVersionId;

    /**
     * 阅读量
     */
    @ExcelProperty(value = "阅读量")
    private Long viewCount;

    /**
     * 首次发布时间
     */
    @ExcelProperty(value = "首次发布时间")
    private LocalDateTime publishedAt;

    /**
     * 管理员下线原因
     */
    @ExcelProperty(value = "管理员下线原因")
    private String adminHiddenReason;

    /**
     * 执行管理员下线的用户ID
     */
    @ExcelProperty(value = "执行管理员下线的用户ID")
    private Long adminHiddenBy;

    /**
     * 管理员下线时间
     */
    @ExcelProperty(value = "管理员下线时间")
    private LocalDateTime adminHiddenTime;

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

    /**
     * 更新者
     */
    @ExcelProperty(value = "更新者")
    private Long updateBy;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private LocalDateTime updateTime;


}
