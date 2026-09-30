package org.dromara.blog.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 标签重命名业务对象（契约 §13.4）
 * <p>
 * 服务端规范化后生成新 slug，旧 name/slug 自动保留为 alias，tag_id 不变。
 *
 * @author Xu
 * @date 2026-09-30
 */
@Data
public class BlogTagRenameBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 新标签名称
     */
    @NotBlank(message = "标签名称不能为空")
    @Size(max = 50, message = "标签名称长度不能超过50个字符")
    private String name;

}
