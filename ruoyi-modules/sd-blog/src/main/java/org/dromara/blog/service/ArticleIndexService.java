package org.dromara.blog.service;

/**
 * 文章 ES 索引同步服务（ROADMAP S16）。
 * <p>
 * 红线（AGENTS.md §9）：索引只收录公共可见文章（status=1 AND visibility=0 AND del_flag=0），
 * 三条件在每个入口（增量同步 / 初始全量 / 对账的 MySQL 侧查询）同源强制，查询端不再过滤。
 *
 * @author Xu
 * @date 2026-10-14
 */
public interface ArticleIndexService {

    /**
     * 增量同步单篇文章：按 DB 现状重推导——公共可见则 upsert 文档，否则删除文档。
     * <p>
     * 幂等：重复调用/事件重复消费结果一致；失败仅记日志不抛出（对账任务兜底）。
     *
     * @param articleId 文章ID
     */
    void indexOrRemove(Long articleId);

    /**
     * 初始全量同步：索引 alias 新建后把存量公共文章灌入（keyset 分页逐批，内存可控）。
     */
    void initialFullSync();

    /**
     * 对账：MySQL 公共文章集合 vs ES 文档集合 diff，补索引 / 删多余文档（定时任务调用）。
     */
    void reconcile();

}
