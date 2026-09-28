package org.dromara.blog.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.base.MPJBaseMapper;
import org.dromara.blog.domain.SdBlogArticle;
import org.dromara.blog.domain.SdBlogArticleVersion;
import org.dromara.blog.domain.bo.BlogArticleMyQueryBo;
import org.dromara.blog.domain.vo.BlogArticleListItemVo;
import org.dromara.blog.domain.vo.SdBlogArticleVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.common.mybatis.core.query.QueryBuilder;

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

}
