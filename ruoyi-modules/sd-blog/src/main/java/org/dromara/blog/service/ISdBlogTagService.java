package org.dromara.blog.service;

import org.dromara.blog.domain.vo.SdBlogTagVo;
import org.dromara.blog.domain.bo.SdBlogTagBo;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 博客标签Service接口
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
public interface ISdBlogTagService {

    /**
     * 查询博客标签
     *
     * @param tagId 主键
     * @return 博客标签
     */
    SdBlogTagVo queryById(Long tagId);

    /**
     * 分页查询博客标签列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 博客标签分页列表
     */
    PageResult<SdBlogTagVo> queryPageList(SdBlogTagBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的博客标签列表
     *
     * @param bo 查询条件
     * @return 博客标签列表
     */
    List<SdBlogTagVo> queryList(SdBlogTagBo bo);


    /**
     * 新增博客标签
     *
     * @param bo 博客标签
     * @return 是否新增成功
     */
    Boolean insertByBo(SdBlogTagBo bo);

    /**
     * 修改博客标签
     *
     * @param bo 博客标签
     * @return 是否修改成功
     */
    Boolean updateByBo(SdBlogTagBo bo);



    /**
     * 校验并批量删除博客标签信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

}
