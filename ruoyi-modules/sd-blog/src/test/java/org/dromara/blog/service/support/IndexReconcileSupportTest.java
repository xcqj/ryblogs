package org.dromara.blog.service.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IndexReconcileSupport 纯逻辑单元测试（不碰数据库 / ES / Spring 容器，符合 D7 单测策略）。
 * <p>
 * 覆盖对账集合 diff：补索引集合、删除集合（含非本应用写入的脏 id）、完全一致、空集合边界。
 *
 * @author Xu
 * @date 2026-10-14
 */
@DisplayName("索引对账纯逻辑单元测试")
class IndexReconcileSupportTest {

    @Test
    @DisplayName("diff：MySQL 有 ES 无 → 补索引；ES 有 MySQL 无 → 删文档")
    void diffBothDirections() {
        IndexReconcileSupport.Diff diff = IndexReconcileSupport.diff(
            Set.of(10L, 20L, 30L), Set.of("20", "40", "50"));
        assertEquals(List.of(10L, 30L), diff.toIndex());
        assertEquals(List.of("40", "50"), diff.toRemove());
        assertFalse(diff.isEmpty());
    }

    @Test
    @DisplayName("diff：两边一致 → 空结果")
    void diffIdentical() {
        IndexReconcileSupport.Diff diff = IndexReconcileSupport.diff(
            Set.of(1L, 2L, 3L), Set.of("1", "2", "3"));
        assertTrue(diff.isEmpty());
        assertTrue(diff.toIndex().isEmpty());
        assertTrue(diff.toRemove().isEmpty());
    }

    @Test
    @DisplayName("diff：ES 侧脏 id（非数字/带空白）归入删除集合，不参与补索引判断")
    void diffGarbageIds() {
        IndexReconcileSupport.Diff diff = IndexReconcileSupport.diff(
            Set.of(1L), Set.of("1", "not-a-number", "  "));
        assertTrue(diff.toIndex().isEmpty());
        assertTrue(diff.toRemove().containsAll(List.of("not-a-number", "  ")));
    }

    @Test
    @DisplayName("diff：ES id 带空白但数字合法 → 正常匹配不误删")
    void diffTrimsEsIds() {
        IndexReconcileSupport.Diff diff = IndexReconcileSupport.diff(
            Set.of(100L), Set.of(" 100 "));
        assertTrue(diff.isEmpty());
    }

    @Test
    @DisplayName("diff：空输入边界（ES 未初始化 / MySQL 空库）")
    void diffEmptyInputs() {
        assertTrue(IndexReconcileSupport.diff(Set.of(), Set.of()).isEmpty());
        IndexReconcileSupport.Diff freshEs = IndexReconcileSupport.diff(Set.of(1L, 2L), Set.of());
        assertEquals(List.of(1L, 2L), freshEs.toIndex());
        assertTrue(freshEs.toRemove().isEmpty());
        IndexReconcileSupport.Diff emptyMysql = IndexReconcileSupport.diff(Set.of(), Set.of("7"));
        assertTrue(emptyMysql.toIndex().isEmpty());
        assertEquals(List.of("7"), emptyMysql.toRemove());
        assertTrue(IndexReconcileSupport.diff(null, null).isEmpty());
    }

}
