package org.dromara.blog.es;

import org.dromara.blog.domain.SdBlogArticle;
import org.dromara.blog.domain.SdBlogArticleVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ArticleIndexDocBuilder 纯逻辑单元测试（不碰数据库 / ES / Spring 容器，符合 D7 单测策略）。
 * <p>
 * 覆盖正文 HTML → 纯文本提取（高亮片段不混 HTML 结构）、publishedAt ISO 序列化、
 * 标签空值兜底、doc 字段映射四类边界。
 *
 * @author Xu
 * @date 2026-10-14
 */
@DisplayName("索引文档构造纯逻辑单元测试")
class ArticleIndexDocBuilderTest {

    private static SdBlogArticleVersion version(String content) {
        SdBlogArticleVersion v = new SdBlogArticleVersion();
        v.setTitle("Python 爬虫入门");
        v.setSummary("从零开始写抓取");
        v.setContent(content);
        return v;
    }

    private static SdBlogArticle article() {
        SdBlogArticle a = new SdBlogArticle();
        a.setArticleId(1234567890123L);
        a.setPublishedAt(LocalDateTime.of(2026, 10, 14, 10, 30, 0));
        return a;
    }

    @Test
    @DisplayName("正文纯文本化：标签剥离、实体解码、空白规整")
    void plainTextExtraction() {
        String html = "<h2>标题</h2><p>第一段&nbsp;文字</p><pre><code>print(1)</code></pre>";
        assertEquals("标题 第一段 文字 print(1)", ArticleIndexDocBuilder.plainText(html));
    }

    @Test
    @DisplayName("正文纯文本化：script/style 内容被 jsoup 剔除；null/空串安全")
    void plainTextStripsNonText() {
        assertFalse(ArticleIndexDocBuilder.plainText("<script>alert(1)</script><p>正文</p>").contains("alert"));
        assertEquals("正文", ArticleIndexDocBuilder.plainText("<script>alert(1)</script><p>正文</p>"));
        assertEquals("", ArticleIndexDocBuilder.plainText(null));
        assertEquals("", ArticleIndexDocBuilder.plainText(""));
    }

    @Test
    @DisplayName("publishedAt：ISO 本地时间串（ES date 可直接解析）；null 兜底空串")
    void publishedAtFormat() {
        assertEquals("2026-10-14T10:30", ArticleIndexDocBuilder.formatPublishedAt(
            LocalDateTime.of(2026, 10, 14, 10, 30, 0)));
        assertEquals("2026-10-14T10:30:15", ArticleIndexDocBuilder.formatPublishedAt(
            LocalDateTime.of(2026, 10, 14, 10, 30, 15)));
        assertEquals("", ArticleIndexDocBuilder.formatPublishedAt(null));
    }

    @Test
    @DisplayName("doc 构造：字段映射正确，标签 null 兜底空列表")
    void docFromEntities() {
        BlogArticleDoc doc = ArticleIndexDocBuilder.from(article(),
            version("<p>用 <b>requests</b> 抓取</p>"), null);
        assertEquals(1234567890123L, doc.getArticleId());
        assertEquals("Python 爬虫入门", doc.getTitle());
        assertEquals("从零开始写抓取", doc.getSummary());
        assertEquals("用 requests 抓取", doc.getContent());
        assertEquals(List.of(), doc.getTags());
        assertEquals("2026-10-14T10:30", doc.getPublishedAt());

        BlogArticleDoc withTags = ArticleIndexDocBuilder.from(article(), version("<p>x</p>"),
            List.of("Python", "爬虫"));
        assertEquals(List.of("Python", "爬虫"), withTags.getTags());
    }

}
