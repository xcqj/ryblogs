package org.dromara.blog.service.support;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.blog.domain.SdBlogArticleTag;
import org.dromara.blog.domain.SdBlogTag;
import org.dromara.blog.domain.vo.SdBlogTagVo;
import org.dromara.blog.mapper.SdBlogArticleTagMapper;
import org.dromara.blog.mapper.SdBlogTagMapper;
import org.dromara.common.mybatis.core.query.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文章标签批量查询组件（两段式，杜绝 N+1）
 * <p>
 * 供文章 Service 与搜索 Service 共用：公共/我的列表、详情、搜索结果的 tags 填充同源，
 * 只输出 ACTIVE 标签的最小展示字段（tagId/name/slug）。
 *
 * @author Xu
 * @date 2026-10-01
 */
@Component
@RequiredArgsConstructor
public class ArticleTagFiller {

    private final SdBlogArticleTagMapper sdBlogArticleTagMapper;
    private final SdBlogTagMapper sdBlogTagMapper;

    /**
     * 按文章ID集合批量查询标签并分组
     *
     * @param articleIds 文章ID集合
     * @return 文章ID → 标签VO列表（无标签的文章不出现在结果中）
     */
    public Map<Long, List<SdBlogTagVo>> queryTagsByArticleIds(List<Long> articleIds) {
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
        LambdaQueryWrapper<SdBlogTag> lqw = QueryBuilder.lambda(SdBlogTag.class)
            .in(SdBlogTag::getTagId, tagIds)
            .build();
        for (SdBlogTag tag : sdBlogTagMapper.selectList(lqw)) {
            SdBlogTagVo vo = new SdBlogTagVo();
            vo.setTagId(tag.getTagId());
            vo.setName(tag.getName());
            vo.setSlug(tag.getSlug());
            tagVoMap.put(tag.getTagId(), vo);
        }
        Map<Long, List<SdBlogTagVo>> result = new HashMap<>();
        for (SdBlogArticleTag rel : relations) {
            SdBlogTagVo vo = tagVoMap.get(rel.getTagId());
            if (vo != null) {
                result.computeIfAbsent(rel.getArticleId(), k -> new ArrayList<>()).add(vo);
            }
        }
        return result;
    }

}
