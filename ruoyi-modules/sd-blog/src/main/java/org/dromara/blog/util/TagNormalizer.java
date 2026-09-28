package org.dromara.blog.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.core.utils.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 标签规范化纯函数工具（契约 §18.10，PRD TAG-03，database-design §2.3）
 * <p>
 * 规则：trim、合并连续空白、丢弃空项、按 slug 去重；
 * slug 生成：小写、空白转连字符、去除特殊字符（保留 Unicode 字母与数字）、折叠多余连字符。
 * 示例：{@code "  Python   爬虫 "} → name {@code "Python 爬虫"} → slug {@code "python-爬虫"}。
 * 纯静态无状态，单元测试覆盖；MERGED/alias 跟随逻辑不在本类（属 S6 标签治理，见 ROADMAP）。
 *
 * @author Xu
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TagNormalizer {

    /**
     * 连续空白
     */
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /**
     * 规范化用户输入的标签名称：trim + 合并内部连续空白为单个空格
     *
     * @param rawName 原始输入
     * @return 规范化名称；输入为空返回空串
     */
    public static String normalizeName(String rawName) {
        if (StringUtils.isBlank(rawName)) {
            return StringUtils.EMPTY;
        }
        return WHITESPACE.matcher(rawName.trim()).replaceAll(StringUtils.SPACE);
    }

    /**
     * 由规范化名称生成 slug：小写、空白转连字符、去除特殊字符、折叠并去除首尾连字符
     *
     * @param normalizedName 规范化名称（{@link #normalizeName} 的产物）
     * @return slug；输入无有效字符时返回空串
     */
    public static String toSlug(String normalizedName) {
        if (StringUtils.isBlank(normalizedName)) {
            return StringUtils.EMPTY;
        }
        String lower = normalizedName.trim().toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(lower.length());
        for (char c : lower.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                sb.append(c);
            } else if (Character.isWhitespace(c) || c == '-' || c == '_' || c == '.') {
                sb.append('-');
            }
            // 其余特殊字符直接丢弃
        }
        // 折叠连续连字符并去掉首尾
        String slug = WHITESPACE.matcher(sb).replaceAll("-");
        slug = Pattern.compile("-{2,}").matcher(slug).replaceAll("-");
        return StringUtils.strip(slug, "-");
    }

    /**
     * 名称 → slug 一步到位
     *
     * @param rawName 原始输入
     * @return slug；无有效字符返回空串
     */
    public static String slugOf(String rawName) {
        return toSlug(normalizeName(rawName));
    }

    /**
     * 批量规范化名称列表：丢弃空白项、按 slug 去重（保持输入顺序）
     *
     * @param rawNames 原始名称列表
     * @return 去重后的规范化名称列表（按 slug 唯一）
     */
    public static List<String> normalizeNames(List<String> rawNames) {
        List<String> result = new ArrayList<>();
        if (rawNames == null) {
            return result;
        }
        Set<String> seenSlugs = new LinkedHashSet<>();
        for (String raw : rawNames) {
            String name = normalizeName(raw);
            String slug = toSlug(name);
            if (StringUtils.isBlank(slug)) {
                continue;
            }
            if (seenSlugs.add(slug)) {
                result.add(name);
            }
        }
        return result;
    }
}
