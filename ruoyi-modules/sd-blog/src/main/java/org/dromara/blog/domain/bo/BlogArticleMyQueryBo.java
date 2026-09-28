package org.dromara.blog.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 我的文章列表查询业务对象（契约 §3.2）
 * <p>
 * authorId 不接受前端传入，由服务端按登录用户强制过滤。
 *
 * @author Xu
 * @date 2026-09-28
 */
@Data
public class BlogArticleMyQueryBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文章状态（0草稿 1已发布），空则不过滤
     */
    private String status;

    /**
     * 可见性（0公开 1用户隐藏 2管理员下线），空则不过滤
     */
    private String visibility;

    /**
     * 标题关键字（模糊匹配当前生效版本标题）
     */
    private String keyword;

}
