package org.dromara.blog.service.impl;

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
import org.dromara.blog.domain.bo.SdBlogTagBo;
import org.dromara.blog.domain.vo.SdBlogTagVo;
import org.dromara.blog.domain.SdBlogTag;
import org.dromara.blog.mapper.SdBlogTagMapper;
import org.dromara.blog.service.ISdBlogTagService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 博客标签Service业务层处理
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SdBlogTagServiceImpl implements ISdBlogTagService {

    private final SdBlogTagMapper sdBlogTagMapper;

    /**
     * 查询博客标签
     *
     * @param tagId 主键
     * @return 博客标签
     */
    @Override
    public SdBlogTagVo queryById(Long tagId) {
        return sdBlogTagMapper.selectVoById(tagId);
    }

    /**
     * 分页查询博客标签列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 博客标签分页列表
     */
    @Override
    public PageResult<SdBlogTagVo> queryPageList(SdBlogTagBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SdBlogTag> lqw = buildQueryWrapper(bo);
        Page<SdBlogTagVo> result = sdBlogTagMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 查询符合条件的博客标签列表
     *
     * @param bo 查询条件
     * @return 博客标签列表
     */
    @Override
    public List<SdBlogTagVo> queryList(SdBlogTagBo bo) {
        LambdaQueryWrapper<SdBlogTag> lqw = buildQueryWrapper(bo);
        return sdBlogTagMapper.selectVoList(lqw);
    }


    private LambdaQueryWrapper<SdBlogTag> buildQueryWrapper(SdBlogTagBo bo) {
        Map<String, Object> params = bo.getParams();
        return QueryBuilder.lambda(SdBlogTag.class)
            .likeIfText(SdBlogTag::getName, bo.getName())
            .eqIfText(SdBlogTag::getSlug, bo.getSlug())
            .eqIfPresent(SdBlogTag::getStatus, bo.getStatus())
            .eqIfPresent(SdBlogTag::getMergedInto, bo.getMergedInto())
            .eqIfPresent(SdBlogTag::getIsFeatured, bo.getIsFeatured())
            .eqIfPresent(SdBlogTag::getSortOrder, bo.getSortOrder())
            .eqIfPresent(SdBlogTag::getCreateBy, bo.getCreateBy())
            .betweenParams(SdBlogTag::getCreateTime, params, "beginCreateTime", "endCreateTime")
            .orderByAsc(SdBlogTag::getTagId)
            .build();
    }

    /**
     * 新增博客标签
     *
     * @param bo 博客标签
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(SdBlogTagBo bo) {
        SdBlogTag add = MapstructUtils.convert(bo, SdBlogTag.class);
        validEntityBeforeSave(add);
        boolean flag = sdBlogTagMapper.insert(add) > 0;
        if (flag) {
            bo.setTagId(add.getTagId());
        }
        return flag;
    }

    /**
     * 修改博客标签
     *
     * @param bo 博客标签
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(SdBlogTagBo bo) {
        SdBlogTag update = MapstructUtils.convert(bo, SdBlogTag.class);
        validEntityBeforeSave(update);
        return sdBlogTagMapper.updateById(update) > 0;
    }



    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SdBlogTag entity) {
        // 可在此扩展通用业务校验
    }


    /**
     * 校验并批量删除博客标签信息
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
        return sdBlogTagMapper.deleteByIds(ids) > 0;
    }

}
