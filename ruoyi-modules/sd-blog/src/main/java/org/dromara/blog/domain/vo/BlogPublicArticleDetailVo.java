package org.dromara.blog.domain.vo;

import lombok.Data;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 公共文章详情视图对象（契约 §5.2，数据最小化）
 * <p>
 * 与作者视角的 {@link BlogArticleDetailVo} 分离：不暴露 status/visibility/slug
 * 与管理员治理字段，公共接口不得泄露内部状态（AGENTS.md §9 红线）。
 *
 * @author Xu
 * @date 2026-10-01
 */
@Data
public class BlogPublicArticleDetailVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文章ID
     */
    private Long articleId;

    /**
     * 作者用户ID
     */
    private Long authorId;

    /**
     * 作者昵称（实时翻译）
     */
    @Translation(type = TransConstant.USER_ID_TO_NAME, mapper = "authorId")
    private String authorName;

    /**
     * 首次发布时间
     */
    private LocalDateTime publishedAt;

    /**
     * 阅读量（本次打开已 +1 后的值）
     */
    private Long viewCount;

    /**
     * 当前生效版本（含正文）
     */
    private SdBlogArticleVersionVo currentVersion;

    /**
     * 标签列表
     */
    private List<SdBlogTagVo> tags;

    /**
     * 上一篇（发布时间更早的相邻公共文章），没有则为 null（S28）
     */
    private BlogAdjacentArticleVo prev;

    /**
     * 下一篇（发布时间更晚的相邻公共文章），没有则为 null（S28）
     */
    private BlogAdjacentArticleVo next;

}
