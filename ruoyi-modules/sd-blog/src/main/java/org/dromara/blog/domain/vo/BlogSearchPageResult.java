package org.dromara.blog.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.domain.PageResult;

import java.io.Serial;
import java.util.Collection;

/**
 * 搜索分页结果（契约 §6，S16）：在框架 {@link PageResult} 基础上增加深翻页游标。
 * <p>
 * 不改公共 {@code PageResult}（全仓共用），用子类承载博客特有的 {@code nextAfter}：
 * JSON 形态 = {@code {rows, total, nextAfter?}}，旧客户端忽略新字段，向后兼容。
 *
 * @author Xu
 * @date 2026-10-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BlogSearchPageResult<T> extends PageResult<T> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 深翻页游标（契约 §6）：客户端下一页原样作为 {@code after} 参数回传；
     * mysql 引擎/无更多结果时为 null（{@code NON_NULL} 不出现在 JSON 中）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String nextAfter;

    public BlogSearchPageResult() {
        super();
    }

    public BlogSearchPageResult(Collection<T> rows, long total, String nextAfter) {
        super(rows, total);
        this.nextAfter = nextAfter;
    }

}
