package org.dromara.blog.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/**
 * 博客 ES 客户端配置（ROADMAP S16）。
 * <p>
 * 客户端选型：sd-blog 传递依赖已锁定 {@code co.elastic.clients:elasticsearch-java:7.17.28}
 * （经 ruoyi-common-elasticsearch → easy-es 3.0.2），与 ES 服务端 7.17.28 精确匹配。
 * <b>不走 easy-es 的 BaseEsMapper 自动索引生命周期</b>（ROADMAP §6 S16-D2 登记）：
 * IK 分词 mapping / boost / alias / 零停机 reindex / highlight / search_after 需要精确 DSL 控制，
 * easy-es 启动期自动建索引会与 alias 方案冲突；{@code easy-es.enable} 保持 false 与本客户端并存互不激活。
 * <p>
 * bean 创建不发连接（懒连接），ES 未启动时应用仍可正常启动——搜索按配置降级 MySQL LIKE。
 *
 * @author Xu
 * @date 2026-10-14
 */
@Configuration
public class BlogEsConfig {

    /**
     * 索引别名：写入与查询都走 alias，零停机重建时原子切换指向（建 v2 → reindex → 切 alias）
     */
    public static final String INDEX_ALIAS = "blog_article";

    /**
     * 版本化索引名前缀：{@code blog_article_v1} / {@code blog_article_v2} ...
     */
    public static final String INDEX_NAME_PREFIX = "blog_article_v";

    /**
     * 深翻页窗口（{@code index.max_result_window}）：from+size 上限，覆盖演示数据量（5 万篇）；
     * 超过此窗口必须走可选 {@code after} 游标（契约 §6 深翻页条款）
     */
    public static final int MAX_RESULT_WINDOW = 50000;

    @Value("${blog.es.address:localhost:9200}")
    private String address;

    @Bean(destroyMethod = "close")
    public ElasticsearchClient blogElasticsearchClient() {
        RestClient restClient = RestClient.builder(HttpHost.create(address))
            // 显式超时防挂死：低层 RestClient 默认 socketTimeout=30s——ES 抖动时搜索请求会
            // 挂满 30s 才进降级，体验极差；收敛到 10s（批量写大包走同一配置，5 万级仍绰绰有余）
            .setRequestConfigCallback(rc -> rc
                .setConnectTimeout(5_000)
                .setSocketTimeout(10_000))
            .build();
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }

}
