package org.dromara.blog.service.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 标签合并链解析纯逻辑单元测试（D7 策略④，契约 §13.5）
 *
 * @author Xu
 */
@DisplayName("TagMergeResolver 合并链展平")
class TagMergeResolverTest {

    @Test
    @DisplayName("无合并记录的标签即 canonical")
    void shouldReturnSelfWhenActive() {
        assertEquals(1L, TagMergeResolver.resolveCanonical(1L, Map.of()));
        assertNull(TagMergeResolver.resolveCanonical(null, Map.of(1L, 2L)));
    }

    @Test
    @DisplayName("多级链 A→B→C 跟随到最终 C")
    void shouldFollowChainToFinalCanonical() {
        Map<Long, Long> merged = Map.of(1L, 2L, 2L, 3L);
        assertEquals(3L, TagMergeResolver.resolveCanonical(1L, merged));
        assertEquals(3L, TagMergeResolver.resolveCanonical(2L, merged));
        assertEquals(3L, TagMergeResolver.resolveCanonical(3L, merged));
    }

    @Test
    @DisplayName("循环链快速失败（脏数据防御）")
    void shouldRejectCyclicChain() {
        Map<Long, Long> cyclic = Map.of(1L, 2L, 2L, 1L);
        assertThrows(IllegalStateException.class, () -> TagMergeResolver.resolveCanonical(1L, cyclic));
    }

    @Test
    @DisplayName("合并去重：冲突集合与迁移集合互补")
    void shouldSplitConflictAndMigrate() {
        Set<Long> source = Set.of(10L, 11L, 12L);
        Set<Long> target = Set.of(12L, 13L);
        assertEquals(Set.of(12L), TagMergeResolver.conflictArticleIds(source, target));
        assertEquals(Set.of(10L, 11L), TagMergeResolver.migrateArticleIds(source, target));
        assertTrue(TagMergeResolver.migrateArticleIds(source, target)
            .stream().noneMatch(TagMergeResolver.conflictArticleIds(source, target)::contains));
    }

}
