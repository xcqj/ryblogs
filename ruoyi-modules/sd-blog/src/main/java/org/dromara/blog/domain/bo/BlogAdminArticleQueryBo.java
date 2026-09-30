package org.dromara.blog.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 管理员文章列表查询业务对象（契约 §11.1）
 * <p>
 * 管理员可见全部状态组合；title 模糊按当前版本匹配；
 * beginTime/endTime 为创建时间范围（格式 yyyy-MM-dd HH:mm:ss，可单边）；
 * 不用 RuoYi 生成器风格的 Map params：GET 查询下空串无法绑定 Map，会直接 500。
 *
 * @author Xu
 * @date 2026-09-30
 */
@Data
public class BlogAdminArticleQueryBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 标题关键字（联查当前版本 title 模糊匹配）
     */
    private String title;

    /**
     * 文章状态（0草稿 1已发布），空为全部
     */
    private String status;

    /**
     * 可见性（0公开 1用户隐藏 2管理员下线），空为全部
     */
    private String visibility;

    /**
     * 作者用户ID，空为全部
     */
    private Long authorId;

    /**
     * 创建时间起（yyyy-MM-dd HH:mm:ss，含）
     */
    private String beginTime;

    /**
     * 创建时间止（yyyy-MM-dd HH:mm:ss，含）
     */
    private String endTime;

}
