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
import org.dromara.blog.domain.bo.BlogAdminCommentQueryBo;
import org.dromara.blog.domain.bo.SdBlogCommentBo;
import org.dromara.blog.domain.vo.BlogCommentListItemVo;
import org.dromara.blog.domain.vo.SdBlogCommentVo;
import org.dromara.blog.domain.SdBlogComment;
import org.dromara.blog.mapper.SdBlogArticleMapper;
import org.dromara.blog.mapper.SdBlogCommentMapper;
import org.dromara.blog.service.ISdBlogCommentService;
import org.dromara.blog.service.support.ArticleGuards;
import org.dromara.blog.util.HtmlSanitizer;

import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final SdBlogArticleMapper sdBlogArticleMapper;

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
     * <p>
     * ⚠️ 护栏：评论写入必须走 addComment（文章可见性校验 + 内容净化 + 归属固定），
     * 裸 insert 可伪造 user_id/article_id/parent_id，违反 AGENTS.md §5 红线。
     *
     * @param bo 博客评论
     * @return 永远抛出异常
     */
    @Override
    public Boolean insertByBo(SdBlogCommentBo bo) {
        throw new ServiceException("请通过发表评论接口操作");
    }

    /**
     * 修改博客评论
     * <p>
     * ⚠️ 护栏：契约 §10.2——MVP 不允许修改评论，评论一经发出不可变。
     *
     * @param bo 博客评论
     * @return 永远抛出异常
     */
    @Override
    public Boolean updateByBo(SdBlogCommentBo bo) {
        throw new ServiceException("评论不允许修改");
    }


    /**
     * 校验并批量删除博客评论信息
     * <p>
     * ⚠️ 护栏：契约 §10.2/§12.2——仅管理员单条逻辑删（adminDelete），无批量/用户自删接口。
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 永远抛出异常
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        throw new ServiceException("评论删除请通过管理端专用接口逐条操作");
    }

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
        return PageResult.build(page.getRecords().stream().map(this::toListItem).toList(), page.getTotal());
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
