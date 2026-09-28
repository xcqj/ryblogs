package org.dromara.blog.service.impl;

import cn.hutool.core.collection.CollUtil;
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
import org.springframework.transaction.annotation.Transactional;
import org.dromara.blog.domain.SdBlogArticle;
import org.dromara.blog.domain.SdBlogArticleTag;
import org.dromara.blog.domain.SdBlogArticleVersion;
import org.dromara.blog.domain.SdBlogTag;
import org.dromara.blog.domain.bo.BlogArticleMyQueryBo;
import org.dromara.blog.domain.bo.BlogArticleSaveBo;
import org.dromara.blog.domain.bo.SdBlogArticleBo;
import org.dromara.blog.domain.vo.BlogArticleDetailVo;
import org.dromara.blog.domain.vo.BlogArticleListItemVo;
import org.dromara.blog.domain.vo.SdBlogArticleVersionVo;
import org.dromara.blog.domain.vo.SdBlogArticleVo;
import org.dromara.blog.domain.vo.SdBlogTagVo;
import org.dromara.blog.enums.BlogArticleStatus;
import org.dromara.blog.enums.BlogVisibility;
import org.dromara.blog.mapper.SdBlogArticleMapper;
import org.dromara.blog.mapper.SdBlogArticleTagMapper;
import org.dromara.blog.mapper.SdBlogArticleVersionMapper;
import org.dromara.blog.mapper.SdBlogTagMapper;
import org.dromara.blog.service.ISdBlogArticleService;
import org.dromara.blog.service.ISdBlogArticleTagService;
import org.dromara.blog.service.ISdBlogTagService;
import org.dromara.blog.service.support.ArticleGuards;
import org.dromara.blog.service.support.VersionPolicy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.Objects;

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
    private final SdBlogArticleVersionMapper sdBlogArticleVersionMapper;
    private final SdBlogArticleTagMapper sdBlogArticleTagMapper;
    private final SdBlogTagMapper sdBlogTagMapper;
    private final ISdBlogTagService sdBlogTagService;
    private final ISdBlogArticleTagService sdBlogArticleTagService;

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

    /**
     * 创建文章：DRAFT + Version 1 + 回写 currentVersionId + 标签同步，同一事务（契约 §3.1）
     *
     * @param bo     保存参数
     * @param userId 当前登录用户ID（作者）
     * @return 新文章ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long createArticle(BlogArticleSaveBo bo, Long userId) {
        List<SdBlogTag> tags = sdBlogTagService.resolveOrCreateByNames(bo.getTagNames());
        validateTagCount(tags);
        SdBlogArticle article = new SdBlogArticle();
        article.setAuthorId(userId);
        article.setStatus(BlogArticleStatus.DRAFT.getCode());
        article.setVisibility(BlogVisibility.PUBLIC.getCode());
        article.setViewCount(0L);
        sdBlogArticleMapper.insert(article);
        SdBlogArticleVersion version = buildVersion(article.getArticleId(), null, bo);
        sdBlogArticleVersionMapper.insert(version);
        SdBlogArticle update = new SdBlogArticle();
        update.setArticleId(article.getArticleId());
        update.setCurrentVersionId(version.getVersionId());
        sdBlogArticleMapper.updateById(update);
        sdBlogArticleTagService.syncArticleTags(article.getArticleId(), toTagIds(tags));
        return article.getArticleId();
    }

    /**
     * 保存文章：内容实际变化才创建新版本并立即生效；标签始终同步（契约 §3.4）
     *
     * @param articleId 文章ID
     * @param bo        保存参数
     * @param userId    当前登录用户ID
     * @return 是否保存成功
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateArticle(Long articleId, BlogArticleSaveBo bo, Long userId) {
        SdBlogArticle article = sdBlogArticleMapper.selectById(articleId);
        ArticleGuards.requireEditable(article, userId);
        List<SdBlogTag> tags = sdBlogTagService.resolveOrCreateByNames(bo.getTagNames());
        validateTagCount(tags);
        SdBlogArticleVersion current = Objects.isNull(article.getCurrentVersionId())
            ? null : sdBlogArticleVersionMapper.selectById(article.getCurrentVersionId());
        boolean flag = true;
        if (VersionPolicy.contentChanged(current, bo.getTitle(), bo.getSummary(), bo.getCoverUrl(), bo.getContent())) {
            SdBlogArticleVersion version = buildVersion(articleId, queryMaxVersionNo(articleId), bo);
            sdBlogArticleVersionMapper.insert(version);
            SdBlogArticle update = new SdBlogArticle();
            update.setArticleId(articleId);
            update.setCurrentVersionId(version.getVersionId());
            flag = sdBlogArticleMapper.updateById(update) > 0;
        }
        sdBlogArticleTagService.syncArticleTags(articleId, toTagIds(tags));
        return flag;
    }

    /**
     * 发布文章：仅 DRAFT 可发布，首次发布设置 publishedAt，不创建新版本（契约 §3.5）
     *
     * @param articleId 文章ID
     * @param userId    当前登录用户ID
     * @return 是否发布成功
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean publishArticle(Long articleId, Long userId) {
        SdBlogArticle article = sdBlogArticleMapper.selectById(articleId);
        ArticleGuards.requirePublishable(article, userId);
        long tagCount = sdBlogArticleTagMapper.lambda()
            .eq(SdBlogArticleTag::getArticleId, articleId)
            .count();
        if (tagCount < 1) {
            throw new ServiceException("文章没有标签，请先添加 1~6 个标签再发布");
        }
        SdBlogArticle update = new SdBlogArticle();
        update.setArticleId(articleId);
        update.setStatus(BlogArticleStatus.PUBLISHED.getCode());
        // 仅首次发布记录时间，后续编辑/重新公开不更新（契约 §3.5）
        if (Objects.isNull(article.getPublishedAt())) {
            update.setPublishedAt(LocalDateTime.now());
        }
        return sdBlogArticleMapper.updateById(update) > 0;
    }

    /**
     * 分页查询我的文章：强制按当前用户过滤，联查当前版本（契约 §3.2）
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @param userId    当前登录用户ID
     * @return 分页结果
     */
    @Override
    public PageResult<BlogArticleListItemVo> queryMyPageList(BlogArticleMyQueryBo bo, PageQuery pageQuery, Long userId) {
        Page<BlogArticleListItemVo> page = sdBlogArticleMapper.selectMyPage(pageQuery.build(), bo, userId);
        fillTags(page.getRecords());
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 查询文章详情：作者本人或管理员（契约 §3.3）
     *
     * @param articleId 文章ID
     * @param userId    当前登录用户ID
     * @param admin     是否管理员
     * @return 文章详情
     */
    @Override
    public BlogArticleDetailVo queryDetail(Long articleId, Long userId, boolean admin) {
        SdBlogArticle article = sdBlogArticleMapper.selectById(articleId);
        ArticleGuards.requireViewable(article, userId, admin);
        BlogArticleDetailVo vo = new BlogArticleDetailVo();
        vo.setArticleId(article.getArticleId());
        vo.setAuthorId(article.getAuthorId());
        vo.setSlug(article.getSlug());
        vo.setStatus(article.getStatus());
        vo.setVisibility(article.getVisibility());
        vo.setViewCount(article.getViewCount());
        vo.setPublishedAt(article.getPublishedAt());
        vo.setCreateTime(article.getCreateTime());
        vo.setUpdateTime(article.getUpdateTime());
        if (Objects.nonNull(article.getCurrentVersionId())) {
            vo.setCurrentVersion(toVersionVo(sdBlogArticleVersionMapper.selectById(article.getCurrentVersionId())));
        }
        vo.setTags(queryTagsByArticleIds(List.of(articleId)).getOrDefault(articleId, List.of()));
        return vo;
    }

    /**
     * 查询文章版本列表：只读、不含正文大字段（契约 §4.1，列表带摘要/封面供展示）
     *
     * @param articleId 文章ID
     * @param userId    当前登录用户ID
     * @param admin     是否管理员
     * @return 版本列表（版本号倒序）
     */
    @Override
    public List<SdBlogArticleVersionVo> queryVersions(Long articleId, Long userId, boolean admin) {
        SdBlogArticle article = sdBlogArticleMapper.selectById(articleId);
        ArticleGuards.requireViewable(article, userId, admin);
        return sdBlogArticleVersionMapper.lambda()
            .select(SdBlogArticleVersion::getVersionId, SdBlogArticleVersion::getArticleId,
                SdBlogArticleVersion::getVersionNo, SdBlogArticleVersion::getTitle,
                SdBlogArticleVersion::getSummary, SdBlogArticleVersion::getCoverUrl,
                SdBlogArticleVersion::getCreateBy, SdBlogArticleVersion::getCreateTime)
            .eq(SdBlogArticleVersion::getArticleId, articleId)
            .orderByDesc(SdBlogArticleVersion::getVersionNo)
            .list().stream().map(this::toVersionVo).toList();
    }

    /**
     * 查询历史版本详情：只读、含完整快照（契约 §4.2）
     *
     * @param articleId 文章ID
     * @param versionId 版本ID
     * @param userId    当前登录用户ID
     * @param admin     是否管理员
     * @return 版本详情
     */
    @Override
    public SdBlogArticleVersionVo queryVersionDetail(Long articleId, Long versionId, Long userId, boolean admin) {
        SdBlogArticle article = sdBlogArticleMapper.selectById(articleId);
        ArticleGuards.requireViewable(article, userId, admin);
        SdBlogArticleVersion version = sdBlogArticleVersionMapper.selectById(versionId);
        if (Objects.isNull(version) || !Objects.equals(version.getArticleId(), articleId)) {
            throw new ServiceException("该版本不存在");
        }
        return toVersionVo(version);
    }

    /**
     * 构建版本快照实体
     *
     * @param articleId    文章ID
     * @param maxVersionNo 当前最大版本号（null 表示无版本）
     * @param bo           保存参数
     * @return 版本实体
     */
    private SdBlogArticleVersion buildVersion(Long articleId, Integer maxVersionNo, BlogArticleSaveBo bo) {
        SdBlogArticleVersion version = new SdBlogArticleVersion();
        version.setArticleId(articleId);
        version.setVersionNo(VersionPolicy.nextVersionNo(maxVersionNo));
        version.setTitle(bo.getTitle());
        version.setSummary(bo.getSummary());
        version.setCoverUrl(bo.getCoverUrl());
        version.setContent(bo.getContent());
        return version;
    }

    /**
     * 查询文章当前最大版本号
     *
     * @param articleId 文章ID
     * @return 最大版本号；无版本时 null
     */
    private Integer queryMaxVersionNo(Long articleId) {
        List<SdBlogArticleVersion> versions = sdBlogArticleVersionMapper.lambda()
            .select(SdBlogArticleVersion::getVersionNo)
            .eq(SdBlogArticleVersion::getArticleId, articleId)
            .orderByDesc(SdBlogArticleVersion::getVersionNo)
            .list();
        return versions.isEmpty() ? null : versions.getFirst().getVersionNo();
    }

    /**
     * 规范化去重后的标签数量仍须满足 1~6（契约 §3.1）
     */
    private void validateTagCount(List<SdBlogTag> tags) {
        if (tags.isEmpty() || tags.size() > 6) {
            throw new ServiceException("文章标签数量必须为1~6个");
        }
    }

    private List<Long> toTagIds(List<SdBlogTag> tags) {
        return tags.stream().map(SdBlogTag::getTagId).toList();
    }

    private SdBlogArticleVersionVo toVersionVo(SdBlogArticleVersion version) {
        if (Objects.isNull(version)) {
            return null;
        }
        SdBlogArticleVersionVo vo = new SdBlogArticleVersionVo();
        vo.setVersionId(version.getVersionId());
        vo.setArticleId(version.getArticleId());
        vo.setVersionNo(version.getVersionNo());
        vo.setTitle(version.getTitle());
        vo.setSummary(version.getSummary());
        vo.setCoverUrl(version.getCoverUrl());
        vo.setContent(version.getContent());
        vo.setCreateBy(version.getCreateBy());
        vo.setCreateTime(version.getCreateTime());
        return vo;
    }

    /**
     * 批量为列表项填充标签（两段式，杜绝 N+1）
     */
    private void fillTags(List<BlogArticleListItemVo> records) {
        if (CollUtil.isEmpty(records)) {
            return;
        }
        Map<Long, List<SdBlogTagVo>> tagMap = queryTagsByArticleIds(
            records.stream().map(BlogArticleListItemVo::getArticleId).toList());
        records.forEach(item -> item.setTags(tagMap.getOrDefault(item.getArticleId(), List.of())));
    }

    /**
     * 按文章ID集合批量查询标签，分组返回
     *
     * @param articleIds 文章ID集合
     * @return 文章ID → 标签VO列表
     */
    private Map<Long, List<SdBlogTagVo>> queryTagsByArticleIds(List<Long> articleIds) {
        if (CollUtil.isEmpty(articleIds)) {
            return Map.of();
        }
        List<SdBlogArticleTag> relations = sdBlogArticleTagMapper.lambda()
            .in(SdBlogArticleTag::getArticleId, articleIds)
            .list();
        if (CollUtil.isEmpty(relations)) {
            return Map.of();
        }
        List<Long> tagIds = relations.stream().map(SdBlogArticleTag::getTagId).distinct().toList();
        Map<Long, SdBlogTagVo> tagVoMap = new HashMap<>();
        for (SdBlogTag tag : sdBlogTagMapper.lambda().in(SdBlogTag::getTagId, tagIds).list()) {
            SdBlogTagVo vo = new SdBlogTagVo();
            vo.setTagId(tag.getTagId());
            vo.setName(tag.getName());
            vo.setSlug(tag.getSlug());
            tagVoMap.put(tag.getTagId(), vo);
        }
        Map<Long, List<SdBlogTagVo>> result = new HashMap<>();
        for (SdBlogArticleTag rel : relations) {
            SdBlogTagVo vo = tagVoMap.get(rel.getTagId());
            if (Objects.nonNull(vo)) {
                result.computeIfAbsent(rel.getArticleId(), k -> new ArrayList<>()).add(vo);
            }
        }
        return result;
    }

}
