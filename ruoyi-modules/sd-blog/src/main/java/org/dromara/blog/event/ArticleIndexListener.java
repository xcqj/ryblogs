package org.dromara.blog.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.blog.service.ArticleIndexService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 文章变更 → ES 索引同步监听（ROADMAP S16）。
 * <p>
 * AFTER_COMMIT：文章写事务提交后才动索引，事务回滚不会污染索引（软删/隐藏同理）。
 * {@code fallbackExecution=true}：兼容无 @Transactional 的写方法（hide/show/delete/adminHide/
 * adminRestore 为 MP 单条自动提交）——无活动事务时立即执行，否则事件将被丢弃。
 * <p>
 * 索引失败不抛回监听器（AFTER_COMMIT 阶段异常由 Spring 记录、不回传业务调用），
 * ArticleIndexService 内部亦已 catch 并记 error，由对账定时任务兜底修复（最终一致）。
 *
 * @author Xu
 * @date 2026-10-14
 */
@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnProperty(prefix = "blog.search", name = "engine", havingValue = "es")
public class ArticleIndexListener {

    private final ArticleIndexService articleIndexService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onArticleChanged(ArticleChangedEvent event) {
        log.debug("[ES] 文章变更事件: articleId={}, action={}", event.getArticleId(), event.getAction());
        articleIndexService.indexOrRemove(event.getArticleId());
    }

}
