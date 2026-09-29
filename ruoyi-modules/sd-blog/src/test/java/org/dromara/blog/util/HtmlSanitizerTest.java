package org.dromara.blog.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 富文本 HTML 净化纯逻辑单元测试（D7 策略·契约 §18.9 / AGENTS.md §9 安全红线）
 *
 * @author Xu
 */
@DisplayName("HtmlSanitizer 富文本净化")
class HtmlSanitizerTest {

    @Test
    @DisplayName("剥离 script 标签及其内容")
    void shouldRemoveScriptTag() {
        String out = HtmlSanitizer.sanitizeHtml("<p>安全段落</p><script>alert(1)</script>");
        assertFalse(out.contains("<script"));
        assertFalse(out.contains("alert"));
        assertTrue(out.contains("<p>安全段落</p>"));
    }

    @Test
    @DisplayName("剥离 on* 事件属性")
    void shouldRemoveEventAttributes() {
        assertFalse(HtmlSanitizer.sanitizeHtml("<img src=\"/a.png\" onerror=\"alert(1)\">").contains("onerror"));
        assertFalse(HtmlSanitizer.sanitizeHtml("<p onclick=\"steal()\">文本</p>").contains("onclick"));
    }

    @Test
    @DisplayName("拒绝 javascript: 等危险协议的链接与图片")
    void shouldDropDangerousProtocols() {
        assertFalse(HtmlSanitizer.sanitizeHtml("<a href=\"javascript:alert(1)\">x</a>").contains("javascript:"));
        assertFalse(HtmlSanitizer.sanitizeHtml("<img src=\"JAVASCRIPT:alert(1)\">").contains("alert"));
    }

    @Test
    @DisplayName("style 属性中的表达式类注入被移除")
    void shouldDropUnsafeStyleValue() {
        String out = HtmlSanitizer.sanitizeHtml("<p style=\"width:expression(alert(1))\">text</p>");
        assertFalse(out.contains("expression"));
        assertFalse(out.contains("style"));
        assertTrue(out.contains("text"));
        // 正常样式不受影响
        assertTrue(HtmlSanitizer.sanitizeHtml("<p style=\"text-align:center\">t</p>").contains("style=\"text-align:center\""));
    }

    @Test
    @DisplayName("iframe 等非白名单标签剥离")
    void shouldRemoveIframe() {
        assertFalse(HtmlSanitizer.sanitizeHtml("<iframe src=\"https://example.com\"></iframe>正文").contains("<iframe"));
    }

    @Test
    @DisplayName("技术博客常用排版完整保留")
    void shouldKeepCommonFormatting() {
        String html = "<h2>标题</h2><p style=\"color:#333\">红色<b>加粗</b></p>"
            + "<ul><li>条目</li></ul><ol start=\"3\"><li>有序</li></ol>"
            + "<pre><code class=\"language-java\">int a = 1;</code></pre>"
            + "<blockquote>引用</blockquote><hr>"
            + "<table><thead><tr><th colspan=\"2\">表头</th></tr></thead><tbody><tr><td>a</td><td>b</td></tr></tbody></table>"
            + "<a href=\"https://a.com\" target=\"_blank\">链接</a>"
            + "<img src=\"https://a.com/b.png\" alt=\"图\">"
            + "<img src=\"data:image/png;base64,AAA=" + "\">"
            + "<a href=\"#sec-1\">目录锚点</a>";
        String out = HtmlSanitizer.sanitizeHtml(html);
        for (String fragment : List.of("<h2>标题</h2>", "style=\"color:#333\"", "<b>加粗</b>", "<ul><li>条目</li></ul>",
            "<ol start=\"3\">", "<pre><code class=\"language-java\">", "<blockquote>引用</blockquote>", "<hr>",
            "<th colspan=\"2\">", "<a href=\"https://a.com\" target=\"_blank\">链接</a>",
            "<img src=\"https://a.com/b.png\"", "data:image/png;base64", "<a href=\"#sec-1\">")) {
            assertTrue(out.contains(fragment), "应保留: " + fragment + "，实际: " + out);
        }
    }

    @Test
    @DisplayName("净化幂等：二次净化结果不变（保证不产生伪版本差异）")
    void shouldIdempotent() {
        String once = HtmlSanitizer.sanitizeHtml("<p>x</p><script>alert(1)</script><a href=\"javascript:void(0)\">y</a>");
        assertEquals(once, HtmlSanitizer.sanitizeHtml(once));
    }

    @Test
    @DisplayName("纯文本字段：标签剥离只留文本，script 整段移除")
    void shouldSanitizePlainText() {
        assertEquals("加粗文字", HtmlSanitizer.sanitizePlainText("<b>加粗文字</b>"));
        assertFalse(HtmlSanitizer.sanitizePlainText("<script>alert(1)</script>标题").contains("alert"));
        assertEquals("标题", HtmlSanitizer.sanitizePlainText("标题"));
        // 摘要场景：富文本粘贴过来只留可见文本
        assertEquals("摘要里的标签", HtmlSanitizer.sanitizePlainText("<p>摘要里的<b>标签</b></p>"));
    }

    @Test
    @DisplayName("边界：正文仅含危险标签时净化后为空（Service 层据此拒绝入库）")
    void shouldReturnEmptyWhenOnlyDangerousTags() {
        assertEquals("", HtmlSanitizer.sanitizeHtml("<script>alert(1)</script>"));
        assertEquals("", HtmlSanitizer.sanitizeHtml("<iframe src=\"https://evil\"></iframe>"));
    }

    @Test
    @DisplayName("null 输入返回 null（字段可空语义不变）")
    void shouldPassThroughNull() {
        assertNull(HtmlSanitizer.sanitizeHtml(null));
        assertNull(HtmlSanitizer.sanitizePlainText(null));
    }

}
