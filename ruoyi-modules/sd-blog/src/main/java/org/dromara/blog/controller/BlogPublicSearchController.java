package org.dromara.blog.controller;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.dromara.blog.domain.vo.BlogPublicArticleListItemVo;
import org.dromara.blog.domain.vo.BlogSearchPageResult;
import org.dromara.blog.service.search.ArticleSearchService;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公共搜索 API
 * <p>
 * 契约 §6.1：匿名接口（/blog/public/** 白名单）。底层经 ArticleSearchService 抽象隔离
 * （S16 起默认 ES 实现、异常降级 MySQL LIKE，契约 §6 已登记）；可选 after 深翻页游标
 * 由上一页响应 nextAfter 透传（mysql 引擎忽略）。
 *
 * @author Xu
 * @date 2026-10-01
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/blog/public/search")
public class BlogPublicSearchController extends BaseController {

    private final ArticleSearchService articleSearchService;

    /**
     * 搜索公开文章（§6.1）：keyword 必填，命中当前版本 title/summary/content 任一；
     * ES 引擎命中行带可选 highlight（D36 服务端高亮）
     *
     * @param keyword   关键字
     * @param pageQuery 分页参数
     * @param after     深翻页游标（可选，opaque；传值时忽略 pageNum）
     * @return 公共文章分页（与 §5.1 列表同构；ES 命中行带可选 highlight）
     */
    @GetMapping
    public R<BlogSearchPageResult<BlogPublicArticleListItemVo>> search(
        @NotBlank(message = "搜索关键字不能为空")
        @RequestParam(value = "keyword", required = false) String keyword,
        PageQuery pageQuery,
        @RequestParam(value = "after", required = false) String after) {
        // required=false 使"参数缺失"与"空串"统一落入 @NotBlank 校验（否则 Spring 先抛
        // MissingServletRequestParameterException，框架无对应 handler 会返回 500 未知异常）
        return R.ok(articleSearchService.search(keyword.trim(), pageQuery, after));
    }

}
