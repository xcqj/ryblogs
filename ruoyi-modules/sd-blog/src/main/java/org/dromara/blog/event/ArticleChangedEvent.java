package org.dromara.blog.event;

import org.springframework.context.ApplicationEvent;

import java.io.Serial;

/**
 * 文章变更事件（ROADMAP S16）：文章主表写操作成功后发布，驱动 ES 索引同步。
 * <p>
 * 监听方（{@link ArticleIndexListener}）在事务提交后（AFTER_COMMIT）重新从 DB 推导
 * 文章当前状态并决定「入索引 / 删文档」——事件只作为触发信号，不携带业务状态，
 * 因此重复消费、乱序消费均安全（幂等）；action 仅用于日志定位。
 *
 * @author Xu
 * @date 2026-10-14
 */
public class ArticleChangedEvent extends ApplicationEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 变更动作（仅日志用途；索引决策由监听方按 DB 现状重推导）
     */
    public enum Action {
        CREATE, UPDATE, PUBLISH, HIDE, SHOW, DELETE, ADMIN_HIDE, ADMIN_RESTORE
    }

    private final Long articleId;

    private final Action action;

    public ArticleChangedEvent(Object source, Long articleId, Action action) {
        super(source);
        this.articleId = articleId;
        this.action = action;
    }

    public Long getArticleId() {
        return articleId;
    }

    public Action getAction() {
        return action;
    }

}
