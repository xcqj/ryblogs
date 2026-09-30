package org.dromara.blog.service.support;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 标签合并链解析纯函数工具（契约 §13.5 / §18.10，D7 策略④）
 * <p>
 * 合并要求"不保留长期合并链"：A→B、B→C 最终必须展平为 A→C、B→C。
 * 本类只负责纯计算（链跟随、冲突集合），SQL 落库在 Service 层。
 * 链跟随带 visited 防御：正常业务前置校验（source/target 均 ACTIVE）下不可能成环，
 * 成环即数据被外力改坏，快速失败好过死循环。
 *
 * @author Xu
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TagMergeResolver {

    /**
     * 沿 merged_into 映射跟随到最终 canonical 标签
     *
     * @param tagId         起始标签ID
     * @param mergedIntoMap 标签ID → 合并目标ID 的映射（仅含 MERGED 节点）
     * @return 最终有效标签ID（映射中无后继的节点）；tagId 为 null 时返回 null
     * @throws IllegalStateException 检测到循环链（脏数据防御）
     */
    public static Long resolveCanonical(Long tagId, Map<Long, Long> mergedIntoMap) {
        if (Objects.isNull(tagId)) {
            return null;
        }
        Long current = tagId;
        Set<Long> visited = new HashSet<>();
        while (Objects.nonNull(mergedIntoMap.get(current))) {
            if (!visited.add(current)) {
                throw new IllegalStateException("标签合并链存在循环，标签ID：" + current);
            }
            current = mergedIntoMap.get(current);
        }
        return current;
    }

    /**
     * 计算合并迁移的两个集合：
     * 同时挂有 source 与 target 的文章，其 source 关系应删除（去重）；
     * 其余 source 关系迁移为 target。
     *
     * @param sourceArticleIds 挂了 source 标签的文章ID集合
     * @param targetArticleIds 挂了 target 标签的文章ID集合
     * @return 需要迁移（非冲突）的文章ID集合
     */
    public static Set<Long> migrateArticleIds(Set<Long> sourceArticleIds, Set<Long> targetArticleIds) {
        Set<Long> migrate = new LinkedHashSet<>(sourceArticleIds);
        migrate.removeAll(targetArticleIds);
        return migrate;
    }

    /**
     * 计算合并去重的冲突集合：两标签同时挂过的文章，source 关系直接删除
     *
     * @param sourceArticleIds 挂了 source 标签的文章ID集合
     * @param targetArticleIds 挂了 target 标签的文章ID集合
     * @return 冲突（重复）文章ID集合
     */
    public static Set<Long> conflictArticleIds(Set<Long> sourceArticleIds, Set<Long> targetArticleIds) {
        Set<Long> conflict = new LinkedHashSet<>(sourceArticleIds);
        conflict.retainAll(targetArticleIds);
        return conflict;
    }

}
