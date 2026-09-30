package org.dromara.blog.domain.vo;

import lombok.Data;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理员文章列表项视图对象（契约 §11.1）
 * <p>
 * 含全部状态与治理信息；title 取自当前生效版本（两段式批量填充）。
 *
 * @author Xu
 * @date 2026-09-30
 */
@Data
public class BlogAdminArticleListItemVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文章ID
     */
    private Long articleId;

    /**
     * 作者用户ID
     */
    private Long authorId;

    /**
     * 作者昵称（实时翻译）
     */
    @Translation(type = TransConstant.USER_ID_TO_NAME, mapper = "authorId")
    private String authorName;

    /**
     * 标题（当前版本）
     */
    private String title;

    /**
     * 文章状态（0草稿 1已发布）
     */
    private String status;

    /**
     * 可见性（0公开 1用户隐藏 2管理员下线）
     */
    private String visibility;

    /**
     * 阅读量
     */
    private Long viewCount;

    /**
     * 首次发布时间
     */
    private LocalDateTime publishedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 下线原因（治理信息）
     */
    private String adminHiddenReason;

    /**
     * 下线操作管理员ID
     */
    private Long adminHiddenBy;

    /**
     * 下线操作管理员昵称（治理信息）
     */
    @Translation(type = TransConstant.USER_ID_TO_NAME, mapper = "adminHiddenBy")
    private String adminHiddenByName;

    /**
     * 下线时间（治理信息）
     */
    private LocalDateTime adminHiddenTime;

}
