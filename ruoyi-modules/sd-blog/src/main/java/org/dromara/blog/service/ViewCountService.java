package org.dromara.blog.service;

/**
 * 阅读量异步计数服务（ROADMAP S15 / D35）。
 * <p>
 * DB（sd_blog_article.view_count）仍是唯一真相源；Redis 只承担两层：
 * 去重（同人 30min 窗口）与待增量缓冲（hash）。详情读返回 DB 值 + 待增量；
 * 待增量由定时任务以「增量 UPDATE」合回 DB，增量合并不覆盖、不回退。
 *
 * @author Xu
 * @date 2026-10-14
 */
public interface ViewCountService {

    /**
     * 计数并返回本次展示阅读量。
     * <p>
     * 分支：① 可识别访客且 Redis 正常 → Lua 原子「窗口去重 + 待增量 +1」，返回 {@code dbViewCount + 待增量}；
     * ② Redis 异常 → 降级直写 DB +1（S8 行为，无窗口去重），返回 {@code dbViewCount + 1}；
     * ③ 无访客标识（未登录且无 visitorId）→ 直写 DB +1（无去重，与 S8 一致）。
     *
     * @param articleId   文章ID（调用方已完成可见性校验）
     * @param userId      登录用户ID（未登录传 null）
     * @param visitorId   匿名访客标识（缺失传 null）
     * @param dbViewCount 调用方已读到的库内 view_count（本方法不再回查 DB，省一次 SELECT）
     * @return 展示阅读量（含本次访问）
     */
    long countAndGetViewCount(Long articleId, Long userId, String visitorId, Long dbViewCount);

    /**
     * 定时批量回写待增量（{@code @Scheduled} 每 10s 触发）。
     * <p>
     * Lua 脚本内原子 drain（hgetall + del，避免先读后删丢增量）→ 逐条增量 UPDATE
     * （{@code del_flag='0'} 守卫防软删文章回写）。逐条独立提交：单条 DB 失败仅丢该 delta
     * 并记 error，不阻断同批其余文章（D35 接受计数场景的丢失取舍）。
     */
    void flushPendingViewCounts();

}
