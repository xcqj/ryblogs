package org.dromara.blog.domain.bo;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 博客个人资料业务对象（契约 §8.2）
 * <p>
 * 仅接收 bio；userId 一律取当前登录上下文，禁止前端指定。
 *
 * @author Xu
 * @date 2026-09-28
 */
@Data
public class BlogProfileBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 个人简介
     */
    @Size(max = 500, message = "个人简介长度不能超过500个字符")
    private String bio;

}
