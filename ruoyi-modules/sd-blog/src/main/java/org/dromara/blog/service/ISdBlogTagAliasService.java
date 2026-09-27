package org.dromara.blog.service;

import org.dromara.blog.domain.vo.SdBlogTagAliasVo;
import org.dromara.blog.domain.bo.SdBlogTagAliasBo;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 博客标签历史别名Service接口
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
public interface ISdBlogTagAliasService {

    /**
     * 查询博客标签历史别名
     *
     * @param tagAliasId 主键
     * @return 博客标签历史别名
     */
    SdBlogTagAliasVo queryById(Long tagAliasId);

    /**
     * 分页查询博客标签历史别名列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 博客标签历史别名分页列表
     */
    PageResult<SdBlogTagAliasVo> queryPageList(SdBlogTagAliasBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的博客标签历史别名列表
     *
     * @param bo 查询条件
     * @return 博客标签历史别名列表
     */
    List<SdBlogTagAliasVo> queryList(SdBlogTagAliasBo bo);


    /**
     * 新增博客标签历史别名
     *
     * @param bo 博客标签历史别名
     * @return 是否新增成功
     */
    Boolean insertByBo(SdBlogTagAliasBo bo);

    /**
     * 修改博客标签历史别名
     *
     * @param bo 博客标签历史别名
     * @return 是否修改成功
     */
    Boolean updateByBo(SdBlogTagAliasBo bo);



    /**
     * 校验并批量删除博客标签历史别名信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

}
