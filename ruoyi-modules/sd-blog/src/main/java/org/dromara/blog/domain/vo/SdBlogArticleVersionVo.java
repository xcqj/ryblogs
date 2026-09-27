package org.dromara.blog.domain.vo;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.dromara.blog.domain.SdBlogArticleVersion;
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
 * 博客文章版本历史视图对象 sd_blog_article_version
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SdBlogArticleVersion.class)
public class SdBlogArticleVersionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文章版本ID
     */
    @ExcelProperty(value = "文章版本ID")
    private Long versionId;

    /**
     * 所属文章ID
     */
    @ExcelProperty(value = "所属文章ID")
    private Long articleId;

    /**
     * 文章版本号，从1开始递增
     */
    @ExcelProperty(value = "文章版本号，从1开始递增")
    private Integer versionNo;

    /**
     * 文章标题快照
     */
    @ExcelProperty(value = "文章标题快照")
    private String title;

    /**
     * 文章摘要快照
     */
    @ExcelProperty(value = "文章摘要快照")
    private String summary;

    /**
     * 文章封面URL快照
     */
    @ExcelProperty(value = "文章封面URL快照")
    private String coverUrl;

    /**
     * 文章正文HTML快照（WangEditor内容，经安全处理后保存）
     */
    @ExcelProperty(value = "文章正文HTML快照", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "W=angEditor内容，经安全处理后保存")
    private String content;

    /**
     * 该版本创建者
     */
    @ExcelProperty(value = "该版本创建者")
    private Long createBy;

    /**
     * 版本创建时间
     */
    @ExcelProperty(value = "版本创建时间")
    private LocalDateTime createTime;


}
