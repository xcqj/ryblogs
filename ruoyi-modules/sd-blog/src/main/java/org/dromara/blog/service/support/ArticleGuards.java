package org.dromara.blog.service.support;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.blog.domain.SdBlogArticle;
import org.dromara.blog.enums.BlogArticleStatus;
import org.dromara.blog.enums.BlogVisibility;
import org.dromara.common.core.exception.ServiceException;

import java.util.Objects;

/**
 * 文章状态与权限守卫（纯逻辑，契约 §3 / §14，AGENTS.md §5 红线）
 * <p>
 * 规则集中于此便于单元测试矩阵覆盖：
 * <ul>
 * <li>查看（我的详情/版本）：作者本人或管理员；</li>
 * <li>编辑/发布：仅作者本人，管理员不可代编辑、代发布（MVP 决议）；</li>
 * <li>ADMIN_HIDDEN 文章作者不可编辑、不可自行发布恢复；</li>
 * <li>公共可见 = status=PUBLISHED 且 visibility=PUBLIC（逻辑删除由 @TableLogic 兜底）。</li>
 * </ul>
 *
 * @author Xu
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ArticleGuards {

    /**
     * 校验文章存在
     *
     * @param article 文章
     */
    public static void requireExists(SdBlogArticle article) {
        if (Objects.isNull(article)) {
            throw new ServiceException("文章不存在");
        }
    }

    /**
     * 校验查看权限：作者本人或管理员
     *
     * @param article 文章
     * @param userId  当前登录用户ID
     * @param admin   是否管理员
     */
    public static void requireViewable(SdBlogArticle article, Long userId, boolean admin) {
        requireExists(article);
        if (!admin && !isAuthor(article, userId)) {
            throw new ServiceException("无权查看该文章");
        }
    }

    /**
     * 校验编辑权限：仅作者本人，且未被管理员下线
     *
     * @param article 文章
     * @param userId  当前登录用户ID
     */
    public static void requireEditable(SdBlogArticle article, Long userId) {
        requireExists(article);
        if (!isAuthor(article, userId)) {
            throw new ServiceException("只能编辑自己的文章");
        }
        if (BlogVisibility.ADMIN_HIDDEN.getCode().equals(article.getVisibility())) {
            throw new ServiceException("文章已被管理员下线，无法编辑");
        }
    }

    /**
     * 校验发布前置条件：仅 DRAFT 可发布，且必须已有生效版本（USER_HIDDEN/ADMIN_HIDDEN 不得借发布重新公开）
     *
     * @param article 文章
     * @param userId  当前登录用户ID
     */
    public static void requirePublishable(SdBlogArticle article, Long userId) {
        requireEditable(article, userId);
        if (!BlogArticleStatus.DRAFT.getCode().equals(article.getStatus())) {
            throw new ServiceException("仅草稿状态的文章可以发布");
        }
        if (Objects.isNull(article.getCurrentVersionId())) {
            throw new ServiceException("文章没有有效版本，无法发布");
        }
    }

    /**
     * 是否作者本人
     *
     * @param article 文章
     * @param userId  用户ID
     * @return true 是作者
     */
    public static boolean isAuthor(SdBlogArticle article, Long userId) {
        return Objects.nonNull(userId) && Objects.equals(article.getAuthorId(), userId);
    }

    /**
     * 是否公共可见（契约 §1.3 三条件之一、二；del_flag 由逻辑删除保证）
     *
     * @param article 文章
     * @return true 可进入公共信息流
     */
    public static boolean isPublicVisible(SdBlogArticle article) {
        return BlogArticleStatus.PUBLISHED.getCode().equals(article.getStatus())
            && BlogVisibility.PUBLIC.getCode().equals(article.getVisibility());
    }
}
