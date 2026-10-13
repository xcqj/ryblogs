package org.dromara.blog.service.support;

import org.dromara.blog.domain.vo.ArticleHighlightVo;
import org.dromara.blog.domain.vo.BlogPublicArticleListItemVo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EsSearchSupport 纯逻辑单元测试（不碰数据库 / ES / Spring 容器，符合 D7 单测策略）。
 * <p>
 * 覆盖深翻页游标编解码（含非法输入退化）、ES 命中序重排（顺序保持 / 缺席 / 防御性丢弃）、
 * highlight 片段合并三类边界。
 *
 * @author Xu
 * @date 2026-10-14
 */
@DisplayName("ES 搜索纯逻辑单元测试")
class EsSearchSupportTest {

    private static BlogPublicArticleListItemVo row(Long articleId) {
        BlogPublicArticleListItemVo vo = new BlogPublicArticleListItemVo();
        vo.setArticleId(articleId);
        return vo;
    }

    @Test
    @DisplayName("游标编解码：往返一致，URL-safe Base64 无 query 参数敏感字符")
    void cursorRoundTrip() {
        String cursor = EsSearchSupport.encodeCursor(3.75, 1760000000000L, 9876543210123L);
        assertFalse(cursor.contains("="));
        assertFalse(cursor.contains("+"));
        assertFalse(cursor.contains("/"));
        EsSearchSupport.CursorValues values = EsSearchSupport.decodeCursor(cursor);
        assertNotNull(values);
        assertEquals(3.75, values.score());
        assertEquals(1760000000000L, values.publishedAtEpochMilli());
        assertEquals(9876543210123L, values.articleId());
    }

    @Test
    @DisplayName("游标解码：null/空串/非法 Base64/段数不足/数字畸形全部退化 null")
    void cursorDecodeInvalid() {
        assertNull(EsSearchSupport.decodeCursor(null));
        assertNull(EsSearchSupport.decodeCursor(""));
        assertNull(EsSearchSupport.decodeCursor("   "));
        assertNull(EsSearchSupport.decodeCursor("!!!not-base64!!!"));
        // 合法 Base64 但段数不是 3
        assertNull(EsSearchSupport.decodeCursor(
            java.util.Base64.getUrlEncoder().encodeToString("1|2".getBytes())));
        // 段数够但数字畸形
        assertNull(EsSearchSupport.decodeCursor(
            java.util.Base64.getUrlEncoder().encodeToString("x|y|z".getBytes())));
    }

    @Test
    @DisplayName("命中序重排：严格按 ES 打分序，MySQL 返回序不影响结果")
    void reorderKeepsHitOrder() {
        List<BlogPublicArticleListItemVo> rows = List.of(row(30L), row(10L), row(20L));
        List<BlogPublicArticleListItemVo> ordered = EsSearchSupport.reorderByHitOrder(
            rows, List.of(10L, 20L, 30L), BlogPublicArticleListItemVo::getArticleId);
        assertEquals(List.of(10L, 20L, 30L),
            ordered.stream().map(BlogPublicArticleListItemVo::getArticleId).toList());
    }

    @Test
    @DisplayName("命中序重排：命中序中缺席（回表三条件过滤）不出占位；空输入安全")
    void reorderDropsAbsent() {
        List<BlogPublicArticleListItemVo> rows = List.of(row(10L), row(20L));
        List<BlogPublicArticleListItemVo> ordered = EsSearchSupport.reorderByHitOrder(
            rows, List.of(20L, 10L, 99L), BlogPublicArticleListItemVo::getArticleId);
        assertEquals(List.of(20L, 10L),
            ordered.stream().map(BlogPublicArticleListItemVo::getArticleId).toList());
        assertTrue(EsSearchSupport.reorderByHitOrder(List.of(), List.of(1L),
            BlogPublicArticleListItemVo::getArticleId).isEmpty());
        assertTrue(EsSearchSupport.reorderByHitOrder(rows, List.of(),
            BlogPublicArticleListItemVo::getArticleId).isEmpty());
    }

    @Test
    @DisplayName("命中序重排：回表出现命中序外的 id（不应发生）被防御性丢弃，不污染打分序")
    void reorderDropsOutOfHitOrder() {
        List<BlogPublicArticleListItemVo> rows = List.of(row(10L), row(20L), row(77L));
        List<BlogPublicArticleListItemVo> ordered = EsSearchSupport.reorderByHitOrder(
            rows, List.of(20L, 10L), BlogPublicArticleListItemVo::getArticleId);
        assertEquals(List.of(20L, 10L),
            ordered.stream().map(BlogPublicArticleListItemVo::getArticleId).toList());
    }

    @Test
    @DisplayName("高亮合并：命中字段进 VO，未命中字段缺省；无片段不动 VO")
    void applyHighlight() {
        BlogPublicArticleListItemVo vo = row(1L);
        EsSearchSupport.applyHighlight(vo, Map.of(
            "title", List.of("Python <em>爬虫</em> 入门"),
            "content", List.of("……用 <em>Python</em> 写抓取……")));
        ArticleHighlightVo hl = vo.getHighlight();
        assertNotNull(hl);
        assertEquals(List.of("Python <em>爬虫</em> 入门"), hl.getTitle());
        assertNull(hl.getSummary());
        assertEquals(List.of("……用 <em>Python</em> 写抓取……"), hl.getContent());

        BlogPublicArticleListItemVo noHl = row(2L);
        EsSearchSupport.applyHighlight(noHl, null);
        EsSearchSupport.applyHighlight(noHl, Map.of());
        assertNull(noHl.getHighlight());
    }

    @Test
    @DisplayName("高亮合并：空片段列表按未命中处理")
    void applyHighlightEmptyFragments() {
        BlogPublicArticleListItemVo vo = row(1L);
        EsSearchSupport.applyHighlight(vo, Map.of("title", new ArrayList<>()));
        assertNull(vo.getHighlight().getTitle());
    }

}
