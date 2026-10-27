package org.dromara.blog.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 富文本 HTML 净化纯函数工具（契约 §18.9 / §9 安全红线，ROADMAP D6 决策）
 * <p>
 * 基于 jsoup Safelist 白名单清洗：不在白名单的标签/属性一律剥离，
 * {@code on*} 事件属性与 javascript: 等危险协议被丢弃；另对 style 值做防御性关键字剔除
 * （jsoup 15+ 不再过滤 style 值中的 IE 遗留表达式）。
 * 白名单范围为"技术博客常用集"：标准排版标签 + 表格 + 代码块 + 图片 + 链接，
 * 允许 class/style/alt/title/target 等展示属性与 img 的 data:image 内联图；不放开 iframe 等媒体嵌入。
 * 清洗操作幂等：对已净化的 HTML 再次执行结果不变（保证 S4 内容比对不因净化产生伪差异）。
 * 纯静态无状态，单元测试覆盖。
 *
 * @author Xu
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HtmlSanitizer {

    /**
     * 解析基址：供协议校验解析相对路径 / # 锚点（无基址时 jsoup 无法判定会被丢弃）；
     * 输出因 preserveRelativeLinks(true) 仍保留原样
     */
    private static final String BASE_URI = "https://blog.local/";

    /**
     * style 值防御性剔除的危险关键字（IE 遗留表达式/行为绑定；小写匹配）
     */
    private static final String[] DANGEROUS_STYLE_KEYWORDS = {"expression(", "behavior:", "javascript:", "vbscript:"};

    /**
     * 代码高亮 token 类名的唯一合法格式（S24 / D43）。
     * <p>
     * 前端用 shiki 的 {@code transformerStyleToClass} 生成 {@code tok-xxxxxx}（6 位小写字母数字），
     * 后端只放行这一种字面量格式，其余 class 一律剥除——把「放开 span[class]」的风险
     * 收窄到可枚举的范围，避免脏数据或作者手写语义类名（如 {@code header}/{@code admin}）
     * 撞上站点 CSS。
     */
    private static final Pattern SAFE_TOKEN_CLASS = Pattern.compile("^tok-[a-z0-9]+$");

    /**
     * 允许携带 style/id 公共属性的标签集（jsoup 无全局属性 API，逐标签声明）
     */
    private static final String[] STYLED_TAGS = {
        "h1", "h2", "h3", "h4", "h5", "h6", "p", "div", "span", "a", "img", "br", "hr",
        "strong", "em", "b", "i", "u", "s", "del", "ins", "mark", "small", "sub", "sup",
        "blockquote", "pre", "code", "kbd", "samp", "var",
        "ul", "ol", "li", "dl", "dt", "dd",
        "table", "thead", "tbody", "tfoot", "tr", "th", "td", "caption", "colgroup", "col",
        "figure", "figcaption", "details", "summary"
    };

    /**
     * 技术博客常用 Safelist：relaxed 基础集 + 列表/表格/代码块/hr/锚点，
     * 全局允许 class/style/id，a 额外允许 target/rel；协议含 data（供 img 内联图）。
     */
    private static final Safelist SAFELIST = buildSafelist();

    /**
     * 正文净化器：白名单外标签剥离，标签内文本保留
     */
    private static final Cleaner HTML_CLEANER = new Cleaner(SAFELIST);

    /**
     * 标题净化器：空白名单清洗，所有标签剥离、内部文本保留，产出纯文本
     */
    private static final Cleaner TEXT_CLEANER = new Cleaner(Safelist.none());

    private static Safelist buildSafelist() {
        Safelist safelist = Safelist.relaxed()
            // relaxed 已含 p/h1-h3/strong/em/u/i/s/a/img/blockquote/pre/code/div/span 与 http/https/mailto 协议校验
            .addTags("h4", "h5", "h6", "ol", "ul", "li", "hr",
                "table", "thead", "tbody", "tfoot", "tr", "th", "td", "caption", "colgroup", "col",
                "kbd", "samp", "var", "sub", "sup", "details", "summary", "figure", "figcaption")
            .addAttributes("a", "target", "rel")
            .addAttributes("th", "colspan", "rowspan")
            .addAttributes("td", "colspan", "rowspan")
            .addAttributes("ol", "start", "type")
            // 语法高亮标记（language-java 等）：relaxed 未给 pre/code 放开 class，技术博客必须保留
            .addAttributes("pre", "class")
            .addAttributes("code", "class")
            // 代码高亮 token 类（S24/D43）：relaxed 同样不给 span 放开 class。
            // 这里放行只是避免 Cleaner 提前剥掉，真正的取值校验在 sanitizeHtml 末尾的
            // stripUnsafeTokenClasses（只留 ^tok-[a-z0-9]+$）。
            .addAttributes("span", "class")
            // 编辑器图片协议：data:image 内联图（粘贴截图）+ oss://ossId 标记（“上传图片”落 OSS 后由
            // 前端 Editor 编码为 oss://ossId，回显时经 listByIds 解析为可用 URL）；不放行会被 jsoup 剥 src 致正文图丢失
            .addProtocols("img", "src", "data", "oss")
            // 保留 # Fragment 等非 URL 形式的相对锚点链接（长文目录）
            .preserveRelativeLinks(true);
        // 内联样式与锚点 id：WangEditor 颜色/字号/对齐依赖 style，逐标签放开
        for (String tag : STYLED_TAGS) {
            safelist.addAttributes(tag, "style", "id");
        }
        return safelist;
    }

    /**
     * 净化富文本正文：白名单外的标签剥离（保留其内部文本），危险属性与协议丢弃
     *
     * @param html 原始 HTML，可为 null
     * @return 净化后的 HTML 片段；输入为 null 时返回 null
     */
    public static String sanitizeHtml(String html) {
        if (html == null) {
            return null;
        }
        Document doc = Jsoup.parseBodyFragment(html, BASE_URI);
        stripDangerousStyles(doc);
        Document cleaned = HTML_CLEANER.clean(doc);
        // span 的 class 在 Safelist 里是整类放行，这里再按格式收一遍（S24/D43）
        stripUnsafeTokenClasses(cleaned);
        return tidyOutput(cleaned);
    }

    /**
     * 净化纯文本字段（如标题）：空白名单清洗，剥离一切标签只留文本；
     * script/style 整段移除，on* 事件属性与危险协议随标签一并丢弃。
     * 全局 XssFilter 对博客接口豁免后，标题防注入由本方法兜底。
     *
     * @param text 原始文本，可为 null
     * @return 纯文本；输入为 null 时返回 null
     */
    public static String sanitizePlainText(String text) {
        if (text == null) {
            return null;
        }
        Document doc = Jsoup.parseBodyFragment(text, BASE_URI);
        return tidyOutput(TEXT_CLEANER.clean(doc));
    }

    /**
     * style 值命中危险关键字时整个属性移除（保守策略：宁可丢样式不可留脚本入口）
     */
    private static void stripDangerousStyles(Document doc) {
        for (Element el : doc.select("[style]")) {
            String style = el.attr("style").toLowerCase(Locale.ROOT);
            for (String keyword : DANGEROUS_STYLE_KEYWORDS) {
                if (style.contains(keyword)) {
                    el.removeAttr("style");
                    break;
                }
            }
        }
    }

    /**
     * span 上的 class 只保留高亮 token（{@code tok-xxxxxx}），其余全部移除（S24 / D43）。
     * <p>
     * 为什么要额外收一道：{@link #buildSafelist()} 里 {@code span} 的 class 是整类放行，
     * jsoup 无法按值过滤。这里按 {@link #SAFE_TOKEN_CLASS} 逐个校验，
     * 既能放行前端 shiki 生成的高亮类，又能挡掉作者手写或脏数据带来的语义类名。
     * <p>
     * 幂等：对已净化的内容再执行一次结果不变（合法 class 保留、空 class 被移除），
     * 满足类注释里「S4 内容比对不因净化产生伪差异」的要求。
     *
     * @param doc 已通过 Safelist 清洗的文档
     */
    private static void stripUnsafeTokenClasses(Document doc) {
        for (Element el : doc.select("span[class]")) {
            StringBuilder safe = new StringBuilder();
            for (String token : el.attr("class").trim().split("\\s+")) {
                if (!token.isEmpty() && SAFE_TOKEN_CLASS.matcher(token).matches()) {
                    if (safe.length() > 0) {
                        safe.append(' ');
                    }
                    safe.append(token);
                }
            }
            if (safe.length() == 0) {
                el.removeAttr("class");
            } else {
                el.attr("class", safe.toString());
            }
        }
    }

    /**
     * 输出紧凑 HTML：关闭 prettyPrint，避免 jsoup 默认缩进换行改变编辑器排版
     */
    private static String tidyOutput(Document cleaned) {
        cleaned.outputSettings()
            .syntax(Document.OutputSettings.Syntax.html)
            .prettyPrint(false);
        return cleaned.body().html();
    }

}
