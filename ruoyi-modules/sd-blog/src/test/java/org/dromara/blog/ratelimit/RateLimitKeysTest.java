package org.dromara.blog.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 限流键组装纯逻辑单元测试（S18；键形态即 ROADMAP §6 S18-D1 备案事实）。
 *
 * @author Xu
 * @date 2026-10-15
 */
@DisplayName("限流键组装纯逻辑")
class RateLimitKeysTest {

    @Test
    @DisplayName("登录键：框架前缀 + IP + username")
    void loginKeyWithUsername() {
        assertEquals("global:rate_limit:auth/login:10.0.0.9:alice",
            RateLimitKeys.login("10.0.0.9", "alice"));
    }

    @Test
    @DisplayName("登录键：username 为 null（非密码 grant）时退化为同 IP 共享键")
    void loginKeyWithoutUsername() {
        assertEquals("global:rate_limit:auth/login:10.0.0.9:",
            RateLimitKeys.login("10.0.0.9", null));
    }

    @Test
    @DisplayName("评论键：按 userId，不携带文章维度（全站统一每用户配额）")
    void commentKeyByUserId() {
        assertEquals("global:rate_limit:blog/article/comments:1001",
            RateLimitKeys.comment(1001L));
    }

    @Test
    @DisplayName("搜索键：按客户端 IP（匿名场景无用户维度）")
    void searchKeyByIp() {
        assertEquals("global:rate_limit:blog/public/search:10.0.0.9",
            RateLimitKeys.search("10.0.0.9"));
    }

}
