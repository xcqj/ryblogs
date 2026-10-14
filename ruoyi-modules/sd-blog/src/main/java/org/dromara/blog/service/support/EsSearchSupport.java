package org.dromara.blog.service.support;

import cn.hutool.core.collection.CollUtil;
import org.dromara.blog.domain.vo.ArticleHighlightVo;
import org.dromara.blog.domain.vo.BlogPublicArticleListItemVo;
import org.dromara.blog.es.ArticleIndexManager;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * ES 搜索纯逻辑支撑（ROADMAP S16，零框架/零 ES 客户端依赖，可单测）。
 * <p>
 * 三件事：① ES 命中序把回表结果重排（打分序不被 MySQL 返回序污染）；
 * ② 深翻页游标编解码（opaque：score + publishedAt(epochMilli) + articleId）；
 * ③ highlight 片段合并进列表 VO。
 *
 * @author Xu
 * @date 2026-10-14
 */
public final class EsSearchSupport {

    private EsSearchSupport() {
    }

    /**
     * 深翻页游标解码值（对应 ES 排序 [_score desc, publishedAt desc, articleId desc] 的 sort 值）
     *
     * @param score                相关度打分
     * @param publishedAtEpochMilli 发布时间（epoch 毫秒，date 字段 sort 值的规范形态）
     * @param articleId            文章ID
     */
    public record CursorValues(double score, long publishedAtEpochMilli, long articleId) {

    }

    /**
     * 编码深翻页游标（URL-safe Base64 去 padding，可安全进 query 参数）
     */
    public static String encodeCursor(double score, long publishedAtEpochMilli, long articleId) {
        String raw = score + "|" + publishedAtEpochMilli + "|" + articleId;
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * 文档 publishedAt（ISO 本地时间串）→ epoch 毫秒，**按 UTC 解释**。
     * <p>
     * ⚠ 时区陷阱（S16 联调实测）：ES 对无时区日期串按 <b>UTC</b> 解析（date 字段默认格式
     * strict_date_optional_time），date sort 值 = 该串按 UTC 的毫秒数；编码游标时若按
     * 系统本地时区（东八区 +08:00）换算，游标边界会整体偏移 8 小时——search_after 的
     * publishedAt 边界错位，深翻页结果与 pageNum 翻页不一致。两端必须同一解释。
     */
    public static long publishedAtToEpochMilli(String publishedAtIso) {
        return java.time.LocalDateTime.parse(publishedAtIso)
            .toEpochSecond(java.time.ZoneOffset.UTC) * 1000;
    }

    /**
     * 解码深翻页游标；非法格式（截断/污染/手工构造）返回 null——调用方按「无游标」退化到 pageNum
     */
    public static CursorValues decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), java.nio.charset.StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|", -1);
            if (parts.length != 3) {
                return null;
            }
            return new CursorValues(Double.parseDouble(parts[0]),
                Long.parseLong(parts[1]), Long.parseLong(parts[2]));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 按 ES 命中序重排回表结果。
     * <p>
     * - 命中序中的 id 在回表结果里没有（ES 查询后被隐藏/删除，回表三条件已过滤）→ 自然缺席；
     * - 回表结果出现命中序外的 id（不应发生：回表以命中 id 集合 in 限定）→ 防御性丢弃，
     *   保证 VO 顺序严格等于 ES 打分序，不被 MySQL 默认序污染。
     *
     * @param rows         MySQL 回表结果
     * @param hitOrderIds  ES 命中 id 序（打分序）
     * @param idExtractor  行 → id 提取器
     * @return 按命中序排列的行（size ≤ min(rows, hitOrderIds)）
     */
    public static <T> List<T> reorderByHitOrder(List<T> rows, List<Long> hitOrderIds, Function<T, Long> idExtractor) {
        if (CollUtil.isEmpty(rows) || CollUtil.isEmpty(hitOrderIds)) {
            return List.of();
        }
        Map<Long, T> byId = new HashMap<>();
        for (T row : rows) {
            byId.put(idExtractor.apply(row), row);
        }
        List<T> ordered = new ArrayList<>(hitOrderIds.size());
        for (Long id : hitOrderIds) {
            T row = byId.remove(id);
            if (row != null) {
                ordered.add(row);
            }
        }
        return ordered;
    }

    /**
     * 把 ES highlight 片段合并进列表 VO（无片段/空 map 时不动 VO 的 highlight）。
     *
     * @param row       列表行
     * @param fragments 该行 highlight（字段名 → 片段列表；ES 保证片段内 HTML 已转义）
     */
    public static void applyHighlight(BlogPublicArticleListItemVo row, Map<String, List<String>> fragments) {
        if (CollUtil.isEmpty(fragments)) {
            return;
        }
        ArticleHighlightVo highlight = new ArticleHighlightVo();
        highlight.setTitle(pick(fragments, ArticleIndexManager.FIELD_TITLE));
        highlight.setSummary(pick(fragments, ArticleIndexManager.FIELD_SUMMARY));
        highlight.setContent(pick(fragments, ArticleIndexManager.FIELD_CONTENT));
        row.setHighlight(highlight);
    }

    private static List<String> pick(Map<String, List<String>> fragments, String field) {
        List<String> value = fragments.get(field);
        return CollUtil.isEmpty(value) ? null : value;
    }

}
