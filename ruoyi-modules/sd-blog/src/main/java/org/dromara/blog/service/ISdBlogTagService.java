package org.dromara.blog.service;

import org.dromara.blog.domain.SdBlogTag;
import org.dromara.blog.domain.vo.BlogPublicTagVo;
import org.dromara.blog.domain.vo.SdBlogTagVo;
import org.dromara.blog.domain.bo.SdBlogTagBo;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.util.List;


/**
 * 博客标签Service接口
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
public interface ISdBlogTagService {

    /**
     * 查询博客标签
     *
     * @param tagId 主键
     * @return 博客标签
     */
    SdBlogTagVo queryById(Long tagId);

    /**
     * 按规范化规则将标签名列表解析为标签实体（契约 §3.1 / §18.10，PRD TAG-02/03）：
     * 同 slug 已存在则复用；不存在则自动入库（无需预审核）。
     * <p>
     * S6 已接入：① MERGED 标签沿 merged_into 跟随到 canonical；② 已软删标签同 slug 恢复原记录
     * 而非新建（物理唯一键占用）；③ 新建前 tag.slug 与 tag_alias.alias_slug 跨表冲突检查。
     *
     * @param rawNames 原始标签名列表
     * @return 规范化去重后的 canonical 标签实体列表（含新建/恢复的）
     */
    List<SdBlogTag> resolveOrCreateByNames(List<String> rawNames);

    /**
     * 精选标签列表（契约 §7.1）：is_featured=1 且 ACTIVE，按 sort_order 升序
     *
     * @return 公共标签视图列表
     */
    List<BlogPublicTagVo> queryFeaturedTags();

    /**
     * 热门标签（契约 §7.2）：按公共可见文章关联数实时统计，不保存 is_hot 字段
     *
     * @param limit 数量上限（Service 层封顶）
     * @return 热门标签列表（含 articleCount）
     */
    List<BlogPublicTagVo> queryHotTags(int limit);

    /**
     * 公共标签分页列表/候选联想（契约 §7.5）：只返回 ACTIVE 且未删，keyword 按名称/slug 模糊
     *
     * @param keyword   可选关键字
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    PageResult<BlogPublicTagVo> queryPublicPageList(String keyword, PageQuery pageQuery);

    /**
     * slug 解析（契约 §7.3/§7.4）：当前 canonical slug → tag_alias.alias_slug → canonical 标签；
     * MERGED 沿链跟随；命中 alias 后映射到 canonical，HTTP 301 由前台/边缘层承担
     *
     * @param slug 公共 URL 标识
     * @return canonical 标签；无法解析返回 null
     */
    SdBlogTag resolveCanonicalBySlug(String slug);

    /**
     * 公共标签详情（契约 §7.3）：slug/alias 解析到 canonical 并附公共可见文章数
     *
     * @param slug 公共 URL 标识
     * @return 标签视图（含 articleCount）
     * @throws org.dromara.common.core.exception.ServiceException 无法解析时“标签不存在”
     */
    BlogPublicTagVo queryPublicTagDetail(String slug);

    /**
     * 管理员标签分页列表（契约 §13.1）：可见 ACTIVE/MERGED 及治理信息，更新时间倒序
     *
     * @param bo        查询条件（name/status/isFeatured）
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    PageResult<SdBlogTagVo> queryAdminPageList(SdBlogTagBo bo, PageQuery pageQuery);

    /**
     * 更新标签运营属性（契约 §13.3）：仅 isFeatured/sortOrder，不碰 name/slug/status/mergedInto
     *
     * @param tagId      标签ID
     * @param isFeatured 是否精选（0否 1是）
     * @param sortOrder  展示顺序
     * @return 是否成功
     */
    Boolean updateOps(Long tagId, String isFeatured, Integer sortOrder);

    /**
     * 重命名标签（契约 §13.4）：旧 name/slug 保留为 alias，当前 tag_id 不变；
     * 新 slug 跨表冲突则拒绝
     *
     * @param tagId   标签ID
     * @param rawName 新标签名
     * @return 是否成功
     */
    Boolean renameTag(Long tagId, String rawName);

    /**
     * 合并标签（契约 §13.5）：同一事务六步——迁移关系、去重（物理删）、处理 alias、
     * source 置 MERGED+merged_into=目标、历史链/alias 展平到最终 canonical
     *
     * @param sourceTagId 源标签ID
     * @param targetTagId 目标标签ID（必须 ACTIVE）
     * @return 是否成功
     */
    Boolean mergeTag(Long sourceTagId, Long targetTagId);

    /**
     * 删除垃圾标签（契约 §13.6）：解除 ArticleTag 关系（物理删）+ 标签逻辑删除，不建 alias；
     * MERGED 历史节点不可删（破坏 alias/合并链）
     *
     * @param tagId 标签ID
     * @return 是否成功
     */
    Boolean adminDelete(Long tagId);
}
