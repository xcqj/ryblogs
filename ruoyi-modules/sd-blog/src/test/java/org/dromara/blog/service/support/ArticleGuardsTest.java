package org.dromara.blog.service.support;

import org.dromara.blog.domain.SdBlogArticle;
import org.dromara.blog.enums.BlogArticleStatus;
import org.dromara.blog.enums.BlogVisibility;
import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文章状态与权限守卫单元测试（D7 策略②，状态×动作×角色矩阵）
 *
 * @author Xu
 */
@DisplayName("ArticleGuards 状态机守卫")
class ArticleGuardsTest {

    private static final Long AUTHOR = 100L;
    private static final Long OTHER = 200L;

    private SdBlogArticle article(BlogArticleStatus status, BlogVisibility visibility, Long versionId) {
        SdBlogArticle a = new SdBlogArticle();
        a.setAuthorId(AUTHOR);
        a.setStatus(status.getCode());
        a.setVisibility(visibility.getCode());
        a.setCurrentVersionId(versionId);
        return a;
    }

    @Test
    @DisplayName("null 文章一律视为不存在")
    void shouldRejectNullArticle() {
        assertThrows(ServiceException.class, () -> ArticleGuards.requireExists(null));
        assertThrows(ServiceException.class, () -> ArticleGuards.requireViewable(null, AUTHOR, false));
    }

    @Test
    @DisplayName("查看：作者或管理员放行，他人拒绝")
    void shouldControlView() {
        SdBlogArticle a = article(BlogArticleStatus.DRAFT, BlogVisibility.PUBLIC, 1L);
        assertDoesNotThrow(() -> ArticleGuards.requireViewable(a, AUTHOR, false));
        assertDoesNotThrow(() -> ArticleGuards.requireViewable(a, OTHER, true));
        assertThrows(ServiceException.class, () -> ArticleGuards.requireViewable(a, OTHER, false));
    }

    @Test
    @DisplayName("编辑：仅作者；管理员也不可编辑；ADMIN_HIDDEN 作者不可编辑")
    void shouldControlEdit() {
        SdBlogArticle draft = article(BlogArticleStatus.DRAFT, BlogVisibility.PUBLIC, 1L);
        assertDoesNotThrow(() -> ArticleGuards.requireEditable(draft, AUTHOR));
        assertThrows(ServiceException.class, () -> ArticleGuards.requireEditable(draft, OTHER));

        SdBlogArticle adminHidden = article(BlogArticleStatus.PUBLISHED, BlogVisibility.ADMIN_HIDDEN, 1L);
        assertThrows(ServiceException.class, () -> ArticleGuards.requireEditable(adminHidden, AUTHOR));
    }

    @Test
    @DisplayName("发布：仅 DRAFT 且有生效版本且 PUBLIC；已发布/无版本/USER_HIDDEN 均拒绝")
    void shouldControlPublish() {
        assertDoesNotThrow(() -> ArticleGuards.requirePublishable(
            article(BlogArticleStatus.DRAFT, BlogVisibility.PUBLIC, 9L), AUTHOR));
        assertThrows(ServiceException.class, () -> ArticleGuards.requirePublishable(
            article(BlogArticleStatus.DRAFT, BlogVisibility.PUBLIC, null), AUTHOR));
        assertThrows(ServiceException.class, () -> ArticleGuards.requirePublishable(
            article(BlogArticleStatus.PUBLISHED, BlogVisibility.USER_HIDDEN, 9L), AUTHOR));
        // S5 补全：DRAFT+USER_HIDDEN 不得借 publish 把状态机拧成"已发布且隐藏"，必须先 show
        assertThrows(ServiceException.class, () -> ArticleGuards.requirePublishable(
            article(BlogArticleStatus.DRAFT, BlogVisibility.USER_HIDDEN, 9L), AUTHOR));
    }

