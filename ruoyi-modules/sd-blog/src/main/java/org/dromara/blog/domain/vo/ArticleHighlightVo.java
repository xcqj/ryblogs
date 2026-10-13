package org.dromara.blog.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 文章搜索高亮（契约 §6 / D36 服务端高亮，2026-10-14 S16）。
 * <p>
 * 每个字段为匹配片段数组（0~N 条，命中词以 {@code <em>} 包裹）；片段内其余 HTML
 * 已由 ES 默认编码器转义，前端可信任直接渲染。字段缺失 = 该字段未命中
 * （{@link JsonInclude.Include#NON_NULL} 保证不出现在 JSON 中）。
 *
 * @author Xu
 * @date 2026-10-14
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArticleHighlightVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 标题命中片段（number_of_fragments=0，整字段高亮）
     */
    private List<String> title;

    /**
     * 摘要命中片段（整字段高亮）
     */
    private List<String> summary;

    /**
     * 正文命中片段（截断片段，最多 3 条）
     */
    private List<String> content;

}
