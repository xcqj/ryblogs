package org.dromara.blog.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.base.MPJBaseMapper;
import org.dromara.blog.domain.SdBlogArticle;
import org.dromara.blog.domain.SdBlogArticleVersion;
import org.dromara.blog.domain.bo.BlogArticleMyQueryBo;
import org.dromara.blog.domain.vo.BlogArticleListItemVo;
import org.dromara.blog.domain.vo.BlogPublicArticleListItemVo;
import org.dromara.blog.domain.vo.SdBlogArticleVo;
import org.dromara.blog.enums.BlogArticleStatus;
import org.dromara.blog.enums.BlogVisibility;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.common.mybatis.core.query.QueryBuilder;

import java.util.Collection;

/**
 * 博客文章Mapper接口
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
public interface SdBlogArticleMapper extends BaseMapperPlus<SdBlogArticle, SdBlogArticleVo>, MPJBaseMapper<SdBlogArticle> {

    /**
     * 分页查询我的文章（契约 §3.2）：联当前生效版本取标题/摘要/封面，支持标题关键字模糊。
     *
     * @param page   分页对象
     * @param bo     查询条件（status/visibility/keyword）
     * @param userId 当前登录用户ID（强制过滤，防越权）
     * @return 我的文章分页结果
     */
    default Page<BlogArticleListItemVo> selectMyPage(Page<BlogArticleListItemVo> page, BlogArticleMyQueryBo bo, Long userId) {
        return QueryBuilder.lambdaJoin("a", SdBlogArticle.class)
            .select("a", SdBlogArticle::getArticleId, SdBlogArticle::getAuthorId, SdBlogArticle::getStatus,
                SdBlogArticle::getVisibility, SdBlogArticle::getViewCount, SdBlogArticle::getPublishedAt,
                SdBlogArticle::getUpdateTime)
            .select("v", SdBlogArticleVersion::getTitle, SdBlogArticleVersion::getSummary, SdBlogArticleVersion::getCoverUrl)
            .leftJoin(SdBlogArticleVersion.class, "v", SdBlogArticleVersion::getVersionId, SdBlogArticle::getCurrentVersionId)
            .eq("a", SdBlogArticle::getAuthorId, userId)
            .eqIfText("a", SdBlogArticle::getStatus, bo.getStatus())
            .eqIfText("a", SdBlogArticle::getVisibility, bo.getVisibility())
            .likeIfText("v", SdBlogArticleVersion::getTitle, bo.getKeyword())
            .orderByDesc("a", SdBlogArticle::getUpdateTime)
            .orderByDesc("a", SdBlogArticle::getArticleId)
            .page(page, BlogArticleListItemVo.class);
    }

    /**
     * 公共文章分页（契约 §5.1 / §7.4 共用底座，S6 落地）：公共可见三条件
     * （status=1 且 visibility=0 且未删，del_flag 由 @TableLogic 兜底）联当前生效版本；
     * 排序 published_at 倒序（公共信息流惯例，ROADMAP S6 自决项）。
     *
     * @param page                分页对象
     * @param restrictArticleIds  限定文章ID集合（标签下文章场景）；null 或空集合表示不限定
     * @return 公共文章分页结果
     */
    default Page<BlogPublicArticleListItemVo> selectPublicPage(Page<BlogPublicArticleListItemVo> page,
                                                               Collection<Long> restrictArticleIds) {
        boolean restrict = restrictArticleIds != null && !restrictArticleIds.isEmpty();
        return QueryBuilder.lambdaJoin("a", SdBlogArticle.class)
            .select("a", SdBlogArticle::getArticleId, SdBlogArticle::getAuthorId,
                SdBlogArticle::getPublishedAt, SdBlogArticle::getViewCount)
            .select("v", SdBlogArticleVersion::getTitle, SdBlogArticleVersion::getSummary, SdBlogArticleVersion::getCoverUrl)
            .leftJoin(SdBlogArticleVersion.class, "v", SdBlogArticleVersion::getVersionId, SdBlogArticle::getCurrentVersionId)
            .eq("a", SdBlogArticle::getStatus, BlogArticleStatus.PUBLISHED.getCode())
            .eq("a", SdBlogArticle::getVisibility, BlogVisibility.PUBLIC.getCode())
            .in(restrict, "a", SdBlogArticle::getArticleId, restrictArticleIds)
            .orderByDesc("a", SdBlogArticle::getPublishedAt)
            .orderByDesc("a", SdBlogArticle::getArticleId)
            .page(page, BlogPublicArticleListItemVo.class);
    }

}
