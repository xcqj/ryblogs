package org.dromara.blog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 博客模块调度开关（S15 阅读量定时回写）。
 * <p>
 * 全仓此前无 Spring {@code @Scheduled} 基建（仅 SnailJob/Quartz，另一套体系），
 * 此处开最小调度支持：只开调度不建线程池——当前模块仅阅读量 flush 一个 10s 轻量任务，
 * 默认单线程调度器足够；将来博客模块任务增多再评估独立 TaskScheduler。
 *
 * @author Xu
 * @date 2026-10-14
 */
@Configuration
@EnableScheduling
public class BlogSchedulingConfig {
}
