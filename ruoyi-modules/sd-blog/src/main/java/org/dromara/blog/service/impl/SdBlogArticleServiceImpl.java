package org.dromara.blog.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
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
import org.dromara.blog.domain.bo.BlogAdminArticleQueryBo;
import org.dromara.blog.domain.bo.BlogPublicArticleQueryBo;
import org.dromara.blog.domain.vo.BlogArticleDetailVo;
import org.dromara.blog.domain.vo.BlogArticleListItemVo;
import org.dromara.blog.domain.vo.BlogAdminArticleListItemVo;
import org.dromara.blog.domain.vo.BlogPublicArticleDetailVo;
import org.dromara.blog.domain.vo.BlogPublicArticleListItemVo;
import org.dromara.blog.domain.vo.SdBlogArticleVersionVo;
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
import org.dromara.blog.service.ViewCountService;
import org.dromara.blog.service.support.ArticleGuards;
import org.dromara.blog.service.support.ArticleTagFiller;
import org.dromara.blog.service.support.VersionPolicy;
import org.dromara.blog.util.HtmlSanitizer;

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
    private final ArticleTagFiller articleTagFiller;
    private final ViewCountService viewCountService;

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
        sanitizeIncoming(bo);
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
     * 保存文章：内容实际变化才创建新版本并立即生效；标签始终同步（契约 §3.4）。
     * 并发策略（D11）：事务内先 FOR UPDATE 锁定文章行，同一文章的并发保存串行排队，
     * 后一个事务拿锁后重新读到最新 currentVersionId/最大版本号，消除 version_no 竞态（§18.12）；
     * 不同文章互不阻塞，MVP 单作者场景锁竞争几乎不存在。
     *
     * @param articleId 文章ID
     * @param bo        保存参数
     * @param userId    当前登录用户ID
     * @return 是否保存成功
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateArticle(Long articleId, BlogArticleSaveBo bo, Long userId) {
        sanitizeIncoming(bo);
        SdBlogArticle article = sdBlogArticleMapper.selectById(articleId);
        ArticleGuards.requireEditable(article, userId);
        // 校验通过后锁文章行，锁持有到事务提交；后续版本读取必须在拿锁之后
        article = sdBlogArticleMapper.lambda()
            .eq(SdBlogArticle::getArticleId, articleId)
            .last("FOR UPDATE")
            .one();
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
        vo.setAdminHiddenReason(article.getAdminHiddenReason());
        vo.setAdminHiddenBy(article.getAdminHiddenBy());
        vo.setAdminHiddenTime(article.getAdminHiddenTime());
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
     * 入库前净化（契约 §18.9 / D6）：正文白名单清洗、标题摘要纯文本化、封面协议校验。
     * 在内容比对之前执行；jsoup 净化幂等，不会对合法内容产生伪版本差异。
     */
    private void sanitizeIncoming(BlogArticleSaveBo bo) {
        bo.setTitle(HtmlSanitizer.sanitizePlainText(bo.getTitle()));
        bo.setSummary(HtmlSanitizer.sanitizePlainText(bo.getSummary()));
        bo.setContent(HtmlSanitizer.sanitizeHtml(bo.getContent()));
        validateCoverUrl(bo.getCoverUrl());
        // 净化后正文可能只剩危险标签（如整段 <script>），剥离后为空则拒绝入库
        if (StringUtils.isBlank(bo.getContent())) {
            throw new ServiceException("文章正文有效内容为空，请检查内容是否仅含不安全标签");
        }
    }

    /**
     * 封面地址仅允许 http/https 或站内相对路径，拒绝 javascript: 等危险协议
     */
    private void validateCoverUrl(String coverUrl) {
        if (StringUtils.isNotBlank(coverUrl)
            && !coverUrl.startsWith("/")
            && !StringUtils.startsWithAny(coverUrl, "http://", "https://")) {
            throw new ServiceException("封面地址格式不合法");
        }
    }

    /**
     * 作者隐藏文章（契约 §3.6）：PUBLIC → USER_HIDDEN，不改 status，同态幂等
     */
    @Override
    public Boolean hideArticle(Long articleId, Long userId) {
        SdBlogArticle article = sdBlogArticleMapper.selectById(articleId);
        ArticleGuards.requireUserToggleVisible(article, userId);
        if (BlogVisibility.USER_HIDDEN.getCode().equals(article.getVisibility())) {
            return true;
        }
        SdBlogArticle update = new SdBlogArticle();
        update.setArticleId(articleId);
        update.setVisibility(BlogVisibility.USER_HIDDEN.getCode());
        return sdBlogArticleMapper.updateById(update) > 0;
    }

    /**
     * 作者重新公开文章（契约 §3.7）：USER_HIDDEN → PUBLIC，同态幂等；不恢复 ADMIN_HIDDEN（守卫已拦）
     */
    @Override
    public Boolean showArticle(Long articleId, Long userId) {
        SdBlogArticle article = sdBlogArticleMapper.selectById(articleId);
        ArticleGuards.requireUserToggleVisible(article, userId);
        if (BlogVisibility.PUBLIC.getCode().equals(article.getVisibility())) {
            return true;
        }
        SdBlogArticle update = new SdBlogArticle();
        update.setArticleId(articleId);
        update.setVisibility(BlogVisibility.PUBLIC.getCode());
        return sdBlogArticleMapper.updateById(update) > 0;
    }

    /**
     * 作者删除文章（契约 §3.8）：逻辑删除 del_flag=1，版本/标签关系/评论保留，不做物理清理
     */
    @Override
    public Boolean deleteByAuthor(Long articleId, Long userId) {
        SdBlogArticle article = sdBlogArticleMapper.selectById(articleId);
        ArticleGuards.requireDeletable(article, userId);
        return sdBlogArticleMapper.deleteById(articleId) > 0;
    }

    /**
     * 管理员分页查询平台文章（契约 §11.1）：可见全部状态；title 两段式批量填充防 N+1
     */
    @Override
    public PageResult<BlogAdminArticleListItemVo> queryAdminPageList(BlogAdminArticleQueryBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SdBlogArticle> lqw = QueryBuilder.lambda(SdBlogArticle.class)
            .eqIfPresent(SdBlogArticle::getAuthorId, bo.getAuthorId())
            .eqIfText(SdBlogArticle::getStatus, bo.getStatus())
            .eqIfText(SdBlogArticle::getVisibility, bo.getVisibility())
            .orderByDesc(SdBlogArticle::getUpdateTime)
            .orderByDesc(SdBlogArticle::getArticleId)
            .build();
        // 创建时间范围（显式字符串字段，MySQL 自动转 datetime 比较；支持单边）
        if (StringUtils.isNotBlank(bo.getBeginTime())) {
            lqw.ge(SdBlogArticle::getCreateTime, bo.getBeginTime());
        }
        if (StringUtils.isNotBlank(bo.getEndTime())) {
            lqw.le(SdBlogArticle::getCreateTime, bo.getEndTime());
        }
        // 标题存于版本表：先按当前版本 title 匹配出 ID 集合再 in（与 §18.11 两段式同源，避免改 XML 联表）
        if (StringUtils.isNotBlank(bo.getTitle())) {
            List<Long> articleIds = sdBlogArticleVersionMapper.lambda()
                .select(SdBlogArticleVersion::getArticleId)
                .like(SdBlogArticleVersion::getTitle, bo.getTitle())
                .list().stream().map(SdBlogArticleVersion::getArticleId).distinct().toList();
            if (articleIds.isEmpty()) {
                return PageResult.build(List.of(), 0);
            }
            lqw.in(SdBlogArticle::getArticleId, articleIds);
        }
        Page<SdBlogArticle> page = sdBlogArticleMapper.selectPage(pageQuery.build(), lqw);
        List<BlogAdminArticleListItemVo> rows = page.getRecords().stream().map(this::toAdminListItem).toList();
        fillTitles(rows);
        return PageResult.build(rows, page.getTotal());
    }

    /**
     * 管理员下线文章（契约 §11.3）：写治理三字段，不改 status；已下线允许更新下线记录（留最新依据），同态幂等
     */
    @Override
    public Boolean adminHide(Long articleId, String reason, Long adminId) {
        SdBlogArticle article = sdBlogArticleMapper.selectById(articleId);
        ArticleGuards.requireExists(article);
        if (BlogVisibility.ADMIN_HIDDEN.getCode().equals(article.getVisibility())) {
            SdBlogArticle refresh = new SdBlogArticle();
            refresh.setArticleId(articleId);
            refresh.setAdminHiddenReason(reason);
            refresh.setAdminHiddenBy(adminId);
            refresh.setAdminHiddenTime(LocalDateTime.now());
            return sdBlogArticleMapper.updateById(refresh) > 0;
        }
        ArticleGuards.requireAdminHidable(article);
        SdBlogArticle update = new SdBlogArticle();
        update.setArticleId(articleId);
        update.setVisibility(BlogVisibility.ADMIN_HIDDEN.getCode());
        update.setAdminHiddenReason(reason);
        update.setAdminHiddenBy(adminId);
        update.setAdminHiddenTime(LocalDateTime.now());
        return sdBlogArticleMapper.updateById(update) > 0;
    }

    /**
     * 管理员恢复文章（契约 §11.4）：visibility → PUBLIC，status 保持不变即重新可见；
     * 治理字段语义为"当前下线状态"，恢复时清空（审计痕迹由 update_by/update_time 保留）
     */
    @Override
    public Boolean adminRestore(Long articleId) {
        SdBlogArticle article = sdBlogArticleMapper.selectById(articleId);
        ArticleGuards.requireExists(article);
        if (BlogVisibility.PUBLIC.getCode().equals(article.getVisibility())) {
            return true;
        }
        ArticleGuards.requireAdminRestorable(article);
        return sdBlogArticleMapper.lambdaUpdate()
            .set(SdBlogArticle::getVisibility, BlogVisibility.PUBLIC.getCode())
            .set(SdBlogArticle::getAdminHiddenReason, null)
            .set(SdBlogArticle::getAdminHiddenBy, null)
            .set(SdBlogArticle::getAdminHiddenTime, null)
            .eq(SdBlogArticle::getArticleId, articleId)
            .update();
    }

    /**
     * 公共文章分页（契约 §5.1 / §9.2 共用入口）：tagSlug 解析 canonical 后取关系集合作限定，
     * 无效 slug/无命中返回空页（列表过滤语义，不报错）；本方法不接受任何绕过三条件的参数
     */
    @Override
    public PageResult<BlogPublicArticleListItemVo> queryPublicPageList(BlogPublicArticleQueryBo bo, PageQuery pageQuery) {
        Collection<Long> restrictIds = null;
        if (StringUtils.isNotBlank(bo.getTagSlug())) {
            SdBlogTag tag = sdBlogTagService.resolveCanonicalBySlug(bo.getTagSlug().trim());
            if (Objects.isNull(tag)) {
                return PageResult.build(List.of(), 0);
            }
            restrictIds = queryArticleIdsByTag(tag.getTagId());
        }
        return doPublicPage(restrictIds, bo.getAuthorId(), pageQuery);
    }

    /**
     * 标签下公开文章（契约 §7.4）：无法解析报错（详情页语义，区别于 §5.1 过滤）
     */
    @Override
    public PageResult<BlogPublicArticleListItemVo> queryPublicTagArticles(String slug, PageQuery pageQuery) {
        SdBlogTag tag = sdBlogTagService.resolveCanonicalBySlug(slug);
        if (Objects.isNull(tag)) {
            throw new ServiceException("标签不存在");
        }
        return doPublicPage(queryArticleIdsByTag(tag.getTagId()), null, pageQuery);
    }

    /**
     * 公共文章详情（契约 §5.2）：验证可见后走阅读量异步计数，返回库内值 + 待增量（D35）
     * <p>
     * S15 / D35：读路径不再写 DB 行锁——Redis 可用时 Lua 去重 + 待增量 +1（登录 userId 优先、
     * 匿名 visitorId，30min 窗口）；Redis 异常或无访客标识时 ViewCountService 内部降级直写 DB（S8 行为）。
     */
    @Override
    public BlogPublicArticleDetailVo queryPublicDetail(Long articleId, String visitorId) {
        SdBlogArticle article = sdBlogArticleMapper.selectById(articleId);
        if (Objects.isNull(article) || !ArticleGuards.isPublicVisible(article)) {
            throw new ServiceException("文章不存在");
        }
        Long userId = LoginHelper.isLogin() ? LoginHelper.getUserId() : null;
        long viewCount = viewCountService.countAndGetViewCount(articleId, userId, visitorId, article.getViewCount());
        BlogPublicArticleDetailVo vo = new BlogPublicArticleDetailVo();
        vo.setArticleId(article.getArticleId());
        vo.setAuthorId(article.getAuthorId());
        vo.setPublishedAt(article.getPublishedAt());
        vo.setViewCount(viewCount);
        if (Objects.nonNull(article.getCurrentVersionId())) {
            vo.setCurrentVersion(toVersionVo(sdBlogArticleVersionMapper.selectById(article.getCurrentVersionId())));
        }
        vo.setTags(queryTagsByArticleIds(List.of(articleId)).getOrDefault(articleId, List.of()));
        return vo;
    }

    /**
     * 公共分页底座（三条件由 selectPublicPage 内建；限定集合为空时短路空页，空 in 会退化为全量）
     */
    private PageResult<BlogPublicArticleListItemVo> doPublicPage(Collection<Long> restrictArticleIds, Long authorId, PageQuery pageQuery) {
        if (Objects.nonNull(restrictArticleIds) && restrictArticleIds.isEmpty()) {
            return PageResult.build(List.of(), 0);
        }
        Page<BlogPublicArticleListItemVo> page = sdBlogArticleMapper.selectPublicPage(pageQuery.build(), restrictArticleIds, authorId);
        List<BlogPublicArticleListItemVo> records = page.getRecords();
        if (CollUtil.isNotEmpty(records)) {
            Map<Long, List<SdBlogTagVo>> tagMap = queryTagsByArticleIds(
                records.stream().map(BlogPublicArticleListItemVo::getArticleId).toList());
            records.forEach(item -> item.setTags(tagMap.getOrDefault(item.getArticleId(), List.of())));
        }
        return PageResult.build(records, page.getTotal());
    }

    /**
     * 取标签下的全部文章ID（关系集合，供公共分页限定；含非公共文章，由底座三条件再过滤）
     */
    private List<Long> queryArticleIdsByTag(Long tagId) {
        return sdBlogArticleTagMapper.lambda()
            .select(SdBlogArticleTag::getArticleId)
            .eq(SdBlogArticleTag::getTagId, tagId)
            .list().stream().map(SdBlogArticleTag::getArticleId).toList();
    }

    /**
     * 实体 → 管理员列表项
     */
    private BlogAdminArticleListItemVo toAdminListItem(SdBlogArticle article) {
        BlogAdminArticleListItemVo vo = new BlogAdminArticleListItemVo();
        vo.setArticleId(article.getArticleId());
        vo.setAuthorId(article.getAuthorId());
        vo.setStatus(article.getStatus());
        vo.setVisibility(article.getVisibility());
        vo.setViewCount(article.getViewCount());
        vo.setPublishedAt(article.getPublishedAt());
        vo.setCreateTime(article.getCreateTime());
        vo.setUpdateTime(article.getUpdateTime());
        vo.setAdminHiddenReason(article.getAdminHiddenReason());
        vo.setAdminHiddenBy(article.getAdminHiddenBy());
        vo.setAdminHiddenTime(article.getAdminHiddenTime());
        return vo;
    }

    /**
     * 批量填充当前版本标题（两段式，杜绝 N+1）
     */
    private void fillTitles(List<BlogAdminArticleListItemVo> rows) {
        if (CollUtil.isEmpty(rows)) {
            return;
        }
        List<Long> articleIds = rows.stream().map(BlogAdminArticleListItemVo::getArticleId).toList();
        Map<Long, Long> articleVersionMap = new HashMap<>();
        for (SdBlogArticle a : sdBlogArticleMapper.lambda()
            .select(SdBlogArticle::getArticleId, SdBlogArticle::getCurrentVersionId)
            .in(SdBlogArticle::getArticleId, articleIds).list()) {
            if (Objects.nonNull(a.getCurrentVersionId())) {
                articleVersionMap.put(a.getArticleId(), a.getCurrentVersionId());
            }
        }
        if (articleVersionMap.isEmpty()) {
            return;
        }
        Map<Long, String> titleMap = new HashMap<>();
        for (SdBlogArticleVersion v : sdBlogArticleVersionMapper.lambda()
            .select(SdBlogArticleVersion::getVersionId, SdBlogArticleVersion::getTitle)
            .in(SdBlogArticleVersion::getVersionId, articleVersionMap.values()).list()) {
            titleMap.put(v.getVersionId(), v.getTitle());
        }
        rows.forEach(row -> {
            Long vid = articleVersionMap.get(row.getArticleId());
            row.setTitle(vid == null ? null : titleMap.get(vid));
        });
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
     * 按文章ID集合批量查询标签，分组返回（委托 ArticleTagFiller，与搜索链路同源）
     *
     * @param articleIds 文章ID集合
     * @return 文章ID → 标签VO列表
     */
    private Map<Long, List<SdBlogTagVo>> queryTagsByArticleIds(List<Long> articleIds) {
        return articleTagFiller.queryTagsByArticleIds(articleIds);
    }

}
