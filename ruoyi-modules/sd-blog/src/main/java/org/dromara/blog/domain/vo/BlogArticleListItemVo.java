package org.dromara.blog.domain.vo;

import lombok.Data;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 我的文章列表项视图对象（契约 §3.2）
 * <p>
 * title/summary/coverUrl 来自 MPJ 联查的当前生效版本（database-design §6.1：主表无内容字段）。
 *
 * @author Xu
 * @date 2026-09-28
 */
@Data
public class BlogArticleListItemVo implements Serializable {

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
     * 文章状态（0草稿 1已发布）
     */
    private String status;

    /**
     * 可见性（0公开 1用户隐藏 2管理员下线）
     */
    private String visibility;

    /**
     * 当前版本标题
     */
    private String title;

    /**
     * 当前版本摘要
     */
    private String summary;

    /**
     * 当前版本封面
     */
    private String coverUrl;

    /**
     * 阅读量
     */
    private Long viewCount;

    /**
     * 首次发布时间
     */
    private LocalDateTime publishedAt;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 标签列表
     */
    private List<SdBlogTagVo> tags;

}
