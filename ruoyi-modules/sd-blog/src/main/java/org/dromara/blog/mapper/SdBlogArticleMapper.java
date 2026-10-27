package org.dromara.blog.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.dromara.blog.domain.SdBlogArticle;
import org.dromara.blog.domain.SdBlogArticleVersion;
import org.dromara.blog.domain.bo.BlogArticleMyQueryBo;
import org.dromara.blog.domain.vo.BlogAdjacentArticleVo;
import org.dromara.blog.domain.vo.BlogArticleListItemVo;
import org.dromara.blog.domain.vo.BlogPublicArticleListItemVo;
import org.dromara.blog.domain.vo.SdBlogArticleVo;
import org.dromara.blog.enums.BlogArticleStatus;
import org.dromara.blog.enums.BlogVisibility;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.common.mybatis.core.query.QueryBuilder;

import java.time.LocalDateTime;
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
     * 公共文章分页（契约 §5.1 / §7.4 共用底座，S6 落地，S8 补 §5.1 的 authorId 过滤）：
     * 公共可见三条件（status=1 且 visibility=0 且未删，del_flag 由 @TableLogic 兜底）联当前生效版本；
     * 排序 published_at 倒序（公共信息流惯例，ROADMAP S6 自决项）。
     *
     * @param page                分页对象
     * @param restrictArticleIds  限定文章ID集合（标签下文章场景）；null 或空集合表示不限定
     * @param authorId            作者过滤（契约 §5.1/§9.2）；null 不限定
     * @return 公共文章分页结果
     */
    default Page<BlogPublicArticleListItemVo> selectPublicPage(Page<BlogPublicArticleListItemVo> page,
                                                               Collection<Long> restrictArticleIds,
                                                               Long authorId) {
        boolean restrict = restrictArticleIds != null && !restrictArticleIds.isEmpty();
        return QueryBuilder.lambdaJoin("a", SdBlogArticle.class)
            .select("a", SdBlogArticle::getArticleId, SdBlogArticle::getAuthorId,
                SdBlogArticle::getPublishedAt, SdBlogArticle::getViewCount)
            .select("v", SdBlogArticleVersion::getTitle, SdBlogArticleVersion::getSummary, SdBlogArticleVersion::getCoverUrl)
            .leftJoin(SdBlogArticleVersion.class, "v", SdBlogArticleVersion::getVersionId, SdBlogArticle::getCurrentVersionId)
            .eq("a", SdBlogArticle::getStatus, BlogArticleStatus.PUBLISHED.getCode())
            .eq("a", SdBlogArticle::getVisibility, BlogVisibility.PUBLIC.getCode())
            .eq(authorId != null, "a", SdBlogArticle::getAuthorId, authorId)
            .in(restrict, "a", SdBlogArticle::getArticleId, restrictArticleIds)
            .orderByDesc("a", SdBlogArticle::getPublishedAt)
            .orderByDesc("a", SdBlogArticle::getArticleId)
            .page(page, BlogPublicArticleListItemVo.class);
    }

    /**
     * 上一篇（S28）：发布时间**更早**的相邻公共文章，供详情页底部导航。
     * <p>
     * 两条硬约束，缺一即错：
     * <ul>
     *   <li><b>限定公共可见</b>（status='1' 且 visibility='0'）——否则会链到草稿或已下线文章，
     *       读者点进去就是 404。原生 SQL 不受 {@code @TableLogic} 保护，del_flag 必须手写。</li>
     *   <li><b>双键排序</b>（published_at + article_id）——只按时间排序时，同一秒发布的两篇
     *       顺序不确定，上下篇会来回跳。</li>
     * </ul>
     * 用原生 SQL 而非 QueryBuilder：需要 `(A or (B and C))` 这种 OR 嵌套，DSL 表达不了。
     *
     * @param articleId   当前文章ID（同秒时用于兜底比较）
     * @param publishedAt 当前文章发布时间
     * @return 相邻文章（articleId + title）；没有更早的公共文章时返回 null
     */
    @Select("""
        select a.article_id as articleId, v.title as title
        from sd_blog_article a
        left join sd_blog_article_version v on v.version_id = a.current_version_id
        where a.status = '1' and a.visibility = '0' and a.del_flag = '0'
          and (a.published_at < #{publishedAt}
               or (a.published_at = #{publishedAt} and a.article_id < #{articleId}))
        order by a.published_at desc, a.article_id desc
        limit 1
        """)
    BlogAdjacentArticleVo selectPrevPublic(@Param("articleId") Long articleId,
                                           @Param("publishedAt") LocalDateTime publishedAt);

    /**
     * 下一篇（S28）：发布时间**更晚**的相邻公共文章。约束与排序方向均与
     * {@link #selectPrevPublic} 互为镜像。
     *
     * @param articleId   当前文章ID（同秒时用于兜底比较）
     * @param publishedAt 当前文章发布时间
     * @return 相邻文章（articleId + title）；没有更晚的公共文章时返回 null
     */
    @Select("""
        select a.article_id as articleId, v.title as title
        from sd_blog_article a
        left join sd_blog_article_version v on v.version_id = a.current_version_id
        where a.status = '1' and a.visibility = '0' and a.del_flag = '0'
          and (a.published_at > #{publishedAt}
               or (a.published_at = #{publishedAt} and a.article_id > #{articleId}))
        order by a.published_at asc, a.article_id asc
        limit 1
        """)
    BlogAdjacentArticleVo selectNextPublic(@Param("articleId") Long articleId,
                                           @Param("publishedAt") LocalDateTime publishedAt);

    /**
     * 阅读量原子自增（契约 §5.2 / D8：同步单条 UPDATE，view_count = view_count + 1 原子安全，
     * 不上 Redis/异步）
     *
     * @param articleId 文章ID
     * @return 更新行数
     */
    @Update("update sd_blog_article set view_count = view_count + 1 where article_id = #{articleId} and del_flag = '0'")
    int increaseViewCount(@Param("articleId") Long articleId);

    /**
     * 阅读量待增量回写（S15 / D35）：增量合并而非覆盖，保证并发 flush 不回退；
     * del_flag='0' 守卫防止软删文章被回写（此时返回 0 行，调用方跳过即可）。
     * <p>
     * 人工复核点（AGENTS.md §9）：增量语义 + 逻辑删除守卫。
     *
     * @param articleId 文章ID
     * @param delta     待增量（>0）
     * @return 更新行数
     */
    @Update("update sd_blog_article set view_count = view_count + #{delta} where article_id = #{articleId} and del_flag = '0'")
    int addViewCount(@Param("articleId") Long articleId, @Param("delta") long delta);

}
