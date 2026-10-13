package org.dromara.blog.service.support;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 索引对账集合 diff 纯逻辑（ROADMAP S16，零框架依赖，可单测）。
 * <p>
 * 输入：MySQL 公共可见文章 id 集合 vs ES 文档 id 集合（文档 _id 即 articleId 字符串）。
 * 输出：需要补索引的文章 id（MySQL 有、ES 无）与需要删除的文档 id（ES 有、MySQL 无）。
 * <p>
 * ES 侧 id 无法解析为 Long 的（非本应用写入的脏数据）一律归入删除集合——它们不可能
 * 对应任何 MySQL 文章，留着只会污染搜索结果。
 *
 * @author Xu
 * @date 2026-10-14
 */
public final class IndexReconcileSupport {

    private IndexReconcileSupport() {
    }

    /**
     * 对账结果
     *
     * @param toIndex  需要补索引的文章 id（升序）
     * @param toRemove 需要删除的 ES 文档 id 字符串（含无法解析的脏 id）
     */
    public record Diff(List<Long> toIndex, List<String> toRemove) {

        public boolean isEmpty() {
            return toIndex.isEmpty() && toRemove.isEmpty();
        }

    }

    /**
     * 集合 diff
     *
     * @param mysqlArticleIds MySQL 公共可见文章 id 集合
     * @param esDocIds        ES 文档 _id 集合（字符串）
     */
    public static Diff diff(Collection<Long> mysqlArticleIds, Collection<String> esDocIds) {
        Set<Long> mysqlSet = mysqlArticleIds == null ? Set.of() : new HashSet<>(mysqlArticleIds);
        Set<Long> esSet = new HashSet<>();
        List<String> garbage = new java.util.ArrayList<>();
        if (esDocIds != null) {
            for (String id : esDocIds) {
                try {
                    esSet.add(Long.valueOf(id.trim()));
                } catch (NumberFormatException e) {
                    garbage.add(id);
                }
            }
        }
        List<Long> toIndex = mysqlSet.stream()
            .filter(id -> !esSet.contains(id))
            .sorted()
            .toList();
        List<String> toRemove = esDocIds == null ? List.of() : esDocIds.stream()
            .filter(id -> {
                try {
                    return !mysqlSet.contains(Long.valueOf(id.trim()));
                } catch (NumberFormatException e) {
                    return true;
                }
            })
            .sorted()
            .toList();
        toRemove = new java.util.ArrayList<>(toRemove);
        toRemove.addAll(garbage);
        return new Diff(toIndex, toRemove);
    }

}
