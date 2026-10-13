package org.dromara.blog.service.support;

import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 阅读量 Redis 异步计数的纯逻辑工具（无 Spring / Redis 依赖，可单测）。
 * <p>
 * 语义（ROADMAP S15 / D35）：PV 口径 + 同人窗口去重（登录 userId / 匿名 visitorId，窗口 30min 可配）；
 * 待增量先落 Redis hash，10s 定时以「增量 UPDATE」合回 DB；读值 = DB view_count + 待增量。
 * 键前缀为博客业务自有（{@code blog:view:*}），不放 {@code GlobalConstants}（其注释限定业务无关 key）。
 *
 * @author Xu
 * @date 2026-10-14
 */
public final class ViewCountSupport {

    /**
     * 阅读量去重键前缀：blog:view:dedup:{articleId}:{visitorKey}
     */
    public static final String DEDUP_KEY_PREFIX = "blog:view:dedup:";

    /**
     * 阅读量待增量 hash 键：blog:view:pending（field=articleId, value=delta）
     */
    public static final String PENDING_KEY = "blog:view:pending";

    /**
     * 去重窗口默认分钟数（配置缺失兜底）
     */
    public static final int DEFAULT_DEDUP_TTL_MINUTES = 30;

    private ViewCountSupport() {
    }

    /**
     * 构造去重键。访客键为空时调用方应改走直写路径，此处不产出可缓存键。
     *
     * @param articleId  文章ID
     * @param visitorKey 访客键（userId 或 visitorId，非空）
     * @return 去重键
     */
    public static String dedupKey(Long articleId, String visitorKey) {
        return DEDUP_KEY_PREFIX + articleId + ":" + visitorKey;
    }

    /**
     * 待增量 hash 键。
     */
    public static String pendingKey() {
        return PENDING_KEY;
    }

    /**
     * 分钟数转秒（Lua EX 参数）。非正值回退默认窗口，防配置错误把去重退化成永久键或立即过期。
     *
     * @param minutes 配置的窗口分钟数
     * @return 秒数（>0）
     */
    public static long ttlSeconds(int minutes) {
        if (minutes <= 0) {
            minutes = DEFAULT_DEDUP_TTL_MINUTES;
        }
        return (long) minutes * 60;
    }

    /**
     * 解析 flush Lua 返回的扁平数组（[field, value, field, value, ...]）为文章增量表。
     * <p>
     * 畸形项（奇数长度、非数字）逐个跳过而不是整批失败——单条脏数据不应阻断其余文章回写；
     * 相同 field 后值覆盖前值（hash 语义下不会出现，防御性处理）。
     *
     * @param flatEntries Lua 扁平返回（元素为 String / Number / byte[]，Redisson 解码形态）
     * @return articleId → delta（保留插入序，仅含可解析且 delta>0 的项）
     */
    public static Map<Long, Long> parsePendingEntries(List<Object> flatEntries) {
        Map<Long, Long> deltas = new LinkedHashMap<>();
        if (flatEntries == null || flatEntries.isEmpty()) {
            return deltas;
        }
        for (int i = 0; i + 1 < flatEntries.size(); i += 2) {
            Long articleId = parseLong(flatEntries.get(i));
            Long delta = parseLong(flatEntries.get(i + 1));
            if (articleId == null || delta == null || delta <= 0) {
                continue;
            }
            deltas.put(articleId, delta);
        }
        return deltas;
    }

    /**
     * 增量表 → 批量回写参数，按 articleId 升序保证确定性顺序（便于日志比对与回归）。
     *
     * @param deltas parsePendingEntries 产物
     * @return [[articleId, delta], ...]
     */
    public static List<long[]> toBatchParams(Map<Long, Long> deltas) {
        if (deltas == null || deltas.isEmpty()) {
            return List.of();
        }
        List<Long> articleIds = new ArrayList<>(deltas.keySet());
        articleIds.sort(Long::compareTo);
        List<long[]> params = new ArrayList<>(articleIds.size());
        for (Long articleId : articleIds) {
            params.add(new long[]{articleId, deltas.get(articleId)});
        }
        return params;
    }

    /**
     * 读值合成：DB view_count + 待增量（null 安全：DB 为 null 视作 0，delta 为 null 视作 0）。
     *
     * @param dbValue      库内阅读量（可能为 null）
     * @param pendingDelta 该文待增量（可能为 null）
     * @return 展示阅读量
     */
    public static long mergeViewCount(Long dbValue, Long pendingDelta) {
        return (dbValue == null ? 0L : dbValue) + (pendingDelta == null ? 0L : pendingDelta);
    }

    /**
     * 访客键优先级：登录 userId 优先于匿名 visitorId；两者都无返回 null（调用方走直写降级）。
     *
     * @param userId    登录用户ID（未登录为 null）
     * @param visitorId 匿名访客标识（缺失为 null/空串）
     * @return 访客键或 null
     */
    public static String visitorKeyOf(Long userId, String visitorId) {
        if (userId != null) {
            return "u" + userId;
        }
        return StrUtil.isBlank(visitorId) ? null : "v" + visitorId.trim();
    }

    private static Long parseLong(Object value) {
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
