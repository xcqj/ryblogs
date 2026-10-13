package org.dromara.web.service.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SocialAccountNamer 纯逻辑单元测试（不碰数据库、不启 Spring 容器，符合 D7 单测策略）。
 * <p>
 * 覆盖账号清洗、长度截断、空值回退、后缀消歧四类契约边界；
 * 冲突的存在性检查由 Service 持锁执行，不在本类范围。
 *
 * @author Xu
 * @date 2026-10-13
 */
@DisplayName("社交登录账号派生纯逻辑单元测试")
class SocialAccountNamerTest {

    @Test
    @DisplayName("账号清洗：合法字符小写保留，非法字符剔除")
    void sanitizeUserNameKeepsLegalCharsOnly() {
        assertEquals("zhangsan", SocialAccountNamer.sanitizeUserName("ZhangSan"));
        assertEquals("hello-world_99", SocialAccountNamer.sanitizeUserName("Hello-World_99"));
        // 点号 / 中文 / 空格 / 表情等一律剔除
        assertEquals("zhangsan", SocialAccountNamer.sanitizeUserName("zhang.san 张三😀"));
    }

    @Test
    @DisplayName("账号清洗：空值与无合法字符返回 null（触发 source_uuid 回退）")
    void sanitizeUserNameReturnsNullWhenNoLegalChar() {
        assertNull(SocialAccountNamer.sanitizeUserName(null));
        assertNull(SocialAccountNamer.sanitizeUserName(""));
        assertNull(SocialAccountNamer.sanitizeUserName("   "));
        assertNull(SocialAccountNamer.sanitizeUserName("张三.李四"));
    }

    @Test
    @DisplayName("兜底账号：source_uuid 形态且不超 30 字符")
    void fallbackUserNameIsSourcePlusUuid() {
        assertEquals("github_12345678", SocialAccountNamer.fallbackUserName("github", "12345678"));
        assertEquals("gitee_12345678", SocialAccountNamer.fallbackUserName("Gitee", "12345678"));
        // 超长 uuid 截断到 30，不撑爆 varchar(30)
        String longUuid = "9".repeat(50);
        String fallback = SocialAccountNamer.fallbackUserName("github", longUuid);
        assertEquals(SocialAccountNamer.USER_NAME_MAX, fallback.length());
        assertTrue(fallback.startsWith("github_"));
    }

    @Test
    @DisplayName("昵称规范化：空值回退账号名，超长截断到 30")
    void normalizeNickNameFallsBackAndTruncates() {
        assertEquals("疯狂的狮子Li", SocialAccountNamer.normalizeNickName("疯狂的狮子Li", "github_lisi"));
        assertEquals("github_lisi", SocialAccountNamer.normalizeNickName(null, "github_lisi"));
        assertEquals("github_lisi", SocialAccountNamer.normalizeNickName("   ", "github_lisi"));
        String longNick = "这".repeat(50);
        String nick = SocialAccountNamer.normalizeNickName(longNick, "github_lisi");
        assertEquals(SocialAccountNamer.NICK_NAME_MAX, nick.length());
    }

    @Test
    @DisplayName("后缀拼接：总长不超过账号上限且保持 base_suffix 形态")
    void withSuffixKeepsLengthLimit() {
        assertEquals("zhangsan_x3k9", SocialAccountNamer.withSuffix("zhangsan", "x3k9"));
        String longBase = "a".repeat(50);
        String result = SocialAccountNamer.withSuffix(longBase, "x3k9");
        assertEquals(SocialAccountNamer.USER_NAME_MAX, result.length());
        assertTrue(result.endsWith("_x3k9"));
    }

    @Test
    @DisplayName("数字后缀候选：base_1 形态且保长")
    void withIndexSuffixFormat() {
        assertEquals("zhangsan_1", SocialAccountNamer.withIndexSuffix("zhangsan", 1));
        assertEquals("zhangsan_9", SocialAccountNamer.withIndexSuffix("zhangsan", 9));
        String longBase = "b".repeat(50);
        assertEquals(SocialAccountNamer.USER_NAME_MAX, SocialAccountNamer.withIndexSuffix(longBase, 9).length());
    }

    @Test
    @DisplayName("随机后缀：长度与字符集符合约定")
    void randomSuffixLengthAndCharset() {
        for (int i = 0; i < 20; i++) {
            String suffix = SocialAccountNamer.randomSuffix(SocialAccountNamer.NICK_NAME_SUFFIX_LENGTH);
            assertEquals(SocialAccountNamer.NICK_NAME_SUFFIX_LENGTH, suffix.length());
            assertTrue(suffix.matches("[a-z0-9]+"));
        }
        // 防御下限：长度参数异常时不抛错
        assertEquals(1, SocialAccountNamer.randomSuffix(0).length());
    }

}
