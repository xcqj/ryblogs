package org.dromara.blog.service;

import org.dromara.blog.domain.vo.BlogArticleDetailVo;
import org.dromara.blog.domain.vo.BlogArticleListItemVo;
import org.dromara.blog.domain.vo.BlogAdminArticleListItemVo;
import org.dromara.blog.domain.vo.BlogPublicArticleDetailVo;
import org.dromara.blog.domain.vo.BlogPublicArticleListItemVo;
import org.dromara.blog.domain.vo.SdBlogArticleVersionVo;
import org.dromara.blog.domain.bo.BlogArticleMyQueryBo;
import org.dromara.blog.domain.bo.BlogArticleSaveBo;
import org.dromara.blog.domain.bo.BlogAdminArticleQueryBo;
import org.dromara.blog.domain.bo.BlogPublicArticleQueryBo;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.util.List;

/**
 * 博客文章Service接口
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
public interface ISdBlogArticleService {

    /**
     * 创建文章（契约 §3.1）：DRAFT + Version1 + 回写 currentVersionId + 标签同步，同一事务
     *
     * @param bo     保存参数
     * @param userId 当前登录用户ID（作者）
     * @return 新文章ID
     */
    Long createArticle(BlogArticleSaveBo bo, Long userId);

    /**
     * 保存文章（契约 §3.4）：内容实际变化才创建新版本并立即生效；标签始终同步
     *
     * @param articleId 文章ID
     * @param bo        保存参数
     * @param userId    当前登录用户ID
     * @return 是否保存成功
     */
    Boolean updateArticle(Long articleId, BlogArticleSaveBo bo, Long userId);

    /**
     * 发布文章（契约 §3.5）：仅 DRAFT 可发布，首次发布设置 published_at，不创建新版本
     *
     * @param articleId 文章ID
     * @param userId    当前登录用户ID
     * @return 是否发布成功
     */
    Boolean publishArticle(Long articleId, Long userId);

    /**
     * 分页查询我的文章（契约 §3.2）：强制按当前用户过滤，联查当前版本
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @param userId    当前登录用户ID
     * @return 分页结果
     */
    PageResult<BlogArticleListItemVo> queryMyPageList(BlogArticleMyQueryBo bo, PageQuery pageQuery, Long userId);

    /**
     * 查询文章详情（契约 §3.3）：作者本人或管理员
     *
     * @param articleId 文章ID
     * @param userId    当前登录用户ID
     * @param admin     是否管理员
     * @return 文章详情（含当前版本与标签）
     */
    BlogArticleDetailVo queryDetail(Long articleId, Long userId, boolean admin);

    /**
     * 查询文章版本列表（契约 §4.1）：只读，不含正文
     *
     * @param articleId 文章ID
     * @param userId    当前登录用户ID
     * @param admin     是否管理员
     * @return 版本列表（版本号倒序）
     */
    List<SdBlogArticleVersionVo> queryVersions(Long articleId, Long userId, boolean admin);

    /**
     * 查询历史版本详情（契约 §4.2）：只读，含完整快照
     *
     * @param articleId 文章ID
     * @param versionId 版本ID
     * @param userId    当前登录用户ID
     * @param admin     是否管理员
     * @return 版本详情
     */
    SdBlogArticleVersionVo queryVersionDetail(Long articleId, Long versionId, Long userId, boolean admin);

    /**
     * 作者隐藏文章（契约 §3.6）：PUBLISHED 且非 ADMIN_HIDDEN，PUBLIC → USER_HIDDEN，同态幂等
     *
     * @param articleId 文章ID
     * @param userId    当前登录用户ID
     * @return 是否成功
     */
    Boolean hideArticle(Long articleId, Long userId);

    /**
     * 作者重新公开文章（契约 §3.7）：USER_HIDDEN → PUBLIC，不可恢复 ADMIN_HIDDEN，同态幂等
     *
     * @param articleId 文章ID
     * @param userId    当前登录用户ID
     * @return 是否成功
     */
    Boolean showArticle(Long articleId, Long userId);

    /**
     * 作者删除文章（契约 §3.8）：四种状态均可，逻辑删除 del_flag=1，
     * 保留版本/标签关系/评论；管理员不可删除（MVP）
     *
     * @param articleId 文章ID
     * @param userId    当前登录用户ID
     * @return 是否成功
     */
    Boolean deleteByAuthor(Long articleId, Long userId);

    /**
     * 管理员分页查询平台文章（契约 §11.1）：可见全部状态，含治理信息
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    PageResult<BlogAdminArticleListItemVo> queryAdminPageList(BlogAdminArticleQueryBo bo, PageQuery pageQuery);

    /**
     * 管理员下线文章（契约 §11.3）：PUBLISHED+PUBLIC → ADMIN_HIDDEN，写治理三字段，同态幂等
     *
     * @param articleId 文章ID
     * @param reason    下线原因
     * @param adminId   当前管理员用户ID
     * @return 是否成功
     */
    Boolean adminHide(Long articleId, String reason, Long adminId);

    /**
     * 管理员恢复文章（契约 §11.4）：ADMIN_HIDDEN → PUBLIC，status 不变，清空治理字段，同态幂等
     *
     * @param articleId 文章ID
     * @return 是否成功
     */
    Boolean adminRestore(Long articleId);

    /**
     * 公共文章分页（契约 §5.1 / §9.2 共用入口，S6 落地、S8 补全参数）：只返回公共可见三条件文章，
     * 联当前版本取标题/摘要/封面，标签两段式批量填充；tagSlug 无效/无命中返回空页
     *
     * @param bo        过滤条件（tagSlug/authorId）
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    PageResult<BlogPublicArticleListItemVo> queryPublicPageList(BlogPublicArticleQueryBo bo, PageQuery pageQuery);

    /**
     * 标签下公开文章分页（契约 §7.4）：slug/alias 解析 canonical 后只返回公共可见文章；
     * 放在文章侧实现以避免 tag→article 循环依赖（article 已依赖 tag）
     *
     * @param slug      标签或别名 slug
     * @param pageQuery 分页参数
     * @return 公共文章分页
     */
    PageResult<BlogPublicArticleListItemVo> queryPublicTagArticles(String slug, PageQuery pageQuery);

    /**
     * 公共文章详情（契约 §5.2）：非公共可见报“文章不存在”；成功打开时 view_count 原子 +1（D8）
     *
     * @param articleId 文章ID
     * @return 公共详情（含当前版本与标签，不含内部状态字段）
     */
    BlogPublicArticleDetailVo queryPublicDetail(Long articleId);
}
