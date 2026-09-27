package org.dromara.blog.domain.vo;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.dromara.blog.domain.SdBlogComment;
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
 * 博客评论视图对象 sd_blog_comment
 *
 * @author Xu
 * @date 2026-09-28 02:23:30
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SdBlogComment.class)
public class SdBlogCommentVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 评论ID
     */
    @ExcelProperty(value = "评论ID")
    private Long commentId;

    /**
     * 文章ID
     */
    @ExcelProperty(value = "文章ID")
    private Long articleId;

    /**
     * 评论用户ID
     */
    @ExcelProperty(value = "评论用户ID")
    private Long userId;

    /**
     * 父评论ID，MVP必须为空，预留未来回复功能
     */
    @ExcelProperty(value = "父评论ID，MVP必须为空，预留未来回复功能")
    private Long parentId;

    /**
     * 评论内容
     */
    @ExcelProperty(value = "评论内容")
    private String content;

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
