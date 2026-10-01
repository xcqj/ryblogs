package org.dromara.blog.service;


import java.util.List;

/**
 * 博客文章标签关系Service接口
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
public interface ISdBlogArticleTagService {

    /**
     * 同步文章标签关系：目标集合与现状做差集，缺的补、多的物理删，已有行保持不动
     * （契约 §3.4"保存时同步标签"，database-design §10；纯关系表物理删除，S2 决议）。
     *
     * @param articleId 文章ID
     * @param tagIds    目标标签ID集合（调用方保证已规范化且 1~6 个）
     */
    void syncArticleTags(Long articleId, List<Long> tagIds);
}
