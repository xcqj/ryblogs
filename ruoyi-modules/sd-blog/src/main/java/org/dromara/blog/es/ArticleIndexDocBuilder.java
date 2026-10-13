package org.dromara.blog.es;

import org.dromara.blog.domain.SdBlogArticle;
import org.dromara.blog.domain.SdBlogArticleVersion;
import org.jsoup.Jsoup;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章 → 索引文档构造（ROADMAP S16，纯逻辑、零框架依赖，可单测）。
 * <p>
 * content 纯文本化：入库正文是已净化 HTML（HtmlSanitizer 白名单，D6），
 * 索引用 jsoup 再去标签取纯文本——既让 IK 按文本分词，也保证 highlight
 * 片段不携带 HTML 结构（ES 默认编码器另会转义片段内其余 HTML，前端可信任）。
 *
 * @author Xu
 * @date 2026-10-14
 */
public final class ArticleIndexDocBuilder {

    private ArticleIndexDocBuilder() {
    }

    /**
     * 由文章主表 + 当前生效版本 + 标签名构造索引文档。
     *
     * @param article  文章主表实体（publishedAt 取此表的首次发布时间，不随编辑更新）
     * @param version  当前生效版本（title/summary/content 来源）
     * @param tagNames 标签名集合（可空）
     */
    public static BlogArticleDoc from(SdBlogArticle article, SdBlogArticleVersion version, List<String> tagNames) {
        BlogArticleDoc doc = new BlogArticleDoc();
        doc.setArticleId(article.getArticleId());
        doc.setTitle(version.getTitle());
        doc.setSummary(version.getSummary());
        doc.setContent(plainText(version.getContent()));
        doc.setTags(tagNames == null ? List.of() : tagNames);
        doc.setPublishedAt(formatPublishedAt(article.getPublishedAt()));
        return doc;
    }

    /**
     * 净化后 HTML → 纯文本（jsoup 解析取 text；null/空串安全）
     */
    public static String plainText(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        return Jsoup.parse(html).text();
    }

    /**
     * publishedAt → ISO 本地时间串；未发布文章（草稿）为 null——草稿不入索引，null 不应出现，
     * 兜底为空串避免 ES date 解析报错（空串在 mapping 下按 null 处理）
     */
    public static String formatPublishedAt(LocalDateTime publishedAt) {
        return publishedAt == null ? "" : publishedAt.toString();
    }

}
