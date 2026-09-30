package org.dromara.blog.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 管理员下线文章业务对象（契约 §11.3）
 * <p>
 * reason 必填：治理动作需留痕，供作者与审计查看下线依据。
 *
 * @author Xu
 * @date 2026-09-30
 */
@Data
public class BlogAdminArticleHideBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 下线原因
     */
    @NotBlank(message = "下线原因不能为空")
    @Size(max = 500, message = "下线原因长度不能超过500个字符")
    private String reason;

}
