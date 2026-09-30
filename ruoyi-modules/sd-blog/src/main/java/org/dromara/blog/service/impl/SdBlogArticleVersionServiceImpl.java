package org.dromara.blog.service.impl;

import cn.hutool.core.util.ObjectUtil;
import org.dromara.common.core.exception.ServiceException;
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
import org.dromara.blog.domain.bo.SdBlogArticleVersionBo;
import org.dromara.blog.domain.vo.SdBlogArticleVersionVo;
import org.dromara.blog.domain.SdBlogArticleVersion;
import org.dromara.blog.mapper.SdBlogArticleVersionMapper;
import org.dromara.blog.service.ISdBlogArticleVersionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 博客文章版本历史Service业务层处理
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SdBlogArticleVersionServiceImpl implements ISdBlogArticleVersionService {

    private final SdBlogArticleVersionMapper sdBlogArticleVersionMapper;

    /**
     * 查询博客文章版本历史
     *
     * @param versionId 主键
     * @return 博客文章版本历史
     */
    @Override
    public SdBlogArticleVersionVo queryById(Long versionId) {
        return sdBlogArticleVersionMapper.selectVoById(versionId);
    }

    /**
     * 分页查询博客文章版本历史列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 博客文章版本历史分页列表
     */
    @Override
    public PageResult<SdBlogArticleVersionVo> queryPageList(SdBlogArticleVersionBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SdBlogArticleVersion> lqw = buildQueryWrapper(bo);
        Page<SdBlogArticleVersionVo> result = sdBlogArticleVersionMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 查询符合条件的博客文章版本历史列表
     *
     * @param bo 查询条件
     * @return 博客文章版本历史列表
     */
    @Override
    public List<SdBlogArticleVersionVo> queryList(SdBlogArticleVersionBo bo) {
        LambdaQueryWrapper<SdBlogArticleVersion> lqw = buildQueryWrapper(bo);
        return sdBlogArticleVersionMapper.selectVoList(lqw);
    }


    private LambdaQueryWrapper<SdBlogArticleVersion> buildQueryWrapper(SdBlogArticleVersionBo bo) {
        Map<String, Object> params = bo.getParams();
        return QueryBuilder.lambda(SdBlogArticleVersion.class)
            .eqIfPresent(SdBlogArticleVersion::getArticleId, bo.getArticleId())
            .eqIfPresent(SdBlogArticleVersion::getVersionNo, bo.getVersionNo())
            .eqIfText(SdBlogArticleVersion::getTitle, bo.getTitle())
            .eqIfText(SdBlogArticleVersion::getSummary, bo.getSummary())
            .eqIfText(SdBlogArticleVersion::getContent, bo.getContent())
            .eqIfPresent(SdBlogArticleVersion::getCreateBy, bo.getCreateBy())
            .betweenParams(SdBlogArticleVersion::getCreateTime, params, "beginCreateTime", "endCreateTime")
            .orderByAsc(SdBlogArticleVersion::getVersionId)
            .build();
    }

    /**
     * 新增博客文章版本历史
     *
     * @param bo 博客文章版本历史
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(SdBlogArticleVersionBo bo) {
        SdBlogArticleVersion add = MapstructUtils.convert(bo, SdBlogArticleVersion.class);
        validEntityBeforeSave(add);
        boolean flag = sdBlogArticleVersionMapper.insert(add) > 0;
        if (flag) {
            bo.setVersionId(add.getVersionId());
        }
        return flag;
    }

    /**
     * 修改博客文章版本历史
     * <p>
     * ⚠️ append-only 红线护栏（AGENTS.md §5 / 契约 §4.2）：版本快照一经写入不可修改。
     * 本方法无业务调用方（Controller 已按契约 §14 停用），保留签名仅为防止将来误用。
     *
     * @param bo 博客文章版本历史
     * @return 永远抛出异常
     */
    @Override
    public Boolean updateByBo(SdBlogArticleVersionBo bo) {
        throw new ServiceException("文章版本为 append-only 快照，禁止修改");
    }



    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SdBlogArticleVersion entity) {
        // 可在此扩展通用业务校验
    }


    /**
     * 校验并批量删除博客文章版本历史信息
     * <p>
     * ⚠️ append-only 红线护栏（AGENTS.md §5 / 契约 §4.2）：版本快照禁止删除。
     * 本方法无业务调用方（Controller 已按契约 §14 停用），保留签名仅为防止将来误用。
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 永远抛出异常
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        throw new ServiceException("文章版本为 append-only 快照，禁止删除");
    }

}
