package org.dromara.blog.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import co.elastic.clients.elasticsearch.indices.update_aliases.Action;
import co.elastic.clients.util.ObjectBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.blog.config.BlogEsConfig;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 文章索引生命周期管理（ROADMAP S16）：建索引 / mapping / alias / 零停机重建。
 * <p>
 * alias 方案：写入与查询一律走 {@link BlogEsConfig#INDEX_ALIAS}；重建时建
 * {@code blog_article_vN+1} → reindex → 单个 update-aliases 请求原子切换，读写无停机窗口。
 * <p>
 * mapping 要点：title 用 ik_max_word（索引侧细粒度最大化召回），查询侧统一 ik_smart；
 * summary/content 用 ik_smart；tags/articleId keyword；publishedAt date。
 * 相关度权重在查询侧 multi_match 的 title^3 实现（演示可调），mapping 不写死 boost。
 *
 * @author Xu
 * @date 2026-10-14
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ArticleIndexManager {

    /** 字段名常量：mapping / 排序 / highlight / 回表共用同一份定义 */
    public static final String FIELD_ARTICLE_ID = "articleId";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_SUMMARY = "summary";
    public static final String FIELD_CONTENT = "content";
    public static final String FIELD_TAGS = "tags";
    public static final String FIELD_PUBLISHED_AT = "publishedAt";

    /** D36 服务端高亮字段集（title/summary/content） */
    public static final Set<String> HIGHLIGHT_FIELDS = Set.of(FIELD_TITLE, FIELD_SUMMARY, FIELD_CONTENT);

    private static final String ANALYZER_IK_MAX_WORD = "ik_max_word";
    private static final String ANALYZER_IK_SMART = "ik_smart";

    private final ElasticsearchClient blogElasticsearchClient;

    /**
     * 确保 alias 可用：不存在则建 v1 并挂 alias（初始全量同步由调用方接着做）。
     *
     * @return true = 本次新建了索引与 alias（调用方需要做初始全量同步）
     */
    public boolean ensureAliasReady() throws IOException {
        if (aliasExists()) {
            return false;
        }
        String index = nextIndexName();
        createIndexWithMapping(index);
        putAlias(index);
        log.info("[ES] alias {} 不存在，已新建索引 {} 并挂载 alias", BlogEsConfig.INDEX_ALIAS, index);
        return true;
    }

    /**
     * alias 当前指向的索引名列表（正常单节点为 1 个；空 = alias 不存在）
     */
    public List<String> aliasIndexNames() throws IOException {
        if (!aliasExists()) {
            return List.of();
        }
        GetIndexResponse resp = blogElasticsearchClient.indices()
            .get(g -> g.index(BlogEsConfig.INDEX_ALIAS));
        return new ArrayList<>(resp.result().keySet());
    }

    /**
     * 建版本化索引（不含 alias）：{@code blog_article_vN} + mapping + settings
     */
    public void createIndexWithMapping(String indexName) throws IOException {
        CreateIndexResponse resp = blogElasticsearchClient.indices().create(c -> c
            .index(indexName)
            .settings(s -> s
                .numberOfShards("1")
                .numberOfReplicas("0")
                .maxResultWindow(BlogEsConfig.MAX_RESULT_WINDOW))
            .mappings(ArticleIndexManager::buildMapping));
        log.info("[ES] 创建索引 {} 完成，acknowledged={}", indexName, resp.acknowledged());
    }

    /**
     * 把文档写入 alias（upsert 语义，_id = articleId）；refreshImmediate 时写入即可查
     */
    public void indexDoc(BlogArticleDoc doc, boolean refreshImmediate) throws IOException {
        blogElasticsearchClient.index(i -> i
            .index(BlogEsConfig.INDEX_ALIAS)
            .id(String.valueOf(doc.getArticleId()))
            .document(doc)
            .refresh(refreshImmediate ? Refresh.True : Refresh.False));
    }

    /**
     * 从 alias 删除文档（不存在时 ES 返回 result=not_found，不抛错）
     */
    public void deleteDoc(Long articleId, boolean refreshImmediate) throws IOException {
        blogElasticsearchClient.delete(d -> d
            .index(BlogEsConfig.INDEX_ALIAS)
            .id(String.valueOf(articleId))
            .refresh(refreshImmediate ? Refresh.True : Refresh.False));
    }

    /**
     * 删除整个索引（重建演示收尾用）
     */
    public void deleteIndex(String indexName) throws IOException {
        blogElasticsearchClient.indices().delete(d -> d.index(indexName));
    }

    /**
     * 零停机重建第二步：把 fromIndex 全量 reindex 到 toIndex（阻塞至完成）
     */
    public void reindex(String fromIndex, String toIndex) throws IOException {
        blogElasticsearchClient.reindex(r -> r
            .source(s -> s.index(fromIndex))
            .dest(d2 -> d2.index(toIndex))
            .waitForCompletion(true));
    }

    /**
     * 零停机重建第三步：单个 update-aliases 请求内「移除旧指向 + 添加新指向」，ES 保证原子切换
     * （Action.Builder 的 add/remove 各自返回单动作 ObjectBuilder，不可链式，故拆两条）
     */
    public void switchAliasAtomically(String fromIndex, String toIndex) throws IOException {
        Action removeOld = new Action.Builder()
            .remove(rm -> rm.index(fromIndex).alias(BlogEsConfig.INDEX_ALIAS)).build();
        Action addNew = new Action.Builder()
            .add(ad -> ad.index(toIndex).alias(BlogEsConfig.INDEX_ALIAS)).build();
        blogElasticsearchClient.indices().updateAliases(u -> u.actions(List.of(removeOld, addNew)));
    }

    /**
     * 当前已有版本化索引的最大版本号 +1（无则 1）
     */
    public int nextVersion() throws IOException {
        GetIndexResponse resp = blogElasticsearchClient.indices()
            .get(g -> g.index(BlogEsConfig.INDEX_NAME_PREFIX + "*"));
        int max = 0;
        for (String name : resp.result().keySet()) {
            max = Math.max(max, parseVersion(name));
        }
        return max + 1;
    }

    /**
     * {@code blog_article_v{N+1}}
     */
    public String nextIndexName() throws IOException {
        return BlogEsConfig.INDEX_NAME_PREFIX + nextVersion();
    }

    /**
     * 所有版本化索引名（版本号升序）
     */
    public List<String> versionedIndexNames() throws IOException {
        GetIndexResponse resp = blogElasticsearchClient.indices()
            .get(g -> g.index(BlogEsConfig.INDEX_NAME_PREFIX + "*"));
        List<String> names = new ArrayList<>(resp.result().keySet());
        names.sort(Comparator.comparingInt(this::parseVersion));
        return names;
    }

    private int parseVersion(String indexName) {
        try {
            return Integer.parseInt(indexName.substring(BlogEsConfig.INDEX_NAME_PREFIX.length()));
        } catch (NumberFormatException e) {
            // 非版本化命名（历史遗留/手工创建）不参与版本推算
            return 0;
        }
    }

    private boolean aliasExists() throws IOException {
        return blogElasticsearchClient.indices()
            .existsAlias(e -> e.name(BlogEsConfig.INDEX_ALIAS)).value();
    }

    private void putAlias(String indexName) throws IOException {
        blogElasticsearchClient.indices().putAlias(p -> p.index(indexName).name(BlogEsConfig.INDEX_ALIAS));
    }

    private static ObjectBuilder<TypeMapping> buildMapping(TypeMapping.Builder m) {
        m.properties(FIELD_ARTICLE_ID, p -> p.keyword(k -> k));
        m.properties(FIELD_TITLE, p -> p.text(t -> t
            .analyzer(ANALYZER_IK_MAX_WORD)
            .searchAnalyzer(ANALYZER_IK_SMART)));
        m.properties(FIELD_SUMMARY, p -> p.text(t -> t
            .analyzer(ANALYZER_IK_SMART)
            .searchAnalyzer(ANALYZER_IK_SMART)));
        m.properties(FIELD_CONTENT, p -> p.text(t -> t
            .analyzer(ANALYZER_IK_SMART)
            .searchAnalyzer(ANALYZER_IK_SMART)));
        m.properties(FIELD_TAGS, p -> p.keyword(k -> k));
        m.properties(FIELD_PUBLISHED_AT, p -> p.date(d -> d));
        return m;
    }

}
