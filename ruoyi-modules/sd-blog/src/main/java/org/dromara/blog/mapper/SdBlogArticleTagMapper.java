package org.dromara.blog.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.blog.domain.SdBlogArticleTag;
import org.dromara.blog.domain.vo.SdBlogArticleTagVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 博客文章标签关系Mapper接口
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
public interface SdBlogArticleTagMapper extends BaseMapperPlus<SdBlogArticleTag, SdBlogArticleTagVo> {

    /**
     * 统计标签下公共可见文章数（契约 §7.3 标签详情展示用，公共三条件显式过滤）
     *
     * @param tagId 标签ID（canonical）
     * @return 公共可见文章数
     */
    @Select("""
        select count(*) from sd_blog_article_tag at
        join sd_blog_article a on a.article_id = at.article_id
            and a.status = '1' and a.visibility = '0' and a.del_flag = '0'
        where at.tag_id = #{tagId}
        """)
    long countPublicArticlesByTag(@Param("tagId") Long tagId);
}
