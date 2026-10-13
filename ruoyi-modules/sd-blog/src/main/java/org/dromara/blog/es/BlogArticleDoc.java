package org.dromara.blog.es;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 博客文章索引文档（ROADMAP S16，契约 §6）。
 * <p>
 * 只收录公共可见文章（status=1 AND visibility=0 AND del_flag=0，AGENTS.md §9）：
 * 索引生命周期与文章状态绑定——发布/重新公开入索引，隐藏/下线/删除移出索引。
 * <p>
 * 字段与 mapping（{@link ArticleIndexManager}）对应：
 * articleId keyword / title text(ik_max_word) / summary text(ik_smart) /
 * content text(ik_smart，<b>净化后纯文本</b>) / tags keyword（标签名）/ publishedAt date。
 * <p>
 * publishedAt 用 ISO 本地时间串（{@code LocalDateTime.toString()}，如 2026-10-14T10:30:00）：
 * ES date 字段可直接解析；所有值同一口径偏移，排序相对序不受影响；同时避免
 * Jackson 对 LocalDateTime 的序列化格式依赖 mapper 配置。
 *
 * @author Xu
 * @date 2026-10-14
 */
@Data
public class BlogArticleDoc implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文章ID（keyword；同时作为 ES 文档 _id 的字符串形态，回表拼 VO 用）
     */
    private Long articleId;

    /**
     * 标题（当前生效版本快照）
     */
    private String title;

    /**
     * 摘要（当前生效版本快照）
     */
    private String summary;

    /**
     * 正文（当前生效版本，<b>净化后 HTML 再提取的纯文本</b>——高亮片段不混 HTML 结构）
     */
    private String content;

    /**
     * 标签名列表（keyword，不分词；标签 rename/merge 后由对账任务兜底刷新）
     */
    private List<String> tags;

    /**
     * 首次发布时间（ISO 本地时间串）
     */
    private String publishedAt;

}
