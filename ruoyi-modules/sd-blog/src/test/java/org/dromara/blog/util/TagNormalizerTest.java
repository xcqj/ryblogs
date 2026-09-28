package org.dromara.blog.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 标签规范化纯逻辑单元测试（D7 策略①）
 *
 * @author Xu
 */
@DisplayName("TagNormalizer 标签规范化")
class TagNormalizerTest {

    @Test
    @DisplayName("名称规范化：trim 并合并连续空白")
    void shouldNormalizeName() {
        assertEquals("Python 爬虫", TagNormalizer.normalizeName("  Python   爬虫 "));
        assertEquals("", TagNormalizer.normalizeName("   "));
        assertEquals("", TagNormalizer.normalizeName(null));
    }

    @Test
    @DisplayName("slug 生成：小写、空白转连字符、去特殊字符")
    void shouldGenerateSlug() {
        assertEquals("python-爬虫", TagNormalizer.toSlug("Python 爬虫"));
        assertEquals("ruoyi-vue-plus", TagNormalizer.toSlug("RuoYi-Vue-Plus"));
        assertEquals("机器学习-深度学习", TagNormalizer.toSlug("机器学习 && 深度学习"));
        assertEquals("ai", TagNormalizer.toSlug("AI!!"));
        assertEquals("", TagNormalizer.toSlug("!!!"));
    }

    @Test
    @DisplayName("一步到位：原始名称直接得 slug")
    void shouldSlugOf() {
        assertEquals("java", TagNormalizer.slugOf("  Java   "));
    }

    @Test
    @DisplayName("批量规范化：丢弃空白项并按 slug 去重、保持顺序")
    void shouldDeduplicateBySlug() {
        List<String> result = TagNormalizer.normalizeNames(List.of("Java", " java ", "Spring", "Java", "  "));
        assertEquals(List.of("Java", "Spring"), result);
    }

    @Test
    @DisplayName("null 输入返回空列表不抛异常")
    void shouldTolerateNullList() {
        assertEquals(List.of(), TagNormalizer.normalizeNames(null));
    }
}
