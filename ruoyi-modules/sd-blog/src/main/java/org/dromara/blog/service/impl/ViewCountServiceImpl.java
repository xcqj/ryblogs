package org.dromara.blog.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.blog.mapper.SdBlogArticleMapper;
import org.dromara.blog.service.ViewCountService;
import org.dromara.blog.service.support.ViewCountSupport;
import org.dromara.common.redis.utils.RedisUtils;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 阅读量异步计数实现（ROADMAP S15 / D35）。
 * <p>
 * 人工复核点（AGENTS.md §9）：flush 逐条增量写（{@code view_count + delta} + {@code del_flag='0'} 守卫）、
 * Redis 故障降级路径（见 {@code countAndGetViewCount} 分支注释）。
 *
 * @author Xu
 * @date 2026-10-14
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ViewCountServiceImpl implements ViewCountService {

    /**
     * 计数 Lua：KEYS[1]=去重键, KEYS[2]=待增量 hash, ARGV[1]=articleId, ARGV[2]=TTL(秒)。
     * SETNX NX EX 成功才 HINCRBY——去重与计数同一次 RTT 原子完成，杜绝「SETNX 后崩溃丢计数」；
     * 返回 1=本次计入（hincrby 结果），0=窗口内重复访问被去重。
     */
    private static final String LUA_COUNT = ""
        + "if redis.call('set', KEYS[1], '1', 'EX', ARGV[2], 'NX') then "
        + "return redis.call('hincrby', KEYS[2], ARGV[1], 1) "
        + "end "
        + "return 0";

    /**
     * flush Lua：KEYS[1]=待增量 hash。hgetall + del 在同一脚本内原子完成
     * （Redis 单线程执行脚本期间无并发写），避免「先读后删」丢失期间的增量。
     * 返回扁平数组 [field, value, ...]。
     */
    private static final String LUA_FLUSH_DRAIN = ""
        + "local all = redis.call('hgetall', KEYS[1]) "
        + "redis.call('del', KEYS[1]) "
        + "return all";

    private final SdBlogArticleMapper sdBlogArticleMapper;

    /**
     * 同人去重窗口（分钟）。配置缺失兜底默认值，防启动失败。
     */
    @Value("${blog.view.dedup-ttl-minutes:30}")
    private int dedupTtlMinutes;

    /**
     * {@inheritDoc}
     */
    @Override
    public long countAndGetViewCount(Long articleId, Long userId, String visitorId, Long dbViewCount) {
        String visitorKey = ViewCountSupport.visitorKeyOf(userId, visitorId);
        if (visitorKey == null) {
            // 无访客标识（未登录且无 visitorId）：无法去重，退化为 S8 直写
            return directIncrease(articleId, dbViewCount);
        }
        try {
            RedissonClient client = RedisUtils.getClient();
            client.getScript().eval(
                RScript.Mode.READ_WRITE,
                LUA_COUNT,
                RScript.ReturnType.LONG,
                List.of(ViewCountSupport.dedupKey(articleId, visitorKey), ViewCountSupport.pendingKey()),
                String.valueOf(articleId),
                String.valueOf(ViewCountSupport.ttlSeconds(dedupTtlMinutes))
            );
            // 读该文待增量（含本次）；与计数分两次 RTT，极端并发下展示值可能相差 1，
            // 属 D35「重复/丢失计数为计数场景可接受取舍」口径
            Object pending = client.getMap(ViewCountSupport.pendingKey()).get(String.valueOf(articleId));
            return ViewCountSupport.mergeViewCount(dbViewCount, toLong(pending));
        } catch (Exception e) {
            log.warn("阅读量计数 Redis 链路异常，降级直写 DB, articleId={}", articleId, e);
            return directIncrease(articleId, dbViewCount);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Scheduled(fixedDelayString = "${blog.view.flush-interval-ms:10000}")
    @Override
    public void flushPendingViewCounts() {
        List<Object> drained;
        try {
            RedissonClient client = RedisUtils.getClient();
            drained = client.getScript().eval(
                RScript.Mode.READ_WRITE,
                LUA_FLUSH_DRAIN,
                RScript.ReturnType.LIST,
                List.of(ViewCountSupport.pendingKey())
            );
        } catch (Exception e) {
            // drain 失败（Redis 不可用）：待增量原样保留，下一周期重试，不丢数据
            log.warn("阅读量待增量 drain 失败，本周期跳过，下周期重试", e);
            return;
        }
        Map<Long, Long> deltas = ViewCountSupport.parsePendingEntries(drained);
        if (deltas.isEmpty()) {
            return;
        }
        // 逐条独立提交（不套整批事务）：单条失败仅丢该 delta 并续跑，符合 D35 取舍口径
        int failed = 0;
        for (long[] param : ViewCountSupport.toBatchParams(deltas)) {
            try {
                sdBlogArticleMapper.addViewCount(param[0], param[1]);
            } catch (Exception e) {
                failed++;
                log.error("阅读量回写 DB 失败, articleId={}, delta={}", param[0], param[1], e);
            }
        }
        log.info("阅读量回写完成, 文章数={}, 失败数={}", deltas.size(), failed);
    }

    /**
     * 降级/无访客标识路径：直写 DB +1，返回库内原值 +1（与 S8 行为完全一致，无需回查）。
     */
    private long directIncrease(Long articleId, Long dbViewCount) {
        sdBlogArticleMapper.increaseViewCount(articleId);
        return ViewCountSupport.mergeViewCount(dbViewCount, 1L);
    }

    /**
     * Redis hash 值 → Long（Redisson 解码可能为 Integer/Long/String/byte[]）。
     */
    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
