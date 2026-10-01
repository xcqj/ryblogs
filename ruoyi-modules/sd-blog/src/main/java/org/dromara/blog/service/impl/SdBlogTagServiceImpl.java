package org.dromara.blog.service.impl;

import cn.hutool.core.util.ObjectUtil;
import org.dromara.common.core.exception.ServiceException;
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
import org.dromara.blog.domain.SdBlogArticleTag;
import org.dromara.blog.domain.SdBlogTagAlias;
import org.dromara.blog.domain.bo.SdBlogTagBo;
import org.dromara.blog.domain.vo.BlogPublicTagVo;
import org.dromara.blog.domain.vo.SdBlogTagVo;
import org.dromara.blog.domain.SdBlogTag;
import org.dromara.blog.enums.BlogTagStatus;
import org.dromara.blog.mapper.SdBlogArticleTagMapper;
import org.dromara.blog.mapper.SdBlogTagAliasMapper;
import org.dromara.blog.mapper.SdBlogTagMapper;
import org.dromara.blog.service.ISdBlogTagService;
import org.dromara.blog.service.support.TagMergeResolver;
import org.dromara.blog.util.TagNormalizer;
import org.springframework.dao.DuplicateKeyException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final SdBlogTagAliasMapper sdBlogTagAliasMapper;
    private final SdBlogArticleTagMapper sdBlogArticleTagMapper;

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
     * 按规范化规则将标签名列表解析为 canonical 标签实体（契约 §3.1 / §18.10，S6 三件补课）：
     * ① MERGED 沿 merged_into 跟随；② 已软删标签同 slug 恢复原记录而非新建（物理唯一键占用）；
     * ③ 新建前 tag.slug 与 tag_alias.alias_slug 跨表冲突检查（命中 alias 复用其 canonical）。
     */
    @Override
    public List<SdBlogTag> resolveOrCreateByNames(List<String> rawNames) {
        List<SdBlogTag> result = new ArrayList<>();
        for (String name : TagNormalizer.normalizeNames(rawNames)) {
            String slug = TagNormalizer.toSlug(name);
            SdBlogTag tag = sdBlogTagMapper.lambda().eq(SdBlogTag::getSlug, slug).one();
            if (ObjectUtil.isNotNull(tag)) {
                result.add(followChain(tag));
                continue;
            }
            // 跨表冲突：slug 命中历史别名 → 复用其 canonical，不新建撞键标签
            SdBlogTagAlias alias = sdBlogTagAliasMapper.lambda().eq(SdBlogTagAlias::getAliasSlug, slug).one();
            if (ObjectUtil.isNotNull(alias)) {
                SdBlogTag canonical = followChain(sdBlogTagMapper.selectById(alias.getTagId()));
                if (ObjectUtil.isNotNull(canonical)) {
                    result.add(canonical);
                    continue;
                }
            }
            // 软删标签仍占用 uq_blog_tag_slug：恢复原记录（保留历史 tag_id/精选/排序）
            SdBlogTag any = sdBlogTagMapper.selectAnyBySlug(slug);
            if (ObjectUtil.isNotNull(any)) {
                result.add(restoreIfNeeded(any));
                continue;
            }
            SdBlogTag add = new SdBlogTag();
            add.setName(name);
            add.setSlug(slug);
            add.setStatus(BlogTagStatus.ACTIVE.getCode());
            // 默认非精选、排序 0；精选由管理员手动设置（契约 §7.1）
            add.setIsFeatured("0");
            add.setSortOrder(0);
            try {
                sdBlogTagMapper.insert(add);
            } catch (DuplicateKeyException e) {
                // 并发创建同 slug：回查含软删，恢复或复用，彻底消除旧版回查 null 进结果的 NPE 隐患
                SdBlogTag raced = sdBlogTagMapper.selectAnyBySlug(slug);
                if (ObjectUtil.isNull(raced)) {
                    throw new ServiceException("标签创建冲突，请重试");
                }
                add = restoreIfNeeded(raced);
            }
            result.add(followChain(add));
        }
        return result;
    }

    /**
     * 软删记录先恢复再返回；未删记录直接返回
     */
    private SdBlogTag restoreIfNeeded(SdBlogTag tag) {
        if ("1".equals(tag.getDelFlag())) {
            sdBlogTagMapper.restoreByTagId(tag.getTagId());
            tag.setDelFlag("0");
        }
        return followChain(tag);
    }

    /**
     * 沿 merged_into 链跟随到 canonical（带 visited 防御，逻辑同 TagMergeResolver，
     * 此处需逐步查库故不复用纯函数版）
     */
    private SdBlogTag followChain(SdBlogTag tag) {
        if (Objects.isNull(tag)) {
            return null;
        }
        Set<Long> visited = new HashSet<>();
        SdBlogTag current = tag;
        while (BlogTagStatus.MERGED.getCode().equals(current.getStatus())) {
            if (!visited.add(current.getTagId()) || Objects.isNull(current.getMergedInto())) {
                throw new ServiceException("标签合并链异常，请联系管理员处理");
            }
            current = sdBlogTagMapper.selectById(current.getMergedInto());
            if (Objects.isNull(current)) {
                throw new ServiceException("标签合并目标不存在，请联系管理员处理");
            }
        }
        return current;
    }

    /**
     * 精选标签（契约 §7.1）
     */
    @Override
    public List<BlogPublicTagVo> queryFeaturedTags() {
        return sdBlogTagMapper.lambda()
            .eq(SdBlogTag::getIsFeatured, "1")
            .eq(SdBlogTag::getStatus, BlogTagStatus.ACTIVE.getCode())
            .orderByAsc(SdBlogTag::getSortOrder)
            .orderByAsc(SdBlogTag::getTagId)
            .list().stream().map(this::toPublicVo).toList();
    }

    /**
     * 热门标签（契约 §7.2）：实时统计，默认 10、封顶 50（ROADMAP S6 自决项）
     */
    @Override
    public List<BlogPublicTagVo> queryHotTags(int limit) {
        return sdBlogTagMapper.selectHotTags(Math.min(Math.max(limit, 1), 50));
    }

    /**
     * 公共标签分页列表/联想（契约 §7.5）：只返回 ACTIVE，keyword 按名称/slug 模糊
     */
    @Override
    public PageResult<BlogPublicTagVo> queryPublicPageList(String keyword, PageQuery pageQuery) {
        LambdaQueryWrapper<SdBlogTag> lqw = QueryBuilder.lambda(SdBlogTag.class)
            .eq(SdBlogTag::getStatus, BlogTagStatus.ACTIVE.getCode())
            .orderByAsc(SdBlogTag::getTagId)
            .build();
        if (StringUtils.isNotBlank(keyword)) {
            String kw = keyword.trim();
            lqw.and(w -> w.like(SdBlogTag::getName, kw).or().like(SdBlogTag::getSlug, kw));
        }
        Page<SdBlogTag> page = sdBlogTagMapper.selectPage(pageQuery.build(), lqw);
        return PageResult.build(page.getRecords().stream().map(this::toPublicVo).toList(), page.getTotal());
    }

    /**
     * slug 解析（契约 §7.3/§7.4）：当前 slug → alias_slug → canonical
     */
    @Override
    public SdBlogTag resolveCanonicalBySlug(String slug) {
        if (StringUtils.isBlank(slug)) {
            return null;
        }
        SdBlogTag tag = sdBlogTagMapper.lambda().eq(SdBlogTag::getSlug, slug).one();
        if (ObjectUtil.isNotNull(tag)) {
            return followChain(tag);
        }
        SdBlogTagAlias alias = sdBlogTagAliasMapper.lambda().eq(SdBlogTagAlias::getAliasSlug, slug).one();
        if (ObjectUtil.isNotNull(alias)) {
            return followChain(sdBlogTagMapper.selectById(alias.getTagId()));
        }
        return null;
    }

    /**
     * 公共标签详情（契约 §7.3）：解析 canonical + 公开文章计数
     */
    @Override
    public BlogPublicTagVo queryPublicTagDetail(String slug) {
        SdBlogTag tag = resolveCanonicalBySlug(slug);
        if (ObjectUtil.isNull(tag)) {
            throw new ServiceException("标签不存在");
        }
        BlogPublicTagVo vo = toPublicVo(tag);
        vo.setArticleCount(sdBlogArticleTagMapper.countPublicArticlesByTag(tag.getTagId()));
        return vo;
    }

    /**
     * 管理员标签分页（契约 §13.1）：可见 ACTIVE/MERGED，更新时间倒序
     */
    @Override
    public PageResult<SdBlogTagVo> queryAdminPageList(SdBlogTagBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SdBlogTag> lqw = QueryBuilder.lambda(SdBlogTag.class)
            .likeIfText(SdBlogTag::getName, bo.getName())
            .eqIfText(SdBlogTag::getStatus, bo.getStatus())
            .eqIfText(SdBlogTag::getIsFeatured, bo.getIsFeatured())
            .orderByDesc(SdBlogTag::getUpdateTime)
            .orderByDesc(SdBlogTag::getTagId)
            .build();
        Page<SdBlogTagVo> page = sdBlogTagMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 运营属性更新（契约 §13.3）：仅 isFeatured/sortOrder
     */
    @Override
    public Boolean updateOps(Long tagId, String isFeatured, Integer sortOrder) {
        SdBlogTag tag = sdBlogTagMapper.selectById(tagId);
        if (ObjectUtil.isNull(tag)) {
            throw new ServiceException("标签不存在");
        }
        SdBlogTag update = new SdBlogTag();
        update.setTagId(tagId);
        update.setIsFeatured(isFeatured);
        update.setSortOrder(sortOrder);
        return sdBlogTagMapper.updateById(update) > 0;
    }

    /**
     * 重命名（契约 §13.4）：旧 name/slug 入 alias，tag_id 不变；新 slug 跨表冲突拒绝
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean renameTag(Long tagId, String rawName) {
        String name = TagNormalizer.normalizeName(rawName);
        if (StringUtils.isBlank(name)) {
            throw new ServiceException("标签名称不能为空");
        }
        SdBlogTag tag = sdBlogTagMapper.selectById(tagId);
        if (ObjectUtil.isNull(tag)) {
            throw new ServiceException("标签不存在");
        }
        if (BlogTagStatus.MERGED.getCode().equals(tag.getStatus())) {
            throw new ServiceException("已合并的历史标签不可重命名");
        }
        String newSlug = TagNormalizer.toSlug(name);
        if (newSlug.equals(tag.getSlug())) {
            // slug 未变仅改名（大小写等规范化后同 slug 场景）：不建 alias，否则自撞历史
            if (name.equals(tag.getName())) {
                return true;
            }
            SdBlogTag nameOnly = new SdBlogTag();
            nameOnly.setTagId(tagId);
            nameOnly.setName(name);
            return sdBlogTagMapper.updateById(nameOnly) > 0;
        }
        // 跨表唯一性（database-design §9.5）：含软删标签与既有 alias，冲突直拒而非撞库级异常
        if (ObjectUtil.isNotNull(sdBlogTagMapper.selectAnyBySlug(newSlug))) {
            throw new ServiceException("新名称与已有标签（含已删除）冲突");
        }
        if (ObjectUtil.isNotNull(sdBlogTagAliasMapper.lambda().eq(SdBlogTagAlias::getAliasSlug, newSlug).one())) {
            throw new ServiceException("新名称已被其它标签的历史别名占用");
        }
        SdBlogTagAlias alias = new SdBlogTagAlias();
        alias.setTagId(tagId);
        alias.setAliasSlug(tag.getSlug());
        alias.setAliasName(tag.getName());
        sdBlogTagAliasMapper.insert(alias);
        SdBlogTag update = new SdBlogTag();
        update.setTagId(tagId);
        update.setName(name);
        update.setSlug(newSlug);
        return sdBlogTagMapper.updateById(update) > 0;
    }

    /**
     * 合并（契约 §13.5）：同一事务六步；source/target 均 ACTIVE 前提下不可能成环；
     * 关系表重复行物理删除（S2 决议）
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean mergeTag(Long sourceTagId, Long targetTagId) {
        if (Objects.equals(sourceTagId, targetTagId)) {
            throw new ServiceException("不能合并到自身");
        }
        SdBlogTag source = sdBlogTagMapper.selectById(sourceTagId);
        if (ObjectUtil.isNull(source)) {
            throw new ServiceException("源标签不存在");
        }
        if (!BlogTagStatus.ACTIVE.getCode().equals(source.getStatus())) {
            throw new ServiceException("源标签不是当前有效标签，不可合并");
        }
        SdBlogTag target = sdBlogTagMapper.selectById(targetTagId);
        if (ObjectUtil.isNull(target) || !BlogTagStatus.ACTIVE.getCode().equals(target.getStatus())) {
            throw new ServiceException("目标标签必须为当前有效标签");
        }
        // 1&2. 迁移 ArticleTag 关系并对重复去重（冲突行物理删，其余迁移）
        Set<Long> sourceArticles = queryArticleIdsByTag(sourceTagId);
        Set<Long> targetArticles = queryArticleIdsByTag(targetTagId);
        Set<Long> conflict = TagMergeResolver.conflictArticleIds(sourceArticles, targetArticles);
        Set<Long> migrate = TagMergeResolver.migrateArticleIds(sourceArticles, targetArticles);
        if (!conflict.isEmpty()) {
            sdBlogArticleTagMapper.delete(QueryBuilder.lambda(SdBlogArticleTag.class)
                .eq(SdBlogArticleTag::getTagId, sourceTagId)
                .in(SdBlogArticleTag::getArticleId, conflict)
                .build());
        }
        if (!migrate.isEmpty()) {
            sdBlogArticleTagMapper.lambdaUpdate()
                .set(SdBlogArticleTag::getTagId, targetTagId)
                .eq(SdBlogArticleTag::getTagId, sourceTagId)
                .in(SdBlogArticleTag::getArticleId, migrate)
                .update();
        }
        // 3. alias：source 既有别名全部改指最终目标，再把 source 自身 slug/name 登记为别名
        sdBlogTagAliasMapper.lambdaUpdate()
            .set(SdBlogTagAlias::getTagId, targetTagId)
            .eq(SdBlogTagAlias::getTagId, sourceTagId)
            .update();
        if (ObjectUtil.isNull(sdBlogTagAliasMapper.lambda().eq(SdBlogTagAlias::getAliasSlug, source.getSlug()).one())) {
            SdBlogTagAlias alias = new SdBlogTagAlias();
            alias.setTagId(targetTagId);
            alias.setAliasSlug(source.getSlug());
            alias.setAliasName(source.getName());
            sdBlogTagAliasMapper.insert(alias);
        }
        // 4&5. source 置 MERGED，merged_into 直指最终目标（target 即 canonical）
        SdBlogTag update = new SdBlogTag();
        update.setTagId(sourceTagId);
        update.setStatus(BlogTagStatus.MERGED.getCode());
        update.setMergedInto(targetTagId);
        sdBlogTagMapper.updateById(update);
        // 6. 展平：所有指向 source 的历史 MERGED 标签改指 target，不保留长期合并链（A→B、B→C ⇒ A→C）
        sdBlogTagMapper.lambdaUpdate()
            .set(SdBlogTag::getMergedInto, targetTagId)
            .eq(SdBlogTag::getMergedInto, sourceTagId)
            .update();
        return true;
    }

    /**
     * 删除垃圾标签（契约 §13.6）：解除关系（物理删）+ 逻辑删；alias 保留（解析命中已删标签
     * → 前台 404；重输同 slug 由 resolveOrCreate 恢复原标签）
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean adminDelete(Long tagId) {
        SdBlogTag tag = sdBlogTagMapper.selectById(tagId);
        if (ObjectUtil.isNull(tag)) {
            throw new ServiceException("标签不存在");
        }
        if (BlogTagStatus.MERGED.getCode().equals(tag.getStatus())) {
            throw new ServiceException("已合并的历史标签不可删除");
        }
        sdBlogArticleTagMapper.delete(QueryBuilder.lambda(SdBlogArticleTag.class)
            .eq(SdBlogArticleTag::getTagId, tagId)
            .build());
        return sdBlogTagMapper.deleteById(tagId) > 0;
    }

    private Set<Long> queryArticleIdsByTag(Long tagId) {
        return sdBlogArticleTagMapper.lambda()
            .select(SdBlogArticleTag::getArticleId)
            .eq(SdBlogArticleTag::getTagId, tagId)
            .list().stream().map(SdBlogArticleTag::getArticleId).collect(Collectors.toSet());
    }

    private BlogPublicTagVo toPublicVo(SdBlogTag tag) {
        BlogPublicTagVo vo = new BlogPublicTagVo();
        vo.setTagId(tag.getTagId());
        vo.setName(tag.getName());
        vo.setSlug(tag.getSlug());
        return vo;
    }

}
