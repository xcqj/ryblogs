package org.dromara.blog.service;

import org.dromara.blog.domain.SdBlogTag;
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

    /**
     * 按规范化规则将标签名列表解析为标签实体（契约 §3.1，PRD TAG-02/03）：
     * 同 slug 已存在则复用；不存在则自动入库（无需预审核）。
     * <p>
     * 已删除标签的恢复、MERGED/alias 跟随与跨表 slug 唯一性校验属 S6 标签治理切片接入。
     *
     * @param rawNames 原始标签名列表
     * @return 规范化去重后的标签实体列表（含新建的）
     */
    List<SdBlogTag> resolveOrCreateByNames(List<String> rawNames);
}
