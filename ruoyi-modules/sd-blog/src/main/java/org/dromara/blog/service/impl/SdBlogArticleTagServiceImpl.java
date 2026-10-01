package org.dromara.blog.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.dromara.common.mybatis.core.query.QueryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.dromara.blog.domain.SdBlogArticleTag;
import org.dromara.blog.mapper.SdBlogArticleTagMapper;
import org.dromara.blog.service.ISdBlogArticleTagService;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
