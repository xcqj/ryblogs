package org.dromara.blog.es;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.blog.service.ArticleIndexService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * ES 索引启动引导（ROADMAP S16）：应用启动后确保 alias 就绪，新建则做初始全量同步。
 * <p>
 * ES 未启动/不可达时**不阻断应用启动**（搜索按降级路径走 MySQL LIKE），只记 error；
 * 后续对账定时任务会在 ES 恢复后补同步（alias 缺失时对账拉取失败跳过，需人工重建或重启应用）。
 *
 * @author Xu
 * @date 2026-10-14
 */
@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnProperty(prefix = "blog.search", name = "engine", havingValue = "es")
public class ArticleIndexBootstrap implements ApplicationRunner {

    private final ArticleIndexManager articleIndexManager;
    private final ArticleIndexService articleIndexService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (articleIndexManager.ensureAliasReady()) {
                articleIndexService.initialFullSync();
            }
        } catch (Exception e) {
            log.error("[ES] 索引初始化失败（不影响应用启动；ES 恢复后重启应用或手动重建索引即可）", e);
        }
    }

}
