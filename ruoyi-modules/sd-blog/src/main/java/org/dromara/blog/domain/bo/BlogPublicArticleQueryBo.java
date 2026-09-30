package org.dromara.blog.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 公共文章列表查询业务对象（契约 §5.1）
 * <p>
 * 公共可见三条件不在本 BO 暴露、由查询底座强制内建（AGENTS.md §9：公共 API 不得绕过）；
 * tagSlug 无效（无法解析 canonical）返回空页而非报错（列表类过滤语义）。
 *
 * @author Xu
 * @date 2026-10-01
 */
@Data
public class BlogPublicArticleQueryBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 标签 slug（含别名，解析到 canonical 后过滤），空为全部
     */
    private String tagSlug;

    /**
     * 作者用户ID，空为全部
     */
    private Long authorId;

}
