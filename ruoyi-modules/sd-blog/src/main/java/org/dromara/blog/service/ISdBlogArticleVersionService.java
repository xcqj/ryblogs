package org.dromara.blog.service;

import org.dromara.blog.domain.vo.SdBlogArticleVersionVo;
import org.dromara.blog.domain.bo.SdBlogArticleVersionBo;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 博客文章版本历史Service接口
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
public interface ISdBlogArticleVersionService {

    /**
     * 查询博客文章版本历史
     *
     * @param versionId 主键
     * @return 博客文章版本历史
     */
    SdBlogArticleVersionVo queryById(Long versionId);

    /**
     * 分页查询博客文章版本历史列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 博客文章版本历史分页列表
     */
    PageResult<SdBlogArticleVersionVo> queryPageList(SdBlogArticleVersionBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的博客文章版本历史列表
     *
     * @param bo 查询条件
     * @return 博客文章版本历史列表
     */
    List<SdBlogArticleVersionVo> queryList(SdBlogArticleVersionBo bo);


    /**
     * 新增博客文章版本历史
     *
     * @param bo 博客文章版本历史
     * @return 是否新增成功
     */
    Boolean insertByBo(SdBlogArticleVersionBo bo);

    /**
     * 修改博客文章版本历史
     *
     * @param bo 博客文章版本历史
     * @return 是否修改成功
     */
    Boolean updateByBo(SdBlogArticleVersionBo bo);



    /**
     * 校验并批量删除博客文章版本历史信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

}
