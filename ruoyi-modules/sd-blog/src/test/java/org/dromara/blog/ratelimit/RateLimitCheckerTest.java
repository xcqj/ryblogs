package org.dromara.blog.ratelimit;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.ServletUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 接口限流校验契约单元测试（S18；mock Redisson 不连真实 Redis，范式同 RedisAspectContractTest）。
 * <p>
 * 覆盖三个限流点的键/阈值/timeout 绑定契约、超限拒绝（框架 i18n 文案）、
 * Redis 异常包装口径；字段阈值经 ReflectionTestUtils 绑定，验证与 application.yml 默认值同源。
 *
 * @author Xu
 * @date 2026-10-15
 */
@DisplayName("接口限流校验契约")
class RateLimitCheckerTest {

    private static RedissonClient redissonClient;

    /**
     * 注入仅包含 mock RedissonClient 的内存 Spring 容器，
     * 使框架 RedisUtils / MessageUtils 静态工具类在测试内可初始化。
     */
    @BeforeAll
    static void initializeRateLimitInfrastructure() {
        redissonClient = mock(RedissonClient.class);
        StaticApplicationContext context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton("redissonClient", redissonClient);
        context.refresh();
        // StaticApplicationContext 刷新时自带内部 messageSource 单例，直接向其注入 i18n 文案
        StaticMessageSource messageSource = context.getBean("messageSource", StaticMessageSource.class);
        messageSource.addMessage("rate.limiter.message", Locale.getDefault(), "访问过于频繁，请稍候再试");
        messageSource.addMessage("rate.limiter.message", Locale.SIMPLIFIED_CHINESE, "访问过于频繁，请稍候再试");
        new SpringUtil().setApplicationContext(context);
    }

    private RateLimitChecker checker;

    /**
     * 每个用例前重置 mock 与校验器，阈值绑定为 application.yml {@code blog.rate-limit} 默认值。
     */
    @BeforeEach
    void setUpChecker() {
        reset(redissonClient);
        checker = new RateLimitChecker();
        ReflectionTestUtils.setField(checker, "loginCount", 5);
        ReflectionTestUtils.setField(checker, "loginTime", 60);
        ReflectionTestUtils.setField(checker, "commentCount", 10);
        ReflectionTestUtils.setField(checker, "commentTime", 60);
        ReflectionTestUtils.setField(checker, "searchCount", 30);
        ReflectionTestUtils.setField(checker, "searchTime", 60);
    }

    @Test
    @DisplayName("登录限流：IP+username 键、配置阈值、超限抛框架文案")
    void checkLoginUsesIpAndUsernameKey() {
        RRateLimiter limiter = mock(RRateLimiter.class);
        when(redissonClient.getRateLimiter("global:rate_limit:auth/login:10.0.0.9:alice")).thenReturn(limiter);
        when(limiter.tryAcquire()).thenReturn(true, false);
        when(limiter.availablePermits()).thenReturn(9L);

        try (MockedStatic<ServletUtils> servlet = mockStatic(ServletUtils.class)) {
            servlet.when(ServletUtils::getClientIP).thenReturn("10.0.0.9");

            assertDoesNotThrow(() -> checker.checkLogin("alice"));
            ServiceException exception = assertThrows(ServiceException.class, () -> checker.checkLogin("alice"));

            assertEquals("访问过于频繁，请稍候再试", exception.getMessage());
            verify(limiter, times(2)).trySetRate(RateType.OVERALL, 5, Duration.ofSeconds(60),
                Duration.ofSeconds(86400));
            verify(limiter, times(2)).tryAcquire();
        }
    }

    @Test
    @DisplayName("登录限流：username 为 null 时键退化为同 IP 共享段")
    void checkLoginWithoutUsernameDegradesToIpKey() {
        RRateLimiter limiter = mock(RRateLimiter.class);
        when(redissonClient.getRateLimiter("global:rate_limit:auth/login:10.0.0.9:")).thenReturn(limiter);
        when(limiter.tryAcquire()).thenReturn(true);

        try (MockedStatic<ServletUtils> servlet = mockStatic(ServletUtils.class)) {
            servlet.when(ServletUtils::getClientIP).thenReturn("10.0.0.9");
            assertDoesNotThrow(() -> checker.checkLogin(null));
        }
        verify(limiter).trySetRate(RateType.OVERALL, 5, Duration.ofSeconds(60), Duration.ofSeconds(86400));
    }

    @Test
    @DisplayName("评论限流：userId 键、独立阈值，不依赖请求上下文")
    void checkCommentUsesUserIdKey() {
        RRateLimiter limiter = mock(RRateLimiter.class);
        when(redissonClient.getRateLimiter("global:rate_limit:blog/article/comments:1001")).thenReturn(limiter);
        when(limiter.tryAcquire()).thenReturn(true);
        when(limiter.availablePermits()).thenReturn(0L);

        assertDoesNotThrow(() -> checker.checkComment(1001L));

        verify(limiter).trySetRate(RateType.OVERALL, 10, Duration.ofSeconds(60), Duration.ofSeconds(86400));
        verify(limiter).tryAcquire();
    }

    @Test
    @DisplayName("搜索限流：IP 键、独立阈值（匿名场景）")
    void checkSearchUsesIpKey() {
        RRateLimiter limiter = mock(RRateLimiter.class);
        when(redissonClient.getRateLimiter("global:rate_limit:blog/public/search:10.0.0.9")).thenReturn(limiter);
        when(limiter.tryAcquire()).thenReturn(true);

        try (MockedStatic<ServletUtils> servlet = mockStatic(ServletUtils.class)) {
            servlet.when(ServletUtils::getClientIP).thenReturn("10.0.0.9");
            assertDoesNotThrow(() -> checker.checkSearch());
        }
        verify(limiter).trySetRate(RateType.OVERALL, 30, Duration.ofSeconds(60), Duration.ofSeconds(86400));
    }

    @Test
    @DisplayName("Redis 异常：按框架同口径包装为运行时异常，不泄露限流细节")
    void redisFailureWrappedAsRuntimeException() {
        // Redisson 4.x 运行期异常均为 RuntimeException 子类；guard 对非 ServiceException 一律包装
        when(redissonClient.getRateLimiter(anyString())).thenThrow(new RuntimeException("redis down"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> checker.checkComment(1L));

        assertEquals("服务器限流异常，请稍候再试", exception.getMessage());
    }

}
