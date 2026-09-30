package org.dromara.blog.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 公共标签视图对象（契约 §7，数据最小化）
 * <p>
 * 公共接口只暴露 tagId/name/slug 与公开文章计数，
 * 不泄露 status/mergedInto/isFeatured/sortOrder 等治理与运营字段。
 *
 * @author Xu
 * @date 2026-09-30
 */
@Data
public class BlogPublicTagVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 标签ID
     */
    private Long tagId;

    /**
     * 标签名称
     */
    private String name;

    /**
     * 标签slug（公共 URL 标识）
     */
    private String slug;

    /**
     * 公共可见文章数（仅 hot/详情场景填充，列表联想可为 null）
     */
    private Long articleCount;

}
