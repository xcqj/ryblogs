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
import org.dromara.blog.domain.bo.SdBlogTagAliasBo;
import org.dromara.blog.domain.vo.SdBlogTagAliasVo;
import org.dromara.blog.domain.SdBlogTagAlias;
import org.dromara.blog.mapper.SdBlogTagAliasMapper;
import org.dromara.blog.service.ISdBlogTagAliasService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 博客标签历史别名Service业务层处理
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SdBlogTagAliasServiceImpl implements ISdBlogTagAliasService {

    private final SdBlogTagAliasMapper sdBlogTagAliasMapper;

    /**
     * 查询博客标签历史别名
     *
     * @param tagAliasId 主键
     * @return 博客标签历史别名
     */
    @Override
    public SdBlogTagAliasVo queryById(Long tagAliasId) {
        return sdBlogTagAliasMapper.selectVoById(tagAliasId);
    }

    /**
     * 分页查询博客标签历史别名列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 博客标签历史别名分页列表
     */
    @Override
    public PageResult<SdBlogTagAliasVo> queryPageList(SdBlogTagAliasBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SdBlogTagAlias> lqw = buildQueryWrapper(bo);
        Page<SdBlogTagAliasVo> result = sdBlogTagAliasMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 查询符合条件的博客标签历史别名列表
     *
     * @param bo 查询条件
     * @return 博客标签历史别名列表
     */
    @Override
    public List<SdBlogTagAliasVo> queryList(SdBlogTagAliasBo bo) {
        LambdaQueryWrapper<SdBlogTagAlias> lqw = buildQueryWrapper(bo);
        return sdBlogTagAliasMapper.selectVoList(lqw);
    }


    private LambdaQueryWrapper<SdBlogTagAlias> buildQueryWrapper(SdBlogTagAliasBo bo) {
        Map<String, Object> params = bo.getParams();
        return QueryBuilder.lambda(SdBlogTagAlias.class)
            .eqIfPresent(SdBlogTagAlias::getTagId, bo.getTagId())
            .eqIfText(SdBlogTagAlias::getAliasSlug, bo.getAliasSlug())
            .likeIfText(SdBlogTagAlias::getAliasName, bo.getAliasName())
            .eqIfPresent(SdBlogTagAlias::getCreateBy, bo.getCreateBy())
            .betweenParams(SdBlogTagAlias::getCreateTime, params, "beginCreateTime", "endCreateTime")
            .orderByAsc(SdBlogTagAlias::getTagAliasId)
            .build();
    }

    /**
     * 新增博客标签历史别名
     *
     * @param bo 博客标签历史别名
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(SdBlogTagAliasBo bo) {
        SdBlogTagAlias add = MapstructUtils.convert(bo, SdBlogTagAlias.class);
        validEntityBeforeSave(add);
        boolean flag = sdBlogTagAliasMapper.insert(add) > 0;
        if (flag) {
            bo.setTagAliasId(add.getTagAliasId());
        }
        return flag;
    }

    /**
     * 修改博客标签历史别名
     *
     * @param bo 博客标签历史别名
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(SdBlogTagAliasBo bo) {
        SdBlogTagAlias update = MapstructUtils.convert(bo, SdBlogTagAlias.class);
        validEntityBeforeSave(update);
        return sdBlogTagAliasMapper.updateById(update) > 0;
    }



    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SdBlogTagAlias entity) {
        // 可在此扩展通用业务校验
    }


    /**
     * 校验并批量删除博客标签历史别名信息
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
        return sdBlogTagAliasMapper.deleteByIds(ids) > 0;
    }

}
