package org.dromara.blog.service;

import org.dromara.blog.domain.vo.BlogArticleDetailVo;
import org.dromara.blog.domain.vo.BlogArticleListItemVo;
import org.dromara.blog.domain.vo.SdBlogArticleVersionVo;
import org.dromara.blog.domain.vo.SdBlogArticleVo;
import org.dromara.blog.domain.bo.BlogArticleMyQueryBo;
import org.dromara.blog.domain.bo.BlogArticleSaveBo;
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
}
