package org.dromara.blog.service;

import org.dromara.blog.domain.vo.SdBlogArticleVo;
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

}
