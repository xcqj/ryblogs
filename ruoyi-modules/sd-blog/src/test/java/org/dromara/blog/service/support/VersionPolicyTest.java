package org.dromara.blog.service.support;

import org.dromara.blog.domain.SdBlogArticleVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文章版本策略单元测试（D7 策略③，四字段比对）
 *
 * @author Xu
 */
@DisplayName("VersionPolicy 版本策略")
class VersionPolicyTest {

    private SdBlogArticleVersion current() {
        SdBlogArticleVersion v = new SdBlogArticleVersion();
        v.setTitle("标题");
        v.setSummary("摘要");
        v.setCoverUrl("cover");
        v.setContent("<p>正文</p>");
        return v;
    }

    @Test
    @DisplayName("无当前版本视为有变化")
    void shouldTreatNullCurrentAsChanged() {
        assertTrue(VersionPolicy.contentChanged(null, "标题", "摘要", "cover", "<p>正文</p>"));
    }

    @Test
    @DisplayName("四字段全同不产生变化")
    void shouldDetectNoChange() {
        assertFalse(VersionPolicy.contentChanged(current(), "标题", "摘要", "cover", "<p>正文</p>"));
    }

    @Test
    @DisplayName("null 与空串等价，不产生假变化")
    void shouldTreatNullAsEmpty() {
        SdBlogArticleVersion v = current();
        v.setSummary(null);
        v.setCoverUrl("");
        assertFalse(VersionPolicy.contentChanged(v, "标题", "", null, "<p>正文</p>"));
    }

    @Test
    @DisplayName("任一字段变化即产生新版本")
    void shouldDetectFieldChange() {
        assertTrue(VersionPolicy.contentChanged(current(), "新标题", "摘要", "cover", "<p>正文</p>"));
        assertTrue(VersionPolicy.contentChanged(current(), "标题", "新摘要", "cover", "<p>正文</p>"));
        assertTrue(VersionPolicy.contentChanged(current(), "标题", "摘要", "new-cover", "<p>正文</p>"));
        assertTrue(VersionPolicy.contentChanged(current(), "标题", "摘要", "cover", "<p>新正文</p>"));
    }

    @Test
    @DisplayName("版本号从 1 开始单调递增")
    void shouldComputeNextVersionNo() {
        assertEquals(1, VersionPolicy.nextVersionNo(null));
        assertEquals(4, VersionPolicy.nextVersionNo(3));
    }
}
