package org.dromara.blog.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 发表评论业务对象（契约 §10.2）
 * <p>
 * 仅 content 一个字段：article_id 取路径、user_id 取登录上下文、parent_id 恒 NULL，
 * 均不接收前端传入（AGENTS.md §5 红线）。
 *
 * @author Xu
 * @date 2026-10-01
 */
@Data
public class BlogCommentAddBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 评论内容（纯文本，Service 层剥离粘贴的 HTML 标签）
     */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容长度不能超过1000个字符")
    private String content;

}
