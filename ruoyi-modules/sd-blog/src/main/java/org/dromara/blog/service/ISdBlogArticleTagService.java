package org.dromara.blog.service;

import org.dromara.blog.domain.vo.SdBlogArticleTagVo;
import org.dromara.blog.domain.bo.SdBlogArticleTagBo;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 博客文章标签关系Service接口
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
public interface ISdBlogArticleTagService {

    /**
     * 查询博客文章标签关系
     *
     * @param articleTagId 主键
     * @return 博客文章标签关系
     */
    SdBlogArticleTagVo queryById(Long articleTagId);

    /**
     * 分页查询博客文章标签关系列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 博客文章标签关系分页列表
     */
    PageResult<SdBlogArticleTagVo> queryPageList(SdBlogArticleTagBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的博客文章标签关系列表
     *
     * @param bo 查询条件
     * @return 博客文章标签关系列表
     */
    List<SdBlogArticleTagVo> queryList(SdBlogArticleTagBo bo);


    /**
     * 新增博客文章标签关系
     *
     * @param bo 博客文章标签关系
     * @return 是否新增成功
     */
    Boolean insertByBo(SdBlogArticleTagBo bo);

    /**
     * 修改博客文章标签关系
     *
     * @param bo 博客文章标签关系
     * @return 是否修改成功
     */
    Boolean updateByBo(SdBlogArticleTagBo bo);



    /**
     * 校验并批量删除博客文章标签关系信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 同步文章标签关系：目标集合与现状做差集，缺的补、多的物理删，已有行保持不动
     * （契约 §3.4"保存时同步标签"，database-design §10；纯关系表物理删除，S2 决议）。
     *
     * @param articleId 文章ID
     * @param tagIds    目标标签ID集合（调用方保证已规范化且 1~6 个）
     */
    void syncArticleTags(Long articleId, List<Long> tagIds);
}
