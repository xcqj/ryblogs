package org.dromara.blog.service;

import org.dromara.blog.domain.vo.SdBlogCommentVo;
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

}
