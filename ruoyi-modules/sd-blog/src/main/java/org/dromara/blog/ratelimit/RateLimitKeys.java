package org.dromara.blog.ratelimit;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.core.constant.GlobalConstants;
import org.dromara.common.core.utils.StringUtils;

/**
 * 限流 Redis 键组装（S18，ROADMAP §3 S18 / §6 S18-D1）
 * <p>
 * 纯静态无状态，键形态即备案事实（ROADMAP §6 S18-D1 与 application.yml {@code blog.rate-limit} 注释
 * 三处同源），单元测试直接断言字面量，便于面试演示时与 Redis 实测键核对。
 * 前缀复用框架 {@link GlobalConstants#RATE_LIMIT_KEY}（{@code global:rate_limit:}），
 * 与框架 {@code @RateLimiter} 注解路径同一命名空间，运维可按前缀统一观测。
 * <p>
 * 三个限流点键形态：
 * <ul>
 *     <li>登录 {@code /auth/login}：{@code auth/login:{ip}:{username}}（IP+username，防暴力破解）；</li>
 *     <li>发表评论 {@code /blog/article/{articleId}/comments}：{@code blog/article/comments:{userId}}（按登录用户，不区分文章）；</li>
 *     <li>公共搜索 {@code /blog/public/search}：{@code blog/public/search:{ip}}（按客户端 IP，匿名场景）。</li>
 * </ul>
 *
 * @author Xu
 * @date 2026-10-15
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RateLimitKeys {

    /**
     * 登录限流键（/auth/login，IP+username）
     *
     * @param ip       客户端 IP（ServletUtils.getClientIP）
     * @param username 登录用户名；非密码 grant（email/sms/xcx）无用户名字段时为 null，退化为同 IP 共享键
     * @return 限流 Redis 键
     */
    public static String login(String ip, String username) {
        return GlobalConstants.RATE_LIMIT_KEY + "auth/login:" + ip + ":"
            + (username == null ? StringUtils.EMPTY : username);
    }

    /**
     * 发表评论限流键（/blog/article/{articleId}/comments，用户键）
     *
     * @param userId 当前登录用户ID
     * @return 限流 Redis 键
     */
    public static String comment(Long userId) {
        return GlobalConstants.RATE_LIMIT_KEY + "blog/article/comments:" + userId;
    }

    /**
     * 公共搜索限流键（/blog/public/search，IP 键）
     *
     * @param ip 客户端 IP（ServletUtils.getClientIP）
     * @return 限流 Redis 键
     */
    public static String search(String ip) {
        return GlobalConstants.RATE_LIMIT_KEY + "blog/public/search:" + ip;
    }

}
