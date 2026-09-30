package org.dromara.blog.service;

import org.dromara.blog.domain.vo.BlogArticleDetailVo;
import org.dromara.blog.domain.vo.BlogArticleListItemVo;
import org.dromara.blog.domain.vo.BlogAdminArticleListItemVo;
import org.dromara.blog.domain.vo.SdBlogArticleVersionVo;
import org.dromara.blog.domain.vo.SdBlogArticleVo;
import org.dromara.blog.domain.bo.BlogArticleMyQueryBo;
import org.dromara.blog.domain.bo.BlogArticleSaveBo;
import org.dromara.blog.domain.bo.BlogAdminArticleQueryBo;
import org.dromara.blog.domain.bo.SdBlogArticleBo;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 博客文章Service接口
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
public interface ISdBlogArticleService {

    /**
     * 查询博客文章
     *
     * @param articleId 主键
     * @return 博客文章
     */
    SdBlogArticleVo queryById(Long articleId);

    /**
     * 分页查询博客文章列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 博客文章分页列表
     */
    PageResult<SdBlogArticleVo> queryPageList(SdBlogArticleBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的博客文章列表
     *
     * @param bo 查询条件
     * @return 博客文章列表
     */
    List<SdBlogArticleVo> queryList(SdBlogArticleBo bo);


    /**
     * 新增博客文章
     *
     * @param bo 博客文章
     * @return 是否新增成功
     */
    Boolean insertByBo(SdBlogArticleBo bo);

    /**
     * 修改博客文章
     *
     * @param bo 博客文章
     * @return 是否修改成功
     */
    Boolean updateByBo(SdBlogArticleBo bo);



    /**
     * 校验并批量删除博客文章信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

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
}
