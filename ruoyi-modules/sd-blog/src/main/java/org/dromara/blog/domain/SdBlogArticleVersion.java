package org.dromara.blog.domain;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serial;

/**
 * 博客文章版本历史对象 sd_blog_article_version
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sd_blog_article_version")
public class SdBlogArticleVersion extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文章版本ID
     */
    @TableId(value = "version_id")
    private Long versionId;

    /**
     * 所属文章ID
     */
    private Long articleId;

    /**
     * 文章版本号，从1开始递增
     */
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
     * 删除标志（0代表存在 1代表删除），业务层禁止删除版本
     */
    @TableLogic
    private String delFlag;


}
