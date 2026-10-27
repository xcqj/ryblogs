package org.dromara.blog.domain.vo;

import lombok.Data;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论列表项视图对象（契约 §10.1 / §12.1 共用）
 * <p>
 * 数据最小化：不含 parentId（MVP 恒 NULL）与审计字段；昵称实时翻译不复制。
 *
 * @author Xu
 * @date 2026-10-01
 */
@Data
public class BlogCommentListItemVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 评论ID
     */
    private Long commentId;

    /**
     * 文章ID
     */
    private Long articleId;

    /**
     * 所属文章标题（治理列表展示用；由 Service 批量回填，避免逐条查询）
     */
    private String articleTitle;

    /**
     * 评论内容（纯文本）
     */
    private String content;

    /**
     * 评论用户ID
     */
    private Long userId;

    /**
     * 评论用户昵称（实时翻译）
     */
    @Translation(type = TransConstant.USER_ID_TO_NAME, mapper = "userId")
    private String userName;

    /**
     * 评论时间
     */
    private LocalDateTime createTime;

}
