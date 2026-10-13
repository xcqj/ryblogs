package org.dromara.blog.service.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ViewCountSupport 纯逻辑单元测试（不碰数据库 / Redis / Spring 容器，符合 D7 单测策略）。
 * <p>
 * 覆盖键生成、TTL 换算、flush 扁平返回配对、批量参数排序、读值合并、访客键优先级六类边界。
 *
 * @author Xu
 * @date 2026-10-14
 */
@DisplayName("阅读量异步计数纯逻辑单元测试")
class ViewCountSupportTest {

    @Test
    @DisplayName("键形态：去重键含文章与访客，待增量键为固定 hash")
    void keyFormats() {
        assertEquals("blog:view:dedup:12345:u1", ViewCountSupport.dedupKey(12345L, "u1"));
        assertEquals("blog:view:dedup:12345:vabc-uuid", ViewCountSupport.dedupKey(12345L, "vabc-uuid"));
        assertEquals("blog:view:pending", ViewCountSupport.pendingKey());
    }

    @Test
    @DisplayName("TTL 换算：分钟转秒，非正值回退默认 30min")
    void ttlSecondsConversion() {
        assertEquals(1800L, ViewCountSupport.ttlSeconds(30));
        assertEquals(2700L, ViewCountSupport.ttlSeconds(45));
        assertEquals(60L, ViewCountSupport.ttlSeconds(1));
        // 配置错误不得把去重退化成永久键或立即过期
        assertEquals(1800L, ViewCountSupport.ttlSeconds(0));
        assertEquals(1800L, ViewCountSupport.ttlSeconds(-5));
    }

    @Test
    @DisplayName("扁平配对：正常项解析，奇数长度与非数字项逐条跳过")
    void parsePendingEntriesSkipsMalformed() {
        List<Object> flat = new ArrayList<>(List.of("101", "3", 202L, "2", "bad", "9", "303", "x", "404", "0"));
        Map<Long, Long> deltas = ViewCountSupport.parsePendingEntries(flat);
        // 101→3、202→2 正常；'bad'→9 被跳过；'303'→'x' 非数字跳过；'404'→0 非正跳过
        assertEquals(2, deltas.size());
        assertEquals(3L, deltas.get(101L));
        assertEquals(2L, deltas.get(202L));
        // 保留插入序（LinkedHashMap）
        assertEquals(List.of(101L, 202L), new ArrayList<>(deltas.keySet()));
    }

    @Test
    @DisplayName("扁平配对：空表与 null 安全，Number 各形态可解析")
    void parsePendingEntriesEmptyAndNumberTypes() {
        assertTrue(ViewCountSupport.parsePendingEntries(null).isEmpty());
        assertTrue(ViewCountSupport.parsePendingEntries(List.of()).isEmpty());
        Map<Long, Long> deltas = ViewCountSupport.parsePendingEntries(List.of(Integer.valueOf(7), Long.valueOf(12)));
        assertEquals(12L, deltas.get(7L));
    }

    @Test
    @DisplayName("批量参数：按 articleId 升序保证确定性，空表返回空")
    void toBatchParamsSorted() {
        Map<Long, Long> deltas = new LinkedHashMap<>();
        deltas.put(303L, 1L);
        deltas.put(101L, 5L);
        deltas.put(202L, 2L);
        List<long[]> params = ViewCountSupport.toBatchParams(deltas);
        assertEquals(3, params.size());
        assertArrayEquals(new long[]{101L, 5L}, params.get(0));
        assertArrayEquals(new long[]{202L, 2L}, params.get(1));
        assertArrayEquals(new long[]{303L, 1L}, params.get(2));
        assertTrue(ViewCountSupport.toBatchParams(null).isEmpty());
        assertTrue(ViewCountSupport.toBatchParams(Map.of()).isEmpty());
    }

    @Test
    @DisplayName("读值合并：DB 与待增量 null 安全合成")
    void mergeViewCountNullSafe() {
        assertEquals(10L, ViewCountSupport.mergeViewCount(10L, null));
        assertEquals(3L, ViewCountSupport.mergeViewCount(null, 3L));
        assertEquals(0L, ViewCountSupport.mergeViewCount(null, null));
        assertEquals(13L, ViewCountSupport.mergeViewCount(10L, 3L));
        assertEquals(0L, ViewCountSupport.mergeViewCount(0L, 0L));
    }

    @Test
    @DisplayName("访客键：登录 userId 优先，匿名 visitorId 次之，都无返回 null")
    void visitorKeyPriority() {
        assertEquals("u1761100000000000001", ViewCountSupport.visitorKeyOf(1761100000000000001L, "some-visitor"));
        assertEquals("vabc-123", ViewCountSupport.visitorKeyOf(null, "abc-123"));
        // 前后空白修剪
        assertEquals("vabc-123", ViewCountSupport.visitorKeyOf(null, "  abc-123  "));
        assertNull(ViewCountSupport.visitorKeyOf(null, null));
        assertNull(ViewCountSupport.visitorKeyOf(null, ""));
        assertNull(ViewCountSupport.visitorKeyOf(null, "   "));
    }

}
