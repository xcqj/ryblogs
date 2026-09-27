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
import org.dromara.blog.domain.bo.SdBlogArticleBo;
import org.dromara.blog.domain.vo.SdBlogArticleVo;
import org.dromara.blog.domain.SdBlogArticle;
import org.dromara.blog.mapper.SdBlogArticleMapper;
import org.dromara.blog.service.ISdBlogArticleService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 博客文章Service业务层处理
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SdBlogArticleServiceImpl implements ISdBlogArticleService {

    private final SdBlogArticleMapper sdBlogArticleMapper;

    /**
     * 查询博客文章
     *
     * @param articleId 主键
     * @return 博客文章
     */
    @Override
    public SdBlogArticleVo queryById(Long articleId) {
        return sdBlogArticleMapper.selectVoById(articleId);
    }

    /**
     * 分页查询博客文章列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 博客文章分页列表
     */
    @Override
    public PageResult<SdBlogArticleVo> queryPageList(SdBlogArticleBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SdBlogArticle> lqw = buildQueryWrapper(bo);
        Page<SdBlogArticleVo> result = sdBlogArticleMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 查询符合条件的博客文章列表
     *
     * @param bo 查询条件
     * @return 博客文章列表
     */
    @Override
    public List<SdBlogArticleVo> queryList(SdBlogArticleBo bo) {
        LambdaQueryWrapper<SdBlogArticle> lqw = buildQueryWrapper(bo);
        return sdBlogArticleMapper.selectVoList(lqw);
    }


    private LambdaQueryWrapper<SdBlogArticle> buildQueryWrapper(SdBlogArticleBo bo) {
        Map<String, Object> params = bo.getParams();
        return QueryBuilder.lambda(SdBlogArticle.class)
            .eqIfPresent(SdBlogArticle::getAuthorId, bo.getAuthorId())
            .eqIfText(SdBlogArticle::getSlug, bo.getSlug())
            .eqIfPresent(SdBlogArticle::getStatus, bo.getStatus())
            .eqIfText(SdBlogArticle::getVisibility, bo.getVisibility())
            .eqIfPresent(SdBlogArticle::getCurrentVersionId, bo.getCurrentVersionId())
            .eqIfPresent(SdBlogArticle::getViewCount, bo.getViewCount())
            .betweenParams(SdBlogArticle::getPublishedAt, params, "beginPublishedAt", "endPublishedAt")
            .eqIfText(SdBlogArticle::getAdminHiddenReason, bo.getAdminHiddenReason())
            .eqIfPresent(SdBlogArticle::getAdminHiddenBy, bo.getAdminHiddenBy())
            .betweenParams(SdBlogArticle::getAdminHiddenTime, params, "beginAdminHiddenTime", "endAdminHiddenTime")
            .eqIfPresent(SdBlogArticle::getCreateBy, bo.getCreateBy())
            .betweenParams(SdBlogArticle::getCreateTime, params, "beginCreateTime", "endCreateTime")
            .eqIfPresent(SdBlogArticle::getUpdateBy, bo.getUpdateBy())
            .betweenParams(SdBlogArticle::getUpdateTime, params, "beginUpdateTime", "endUpdateTime")
            .orderByAsc(SdBlogArticle::getArticleId)
            .build();
    }

    /**
     * 新增博客文章
     *
     * @param bo 博客文章
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(SdBlogArticleBo bo) {
        SdBlogArticle add = MapstructUtils.convert(bo, SdBlogArticle.class);
        validEntityBeforeSave(add);
        boolean flag = sdBlogArticleMapper.insert(add) > 0;
        if (flag) {
            bo.setArticleId(add.getArticleId());
        }
        return flag;
    }

    /**
     * 修改博客文章
     *
     * @param bo 博客文章
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(SdBlogArticleBo bo) {
        SdBlogArticle update = MapstructUtils.convert(bo, SdBlogArticle.class);
        validEntityBeforeSave(update);
        return sdBlogArticleMapper.updateById(update) > 0;
    }



    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SdBlogArticle entity) {
        // 可在此扩展通用业务校验
    }


    /**
     * 校验并批量删除博客文章信息
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
        return sdBlogArticleMapper.deleteByIds(ids) > 0;
    }

}
