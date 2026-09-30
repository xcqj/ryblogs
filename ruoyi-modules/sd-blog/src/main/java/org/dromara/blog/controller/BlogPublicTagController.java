package org.dromara.blog.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.dromara.blog.domain.vo.BlogPublicArticleListItemVo;
import org.dromara.blog.domain.vo.BlogPublicTagVo;
import org.dromara.blog.service.ISdBlogArticleService;
import org.dromara.blog.service.ISdBlogTagService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公共标签 API
 * <p>
 * 契约 §7：匿名接口，走 security.excludes 的 /blog/public/** 白名单；
 * 只返回 ACTIVE 且未删标签，公共视角不泄露治理字段。
 * 注意：featured/hot 为保留路径（Spring 精确路径优先于 {slug} 模板），
 * 同名 slug 的标签将无法按 §7.3 访问，属契约既定取舍。
 *
 * @author Xu
 * @date 2026-09-30
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/blog/public/tags")
public class BlogPublicTagController extends BaseController {

    private final ISdBlogTagService sdBlogTagService;
    private final ISdBlogArticleService sdBlogArticleService;

    /**
     * 精选标签（§7.1）：is_featured=1，按 sort_order 升序
     *
     * @return 精选标签列表
     */
    @GetMapping("/featured")
    public R<List<BlogPublicTagVo>> featured() {
        return R.ok(sdBlogTagService.queryFeaturedTags());
    }

    /**
     * 热门标签（§7.2）：按公共可见文章数实时统计
     *
     * @param limit 数量上限，默认 10，封顶 50
     * @return 热门标签列表
     */
    @GetMapping("/hot")
    public R<List<BlogPublicTagVo>> hot(@RequestParam(defaultValue = "10")
                                        @Min(value = 1, message = "limit最小为1")
                                        @Max(value = 50, message = "limit最大为50") int limit) {
        return R.ok(sdBlogTagService.queryHotTags(limit));
    }

    /**
     * 公共标签列表/候选联想（§7.5）：keyword 按名称/slug 模糊
     *
     * @param keyword   可选关键字
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    @GetMapping
    public R<PageResult<BlogPublicTagVo>> list(@RequestParam(required = false) String keyword,
                                               PageQuery pageQuery) {
        return R.ok(sdBlogTagService.queryPublicPageList(keyword, pageQuery));
    }

    /**
     * 公共标签详情（§7.3）：slug 或 alias slug 解析到 canonical，301 由前台/边缘层负责
     *
     * @param slug 标签slug
     * @return 标签详情（含公开文章数）
     */
    @GetMapping("/{slug}")
    public R<BlogPublicTagVo> detail(@PathVariable("slug") String slug) {
        return R.ok(sdBlogTagService.queryPublicTagDetail(slug));
    }

    /**
     * 标签下公开文章（§7.4）：只返回公共可见文章
     *
     * @param slug      标签slug
     * @param pageQuery 分页参数
     * @return 公共文章分页
     */
    @GetMapping("/{slug}/articles")
    public R<PageResult<BlogPublicArticleListItemVo>> articles(@PathVariable("slug") String slug,
                                                               PageQuery pageQuery) {
        return R.ok(sdBlogArticleService.queryPublicTagArticles(slug, pageQuery));
    }

}
