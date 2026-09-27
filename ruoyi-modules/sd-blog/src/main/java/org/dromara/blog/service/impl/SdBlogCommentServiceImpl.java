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
import org.dromara.blog.domain.bo.SdBlogCommentBo;
import org.dromara.blog.domain.vo.SdBlogCommentVo;
import org.dromara.blog.domain.SdBlogComment;
import org.dromara.blog.mapper.SdBlogCommentMapper;
import org.dromara.blog.service.ISdBlogCommentService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 博客评论Service业务层处理
 *
 * @author Xu
 * @date 2026-09-28 02:23:30
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SdBlogCommentServiceImpl implements ISdBlogCommentService {

    private final SdBlogCommentMapper sdBlogCommentMapper;

    /**
     * 查询博客评论
     *
     * @param commentId 主键
     * @return 博客评论
     */
    @Override
    public SdBlogCommentVo queryById(Long commentId) {
        return sdBlogCommentMapper.selectVoById(commentId);
    }

    /**
     * 分页查询博客评论列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 博客评论分页列表
     */
    @Override
    public PageResult<SdBlogCommentVo> queryPageList(SdBlogCommentBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SdBlogComment> lqw = buildQueryWrapper(bo);
        Page<SdBlogCommentVo> result = sdBlogCommentMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 查询符合条件的博客评论列表
     *
     * @param bo 查询条件
     * @return 博客评论列表
     */
    @Override
    public List<SdBlogCommentVo> queryList(SdBlogCommentBo bo) {
        LambdaQueryWrapper<SdBlogComment> lqw = buildQueryWrapper(bo);
        return sdBlogCommentMapper.selectVoList(lqw);
    }


    private LambdaQueryWrapper<SdBlogComment> buildQueryWrapper(SdBlogCommentBo bo) {
        Map<String, Object> params = bo.getParams();
        return QueryBuilder.lambda(SdBlogComment.class)
            .eqIfPresent(SdBlogComment::getArticleId, bo.getArticleId())
            .eqIfPresent(SdBlogComment::getUserId, bo.getUserId())
            .eqIfPresent(SdBlogComment::getParentId, bo.getParentId())
            .eqIfText(SdBlogComment::getContent, bo.getContent())
            .eqIfPresent(SdBlogComment::getCreateBy, bo.getCreateBy())
            .betweenParams(SdBlogComment::getCreateTime, params, "beginCreateTime", "endCreateTime")
            .orderByAsc(SdBlogComment::getCommentId)
            .build();
    }

    /**
     * 新增博客评论
     *
     * @param bo 博客评论
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(SdBlogCommentBo bo) {
        SdBlogComment add = MapstructUtils.convert(bo, SdBlogComment.class);
        validEntityBeforeSave(add);
        boolean flag = sdBlogCommentMapper.insert(add) > 0;
        if (flag) {
            bo.setCommentId(add.getCommentId());
        }
        return flag;
    }

    /**
     * 修改博客评论
     *
     * @param bo 博客评论
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(SdBlogCommentBo bo) {
        SdBlogComment update = MapstructUtils.convert(bo, SdBlogComment.class);
        validEntityBeforeSave(update);
        return sdBlogCommentMapper.updateById(update) > 0;
    }



    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SdBlogComment entity) {
        // 可在此扩展通用业务校验
    }


    /**
     * 校验并批量删除博客评论信息
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
        return sdBlogCommentMapper.deleteByIds(ids) > 0;
    }

}
