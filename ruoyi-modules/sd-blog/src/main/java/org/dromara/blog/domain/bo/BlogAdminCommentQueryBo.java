package org.dromara.blog.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 管理员评论列表查询业务对象（契约 §12.1）
 * <p>
 * 不用 Map params（GET 空值绑定 500 坑，见 blog_s5 修复先例）。
 *
 * @author Xu
 * @date 2026-10-01
 */
@Data
public class BlogAdminCommentQueryBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文章ID，空为全部
     */
    private Long articleId;

    /**
     * 评论用户ID，空为全部
     */
    private Long userId;

    /**
     * 内容关键字（模糊）
     */
    private String keyword;

}
