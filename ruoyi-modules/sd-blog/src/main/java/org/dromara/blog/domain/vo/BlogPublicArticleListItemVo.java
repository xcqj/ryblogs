package org.dromara.blog.domain.vo;

import lombok.Data;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 公共文章列表项视图对象（契约 §5.1 / §7.4，数据最小化）
 * <p>
 * 只暴露公共可见文章的展示字段，不含 status/visibility/slug 等内部状态；
 * title/summary/coverUrl 取自当前生效版本（联表，契约 §18.11）。
 *
 * @author Xu
 * @date 2026-09-30
 */
@Data
public class BlogPublicArticleListItemVo implements Serializable {

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
     * 标题（当前版本）
     */
    private String title;

    /**
     * 摘要（当前版本）
     */
    private String summary;

    /**
     * 封面地址（当前版本）
     */
    private String coverUrl;

    /**
     * 首次发布时间
     */
    private LocalDateTime publishedAt;

    /**
     * 阅读量
     */
    private Long viewCount;

    /**
     * 标签列表
     */
    private List<SdBlogTagVo> tags;

}
