package org.dromara.blog.service.impl;

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
import org.dromara.blog.domain.SdBlogArticle;
import org.dromara.blog.domain.SdBlogArticleVersion;
import org.dromara.blog.domain.bo.BlogAdminCommentQueryBo;
import org.dromara.blog.domain.vo.BlogCommentListItemVo;
import org.dromara.blog.domain.SdBlogComment;
import org.dromara.blog.mapper.SdBlogArticleMapper;
import org.dromara.blog.mapper.SdBlogArticleVersionMapper;
import org.dromara.blog.mapper.SdBlogCommentMapper;
import org.dromara.blog.service.ISdBlogCommentService;
import org.dromara.blog.service.support.ArticleGuards;
import org.dromara.blog.util.HtmlSanitizer;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final SdBlogArticleMapper sdBlogArticleMapper;
    private final SdBlogArticleVersionMapper sdBlogArticleVersionMapper;

    /**
     * 发表评论（契约 §10.2）：目标文章必须 PUBLISHED+PUBLIC+未删（软删文章 selectById 被
     * @TableLogic 过滤，自然落入“文章不存在”）；内容纯文本净化，空内容拒绝；
     * 归属字段全部服务端固定，不信任任何外部传入
     */
    @Override
    public Long addComment(Long articleId, String content, Long userId) {
        SdBlogArticle article = sdBlogArticleMapper.selectById(articleId);
        if (Objects.isNull(article) || !ArticleGuards.isPublicVisible(article)) {
            throw new ServiceException("文章不存在或不可评论");
        }
        // 评论是纯文本字段：剥离粘贴的 HTML 标签（同 title 口径），净化后为空则拒绝
        String clean = HtmlSanitizer.sanitizePlainText(content);
        if (StringUtils.isBlank(clean)) {
            throw new ServiceException("评论内容有效内容为空，请检查是否仅含不安全标签");
        }
        SdBlogComment comment = new SdBlogComment();
        comment.setArticleId(articleId);
        comment.setUserId(userId);
        comment.setParentId(null);
        comment.setContent(clean);
        sdBlogCommentMapper.insert(comment);
        return comment.getCommentId();
    }

    /**
     * 公共评论列表（契约 §10.1）：先验文章公共可见，防通过评论探测草稿/隐藏文章存在性
     */
    @Override
    public PageResult<BlogCommentListItemVo> queryPublicPageList(Long articleId, PageQuery pageQuery) {
        SdBlogArticle article = sdBlogArticleMapper.selectById(articleId);
        if (Objects.isNull(article) || !ArticleGuards.isPublicVisible(article)) {
            throw new ServiceException("文章不存在");
        }
        LambdaQueryWrapper<SdBlogComment> lqw = QueryBuilder.lambda(SdBlogComment.class)
            .eq(SdBlogComment::getArticleId, articleId)
            .orderByAsc(SdBlogComment::getCreateTime)
            .orderByAsc(SdBlogComment::getCommentId)
            .build();
        Page<SdBlogComment> page = sdBlogCommentMapper.selectPage(pageQuery.build(), lqw);
        return PageResult.build(page.getRecords().stream().map(this::toListItem).toList(), page.getTotal());
    }

    /**
     * 管理员评论列表（契约 §12.1）：时间倒序；已删评论被 @TableLogic 过滤（治理只面对现存评论）
     */
    @Override
    public PageResult<BlogCommentListItemVo> queryAdminPageList(BlogAdminCommentQueryBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SdBlogComment> lqw = QueryBuilder.lambda(SdBlogComment.class)
            .eqIfPresent(SdBlogComment::getArticleId, bo.getArticleId())
            .eqIfPresent(SdBlogComment::getUserId, bo.getUserId())
            .likeIfText(SdBlogComment::getContent, bo.getKeyword())
            .orderByDesc(SdBlogComment::getCreateTime)
            .orderByDesc(SdBlogComment::getCommentId)
            .build();
        Page<SdBlogComment> page = sdBlogCommentMapper.selectPage(pageQuery.build(), lqw);
        List<BlogCommentListItemVo> list = page.getRecords().stream().map(this::toListItem).toList();
        // 审核时必须知道评论挂在哪篇文章下，否则只有一个 articleId 等于盲审
        fillArticleTitles(list);
        return PageResult.build(list, page.getTotal());
    }

    /**
     * 批量回填「所属文章标题」：评论表只存 articleId，标题在版本表里。
     * 两跳查询（文章 → currentVersionId，版本 → title），按当前分页一次取完，避免 N+1。
     * 查不到标题时保持 null，前端回落显示 {@code #articleId}。
     *
     * @param list 当前页评论
     */
    private void fillArticleTitles(List<BlogCommentListItemVo> list) {
        if (list.isEmpty()) {
            return;
        }
        Set<Long> articleIds = list.stream()
            .map(BlogCommentListItemVo::getArticleId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (articleIds.isEmpty()) {
            return;
        }
        Map<Long, Long> versionIdByArticle = sdBlogArticleMapper.selectByIds(articleIds).stream()
            .filter(a -> a.getCurrentVersionId() != null)
            .collect(Collectors.toMap(SdBlogArticle::getArticleId, SdBlogArticle::getCurrentVersionId, (a, b) -> a));
        if (versionIdByArticle.isEmpty()) {
            return;
        }
        Map<Long, String> titleByVersion = sdBlogArticleVersionMapper.selectByIds(versionIdByArticle.values()).stream()
            .filter(v -> StringUtils.isNotBlank(v.getTitle()))
            .collect(Collectors.toMap(SdBlogArticleVersion::getVersionId, SdBlogArticleVersion::getTitle, (a, b) -> a));
        list.forEach(vo -> vo.setArticleTitle(titleByVersion.get(versionIdByArticle.get(vo.getArticleId()))));
    }

    /**
     * 管理员删除评论（契约 §12.2）：逻辑删除，同态幂等（已删/不存在均报“评论不存在”，
     * 因 @TableLogic 过滤后不可见，不做幂等伪成功以简化治理语义）
     */
    @Override
    public Boolean adminDelete(Long commentId) {
        SdBlogComment comment = sdBlogCommentMapper.selectById(commentId);
        if (Objects.isNull(comment)) {
            throw new ServiceException("评论不存在");
        }
        return sdBlogCommentMapper.deleteById(commentId) > 0;
    }

    private BlogCommentListItemVo toListItem(SdBlogComment comment) {
        BlogCommentListItemVo vo = new BlogCommentListItemVo();
        vo.setCommentId(comment.getCommentId());
        vo.setArticleId(comment.getArticleId());
        vo.setContent(comment.getContent());
        vo.setUserId(comment.getUserId());
        vo.setCreateTime(comment.getCreateTime());
        return vo;
    }

}
