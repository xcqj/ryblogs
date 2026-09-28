package org.dromara.blog.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.dromara.common.mybatis.core.query.QueryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.dromara.blog.domain.bo.SdBlogArticleTagBo;
import org.dromara.blog.domain.vo.SdBlogArticleTagVo;
import org.dromara.blog.domain.SdBlogArticleTag;
import org.dromara.blog.mapper.SdBlogArticleTagMapper;
import org.dromara.blog.service.ISdBlogArticleTagService;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 博客文章标签关系Service业务层处理
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SdBlogArticleTagServiceImpl implements ISdBlogArticleTagService {

    private final SdBlogArticleTagMapper sdBlogArticleTagMapper;

    /**
     * 查询博客文章标签关系
     *
     * @param articleTagId 主键
     * @return 博客文章标签关系
     */
    @Override
    public SdBlogArticleTagVo queryById(Long articleTagId) {
        return sdBlogArticleTagMapper.selectVoById(articleTagId);
    }

    /**
     * 分页查询博客文章标签关系列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 博客文章标签关系分页列表
     */
    @Override
    public PageResult<SdBlogArticleTagVo> queryPageList(SdBlogArticleTagBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SdBlogArticleTag> lqw = buildQueryWrapper(bo);
        Page<SdBlogArticleTagVo> result = sdBlogArticleTagMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 查询符合条件的博客文章标签关系列表
     *
     * @param bo 查询条件
     * @return 博客文章标签关系列表
     */
    @Override
    public List<SdBlogArticleTagVo> queryList(SdBlogArticleTagBo bo) {
        LambdaQueryWrapper<SdBlogArticleTag> lqw = buildQueryWrapper(bo);
        return sdBlogArticleTagMapper.selectVoList(lqw);
    }


    private LambdaQueryWrapper<SdBlogArticleTag> buildQueryWrapper(SdBlogArticleTagBo bo) {
        Map<String, Object> params = bo.getParams();
        return QueryBuilder.lambda(SdBlogArticleTag.class)
            .eqIfPresent(SdBlogArticleTag::getArticleTagId, bo.getArticleTagId())
            .eqIfPresent(SdBlogArticleTag::getArticleId, bo.getArticleId())
            .eqIfPresent(SdBlogArticleTag::getTagId, bo.getTagId())
            .eqIfPresent(SdBlogArticleTag::getCreateBy, bo.getCreateBy())
            .betweenParams(SdBlogArticleTag::getCreateTime, params, "beginCreateTime", "endCreateTime")
            .orderByAsc(SdBlogArticleTag::getArticleTagId)
            .build();
    }

    /**
     * 新增博客文章标签关系
     *
     * @param bo 博客文章标签关系
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(SdBlogArticleTagBo bo) {
        SdBlogArticleTag add = MapstructUtils.convert(bo, SdBlogArticleTag.class);
        validEntityBeforeSave(add);
        boolean flag = sdBlogArticleTagMapper.insert(add) > 0;
        if (flag) {
            bo.setArticleTagId(add.getArticleTagId());
        }
        return flag;
    }

    /**
     * 修改博客文章标签关系
     *
     * @param bo 博客文章标签关系
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(SdBlogArticleTagBo bo) {
        SdBlogArticleTag update = MapstructUtils.convert(bo, SdBlogArticleTag.class);
        validEntityBeforeSave(update);
        return sdBlogArticleTagMapper.updateById(update) > 0;
    }



    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SdBlogArticleTag entity) {
        // 可在此扩展通用业务校验
    }


    /**
     * 校验并批量删除博客文章标签关系信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 可在此扩展删除前业务校验
        }
        return sdBlogArticleTagMapper.deleteByIds(ids) > 0;
    }

    /**
     * 同步文章标签关系：差集维护，缺的补、多的物理删，已有行保持不动
     *
     * @param articleId 文章ID
     * @param tagIds    目标标签ID集合
     */
    @Override
    public void syncArticleTags(Long articleId, List<Long> tagIds) {
        List<SdBlogArticleTag> existing = sdBlogArticleTagMapper.lambda()
            .eq(SdBlogArticleTag::getArticleId, articleId)
            .list();
        Set<Long> existingTagIds = existing.stream().map(SdBlogArticleTag::getTagId).collect(Collectors.toSet());
        Set<Long> targetTagIds = new LinkedHashSet<>(tagIds);
        // 物理删除多余关系（纯关系表无逻辑删除）
        List<Long> removeIds = existing.stream()
            .filter(rel -> !targetTagIds.contains(rel.getTagId()))
            .map(SdBlogArticleTag::getArticleTagId)
            .toList();
        if (CollUtil.isNotEmpty(removeIds)) {
            sdBlogArticleTagMapper.deleteByIds(removeIds);
        }
        // 补入缺失关系
        List<SdBlogArticleTag> adds = targetTagIds.stream()
            .filter(tagId -> !existingTagIds.contains(tagId))
            .map(tagId -> {
                SdBlogArticleTag rel = new SdBlogArticleTag();
                rel.setArticleId(articleId);
                rel.setTagId(tagId);
                return rel;
            })
            .toList();
        if (CollUtil.isNotEmpty(adds)) {
            sdBlogArticleTagMapper.insertBatch(adds);
        }
    }

}
