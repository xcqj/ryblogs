package org.dromara.blog.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MessageUtils;
import org.dromara.common.core.utils.ServletUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.redisson.api.RateType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 接口限流校验器（S18，ROADMAP §3 S18 / §6 S18-D1）
 * <p>
 * 形态决策（S18-D1）：<b>不采用框架 {@code @RateLimiter} 注解</b>——其 {@code count/time} 是编译期
 * 字面量，读不到 application.yml，与 S18「规则配置化不硬编码」要求冲突；改为阈值全部配置化
 * （{@code blog.rate-limit.*}，遵循本模块 S15/S16 的 {@code @Value} 内联默认约定）+ 本类直调框架
 * {@link RedisUtils#rateLimiter}（Redisson {@code RRateLimiter} 令牌桶，与框架切面同一入口），
 * 并复用框架 i18n 文案 {@code rate.limiter.message}。
 * <p>
 * 超限行为与注解路径<b>完全一致</b>：抛 {@link ServiceException} → {@code GlobalExceptionHandler}
 * → {@code R.fail(文案)}，即 HTTP 200 + envelope code 500 + msg「访问过于频繁，请稍候再试」
 * （<b>不是 HTTP 429</b>，RuoYi 全局统一信封风格）；Redis 异常按框架同口径包装为
 * {@code RuntimeException("服务器限流异常，请稍候再试")}。
 * <p>
 * 限流点与键形态见 {@link RateLimitKeys}；阈值调参见 application.yml {@code blog.rate-limit} 段。
 *
 * @author Xu
 * @date 2026-10-15
 */
@Slf4j
@Component
public class RateLimitChecker {

    /**
     * 限流策略存活时间（秒）：与框架 {@code @RateLimiter} 默认 timeout（86400=1 天）一致。
     * Redisson 令牌桶策略数据超时清除，窗口到期后自然重新计取。
     */
    private static final int STRATEGY_TIMEOUT = 86400;

    /**
     * 登录限流：同一 IP+username 在 {@code blog.rate-limit.login.time} 秒内最多
     * {@code blog.rate-limit.login.count} 次（防暴力破解，与账号密码错误锁定互补）
     */
    @Value("${blog.rate-limit.login.count:5}")
    private int loginCount;

    @Value("${blog.rate-limit.login.time:60}")
    private int loginTime;

    /**
     * 评论限流：同一登录用户在 {@code blog.rate-limit.comment.time} 秒内最多
     * {@code blog.rate-limit.comment.count} 次（与 @RepeatSubmit 防重互补：防重挡同内容，
     * 限流挡高频不同内容）
     */
    @Value("${blog.rate-limit.comment.count:10}")
    private int commentCount;

    @Value("${blog.rate-limit.comment.time:60}")
    private int commentTime;

    /**
     * 公共搜索限流：同一 IP 在 {@code blog.rate-limit.search.time} 秒内最多
     * {@code blog.rate-limit.search.count} 次（保护 ES/MySQL 搜索链路，匿名场景按 IP）
     */
    @Value("${blog.rate-limit.search.count:30}")
    private int searchCount;

    @Value("${blog.rate-limit.search.time:60}")
    private int searchTime;

    /**
     * 登录限流校验（{@code /auth/login}，IP+username 键）
     *
     * @param username 登录用户名；非密码 grant（email/sms/xcx）无用户名字段时传 null，退化为同 IP 共享键
     */
    public void checkLogin(String username) {
        guard(RateLimitKeys.login(ServletUtils.getClientIP(), username), loginCount, loginTime);
    }

    /**
     * 发表评论限流校验（{@code /blog/article/{articleId}/comments}，用户键）
     *
     * @param userId 当前登录用户ID（LoginHelper.getUserId()，接口已要求登录）
     */
    public void checkComment(Long userId) {
        guard(RateLimitKeys.comment(userId), commentCount, commentTime);
    }

    /**
     * 公共搜索限流校验（{@code /blog/public/search}，IP 键，匿名场景）
     */
    public void checkSearch() {
        guard(RateLimitKeys.search(ServletUtils.getClientIP()), searchCount, searchTime);
    }

    /**
     * 扣减令牌并在超限时阻断请求；异常包装口径与框架 RateLimiterAspect 一致。
     *
     * @param key   限流 Redis 键（见 {@link RateLimitKeys}）
     * @param count 窗口内最大次数
     * @param time  窗口时长（秒）
     */
    private void guard(String key, int count, int time) {
        try {
            long remain = RedisUtils.rateLimiter(key, RateType.OVERALL, count, time, STRATEGY_TIMEOUT);
            if (remain == -1L) {
                log.warn("接口限流触发 => key: '{}', 阈值: {}/{}s", key, count, time);
                throw new ServiceException(MessageUtils.message("rate.limiter.message"));
            }
            log.debug("限流通过 => key: '{}', 阈值: {}/{}s, 剩余令牌: {}", key, count, time, remain);
        } catch (Exception e) {
            if (e instanceof ServiceException) {
                throw e;
            }
            throw new RuntimeException("服务器限流异常，请稍候再试", e);
        }
    }

}