    @Test
    @DisplayName("作者隐藏/重新公开：仅已发布且非 ADMIN_HIDDEN；草稿与非作者拒绝（S5 决议）")
    void shouldControlUserToggleVisible() {
        // 已发布 + PUBLIC/USER_HIDDEN 均可操作（具体幂等在 Service 层）
        assertDoesNotThrow(() -> ArticleGuards.requireUserToggleVisible(
            article(BlogArticleStatus.PUBLISHED, BlogVisibility.PUBLIC, 1L), AUTHOR));
        assertDoesNotThrow(() -> ArticleGuards.requireUserToggleVisible(
            article(BlogArticleStatus.PUBLISHED, BlogVisibility.USER_HIDDEN, 1L), AUTHOR));
        // 草稿无需隐藏；管理员下线不可自行调整；非作者拒绝
        assertThrows(ServiceException.class, () -> ArticleGuards.requireUserToggleVisible(
            article(BlogArticleStatus.DRAFT, BlogVisibility.PUBLIC, 1L), AUTHOR));
        assertThrows(ServiceException.class, () -> ArticleGuards.requireUserToggleVisible(
            article(BlogArticleStatus.PUBLISHED, BlogVisibility.ADMIN_HIDDEN, 1L), AUTHOR));
        assertThrows(ServiceException.class, () -> ArticleGuards.requireUserToggleVisible(
            article(BlogArticleStatus.PUBLISHED, BlogVisibility.PUBLIC, 1L), OTHER));
    }

    @Test
    @DisplayName("作者删除：四种状态均可，仅作者本人")
    void shouldControlDelete() {
        for (BlogVisibility v : BlogVisibility.values()) {
            assertDoesNotThrow(() -> ArticleGuards.requireDeletable(
                article(BlogArticleStatus.PUBLISHED, v, 1L), AUTHOR));
        }
        assertDoesNotThrow(() -> ArticleGuards.requireDeletable(
            article(BlogArticleStatus.DRAFT, BlogVisibility.PUBLIC, 1L), AUTHOR));
        assertThrows(ServiceException.class, () -> ArticleGuards.requireDeletable(
            article(BlogArticleStatus.PUBLISHED, BlogVisibility.PUBLIC, 1L), OTHER));
    }

    @Test
    @DisplayName("管理员下线：仅 PUBLISHED+PUBLIC；恢复：仅 ADMIN_HIDDEN（S5 决议）")
    void shouldControlAdminActions() {
        assertDoesNotThrow(() -> ArticleGuards.requireAdminHidable(
            article(BlogArticleStatus.PUBLISHED, BlogVisibility.PUBLIC, 1L)));
        assertThrows(ServiceException.class, () -> ArticleGuards.requireAdminHidable(
            article(BlogArticleStatus.DRAFT, BlogVisibility.PUBLIC, 1L)));
        assertThrows(ServiceException.class, () -> ArticleGuards.requireAdminHidable(
            article(BlogArticleStatus.PUBLISHED, BlogVisibility.USER_HIDDEN, 1L)));

        assertDoesNotThrow(() -> ArticleGuards.requireAdminRestorable(
            article(BlogArticleStatus.PUBLISHED, BlogVisibility.ADMIN_HIDDEN, 1L)));
        assertThrows(ServiceException.class, () -> ArticleGuards.requireAdminRestorable(
            article(BlogArticleStatus.PUBLISHED, BlogVisibility.PUBLIC, 1L)));
        assertThrows(ServiceException.class, () -> ArticleGuards.requireAdminRestorable(
            article(BlogArticleStatus.PUBLISHED, BlogVisibility.USER_HIDDEN, 1L)));
    }

    @Test
    @DisplayName("公共可见判定：PUBLISHED+PUBLIC 才可见")
    void shouldJudgePublicVisibility() {
        assertTrue(ArticleGuards.isPublicVisible(article(BlogArticleStatus.PUBLISHED, BlogVisibility.PUBLIC, 1L)));
        assertFalse(ArticleGuards.isPublicVisible(article(BlogArticleStatus.DRAFT, BlogVisibility.PUBLIC, 1L)));
        assertFalse(ArticleGuards.isPublicVisible(article(BlogArticleStatus.PUBLISHED, BlogVisibility.USER_HIDDEN, 1L)));
        assertFalse(ArticleGuards.isPublicVisible(article(BlogArticleStatus.PUBLISHED, BlogVisibility.ADMIN_HIDDEN, 1L)));
    }
}
