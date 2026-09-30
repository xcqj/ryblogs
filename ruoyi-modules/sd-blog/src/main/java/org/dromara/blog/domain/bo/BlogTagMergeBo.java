package org.dromara.blog.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 标签合并业务对象（契约 §13.5）
 * <p>
 * source 为路径参数；target 必须为当前 ACTIVE 标签，合并事务六步在 Service 同事务完成。
 *
 * @author Xu
 * @date 2026-09-30
 */
@Data
public class BlogTagMergeBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 目标标签ID（合并去向，必须 ACTIVE）
     */
    @NotNull(message = "目标标签ID不能为空")
    private Long targetTagId;

}
