package org.dromara.blog.service;

import org.dromara.blog.domain.vo.BlogCommentListItemVo;
import org.dromara.blog.domain.vo.SdBlogCommentVo;
import org.dromara.blog.domain.bo.BlogAdminCommentQueryBo;
import org.dromara.blog.domain.bo.SdBlogCommentBo;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 博客评论Service接口
 *
 * @author Xu
 * @date 2026-09-28 02:23:30
 */
public interface ISdBlogCommentService {

    /**
     * 查询博客评论
     *
     * @param commentId 主键
     * @return 博客评论
     */
    SdBlogCommentVo queryById(Long commentId);

    /**
     * 分页查询博客评论列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 博客评论分页列表
     */
    PageResult<SdBlogCommentVo> queryPageList(SdBlogCommentBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的博客评论列表
     *
     * @param bo 查询条件
     * @return 博客评论列表
     */
    List<SdBlogCommentVo> queryList(SdBlogCommentBo bo);


    /**
     * 新增博客评论
     *
     * @param bo 博客评论
     * @return 是否新增成功
     */
    Boolean insertByBo(SdBlogCommentBo bo);

    /**
     * 修改博客评论
     *
     * @param bo 博客评论
     * @return 是否修改成功
     */
    Boolean updateByBo(SdBlogCommentBo bo);



    /**
     * 校验并批量删除博客评论信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 发表评论（契约 §10.2）：文章必须 PUBLISHED+PUBLIC+未删；
     * content 纯文本净化后入库，user_id 取登录上下文，parent_id 恒 NULL
     *
     * @param articleId 文章ID（路径）
     * @param content   评论内容
     * @param userId    当前登录用户ID
     * @return 新评论ID
     */
    Long addComment(Long articleId, String content, Long userId);

    /**
     * 公共评论列表（契约 §10.1）：仅公共可见文章可查，只返未删评论，create_time 升序（D17）
     *
     * @param articleId 文章ID
     * @param pageQuery 分页参数
     * @return 评论分页
     */
    PageResult<BlogCommentListItemVo> queryPublicPageList(Long articleId, PageQuery pageQuery);

    /**
     * 管理员评论列表（契约 §12.1）：可按 articleId/userId/keyword 过滤，时间倒序
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 评论分页
     */
    PageResult<BlogCommentListItemVo> queryAdminPageList(BlogAdminCommentQueryBo bo, PageQuery pageQuery);

    /**
     * 管理员删除评论（契约 §12.2）：逻辑删除 del_flag=1，不物理删；不提供批量/用户自删
     *
     * @param commentId 评论ID
     * @return 是否成功
     */
    Boolean adminDelete(Long commentId);

}
