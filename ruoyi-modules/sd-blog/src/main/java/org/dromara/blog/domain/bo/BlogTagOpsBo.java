package org.dromara.blog.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 标签运营属性更新业务对象（契约 §13.3）
 * <p>
 * 仅 isFeatured/sortOrder 两字段；name/slug/status/mergedInto 禁止普通更新（§13.3），
 * 必须走 rename/merge 专用动作接口。
 *
 * @author Xu
 * @date 2026-09-30
 */
@Data
public class BlogTagOpsBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否精选（0否 1是）
     */
    @NotBlank(message = "精选标记不能为空")
    @Pattern(regexp = "[01]", message = "精选标记只能为0或1")
    private String isFeatured;

    /**
     * 精选展示顺序（越小越靠前）
     */
    @NotNull(message = "展示顺序不能为空")
    private Integer sortOrder;

}
