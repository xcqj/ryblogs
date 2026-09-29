package org.dromara.blog.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 博客文章保存业务对象（契约 §3.1 / §3.4）
 * <p>
 * 创建与保存共用；title/summary/coverUrl/content 为版本快照四字段；
 * tagNames 为原始标签名，由服务端规范化并按名解析/自动建标签。
 * 不接收 authorId——文章归属一律取当前登录用户（AGENTS.md §5 红线）。
 *
 * @author Xu
 * @date 2026-09-28
 */
@Data
public class BlogArticleSaveBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文章标题（Service 层纯文本化，剥离一切标签）
     */
    @NotBlank(message = "文章标题不能为空")
    @Size(max = 200, message = "文章标题长度不能超过200个字符")
    private String title;

    /**
     * 文章摘要（纯文本字段，Service 层纯文本化，列表/meta 场景直接当文本渲染）
     */
    @Size(max = 500, message = "文章摘要长度不能超过500个字符")
    private String summary;

    /**
     * 封面地址
     */
    @Size(max = 500, message = "封面地址长度不能超过500个字符")
    private String coverUrl;

    /**
     * 正文 HTML（Service 层入库前经 HtmlSanitizer 白名单净化，契约 §18.9 / D6）
     */
    @NotBlank(message = "文章正文不能为空")
    private String content;

    /**
     * 标签名称集合，每篇文章 1~6 个（规范化去重后仍须满足）
     */
    @NotEmpty(message = "文章至少需要1个标签")
    @Size(min = 1, max = 6, message = "文章标签数量必须为1~6个")
    private List<String> tagNames;

}
