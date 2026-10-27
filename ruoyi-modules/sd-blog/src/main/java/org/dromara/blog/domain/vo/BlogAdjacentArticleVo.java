package org.dromara.blog.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 相邻文章视图对象（契约 §5.2，S28）
 * <p>
 * 只暴露导航所需的最小信息：文章ID + 标题。不返回摘要与封面——详情页底部的
 * 上一篇/下一篇是「文本性导航」，带摘要会把正文尾部撑成卡片墙。
 * <p>
 * 可见性：只在公共可见文章之间跳转（status=1 且 visibility=0），否则会链到草稿
 * 或已下线文章，读者点进去即 404。
 *
 * @author Xu
 * @date 2026-10-28
 */
@Data
public class BlogAdjacentArticleVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 相邻文章ID
     */
    private Long articleId;

    /**
     * 相邻文章标题（当前生效版本快照）
     */
    private String title;

}
