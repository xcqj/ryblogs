package org.dromara.blog.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.dromara.blog.domain.SdBlogTag;
import org.dromara.blog.domain.vo.BlogPublicTagVo;
import org.dromara.blog.domain.vo.SdBlogTagVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;

/**
 * 博客标签Mapper接口
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
public interface SdBlogTagMapper extends BaseMapperPlus<SdBlogTag, SdBlogTagVo> {

    /**
     * 按 slug 查询标签（含已逻辑删除记录）。
     * 软删标签仍占用 uq_blog_tag_slug 物理唯一键，"重输同 slug 恢复原标签"（S6 决议）
     * 需要先看到被逻辑过滤的行，故绕过 @TableLogic 用注解 SQL；仅限 Service 治理链路使用。
     *
     * @param slug 标签slug
     * @return 标签实体（可能为已删除记录）；不存在返回 null
     */
    @Select("select * from sd_blog_tag where slug = #{slug} limit 1")
    SdBlogTag selectAnyBySlug(@Param("slug") String slug);

    /**
     * 恢复已逻辑删除的标签（del_flag 置 0，status/merged_into 保持原值——MERGED 标签不允许删除，
     * 被删标签必为 ACTIVE，见 adminDelete 前置校验）
     *
     * @param tagId 标签ID
     * @return 更新行数（0 表示已被并发恢复）
     */
    @Update("update sd_blog_tag set del_flag = '0', update_time = now() where tag_id = #{tagId} and del_flag = '1'")
    int restoreByTagId(@Param("tagId") Long tagId);

    /**
     * 热门标签（契约 §7.2）：按公共可见文章（status=1 且 visibility=0 且未删）的关联数倒序。
     * 公共条件写在 join on 上；计数必须用 count(a.article_id)——left join 未命中公共文章时
     * a.* 为 NULL 而 at.* 仍非空，count(at.article_id) 会把草稿/隐藏计入（冒烟实测 6 vs 2 差异根因）。
     * 不保存 is_hot 字段，实时计算。
     *
     * @param limit 返回数量上限
     * @return 热门标签列表（含 articleCount）
     */
    @Select("""
        select t.tag_id, t.name, t.slug, count(a.article_id) as article_count
        from sd_blog_tag t
        left join sd_blog_article_tag at on at.tag_id = t.tag_id
        left join sd_blog_article a on a.article_id = at.article_id
            and a.status = '1' and a.visibility = '0' and a.del_flag = '0'
        where t.status = '0' and t.del_flag = '0'
        group by t.tag_id, t.name, t.slug
        order by article_count desc, t.tag_id asc
        limit #{limit}
        """)
    List<BlogPublicTagVo> selectHotTags(@Param("limit") int limit);

}
