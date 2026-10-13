# S16 LIKE vs ES 检索延迟对比（面试材料留档）

> 状态：方法与命令就绪，**结果表留人工实测后填写**（需 ES 容器在跑 + 演示数据已灌入 + 对账已追平）。
> 数据：`blog_s16_demo_data.sql`（2 万篇压测 + 24 篇精选）；清理：`blog_s16_cleanup.sql`。

## 1. 测试方法

- 数据量：MySQL `sd_blog_article` 公共可见 ≈ 20024 篇演示 + 存量；ES alias `blog_article` 与之等量（对账追平后）。
- 对比查询词（每组约 4000 篇命中）：`Redis 缓存` / `MySQL 索引` / `Docker 容器` / `Spring Boot 配置` / `微服务 网关`；另测低频词（如 `Python爬虫`）与空结果词（如 `量子纠缠`）。
- 度量：`curl -w` 取 `time_total`（端到端含网络与序列化），每组词连打 10 次取中位数，剔除首次冷启动值；ES 引擎与 mysql 引擎各跑一轮（`blog.search.engine` 切换后重启后端）。
- 接口：`GET http://localhost:8081/blog/public/search?keyword=<词>&pageNum=1&pageSize=10`（匿名，无 token）。

## 2. 执行命令

```bash
# ES 引擎（engine=es，默认）
for i in $(seq 1 11); do curl -s -o /dev/null -w '%{time_total}\n' \
  'http://localhost:8081/blog/public/search?keyword=Redis%20%E7%BC%93%E5%AD%98&pageNum=1&pageSize=10'; done

# MySQL 引擎（application.yml: blog.search.engine=mysql，重启后端后同法）

# ES 原生耗时参考（剥离 Java 回表成本）
curl -s -o /dev/null -w '%{time_total}\n' \
  -X POST 'localhost:9200/blog_article/_search' -H 'Content-Type: application/json' \
  -d '{"query":{"multi_match":{"query":"Redis 缓存","fields":["title^3","summary","content"],"analyzer":"ik_smart"}},"size":10}'
```

深翻页对比（第 4000 页，`from+size` 越窗用 `after` 游标）：取上一页响应 `nextAfter` 传入 `after` 参数重放。

## 3. 结果表（人工实测后填写）

| 场景（2 万篇，pageSize=10） | MySQL LIKE 中位数 | ES 中位数 | ES 原生（不含回表） | 备注 |
|---|---|---|---|---|
| 高频词「Redis 缓存」 | — ms | — ms | — ms | |
| 高频词「MySQL 索引」 | — ms | — ms | — ms | |
| 高频词「Docker 容器」 | — ms | — ms | — ms | |
| 高频词「Spring Boot 配置」 | — ms | — ms | — ms | |
| 高频词「微服务 网关」 | — ms | — ms | — ms | |
| 低频词「Python爬虫」 | — ms | — ms | — ms | 验收用例：命中「Python 爬虫入门」 |
| 空结果词「量子纠缠」 | — ms | — ms | — ms | |
| 深翻页（第 4000 页，after 游标） | — ms | — ms | — ms | LIKE 分页 OFFSET 代价 vs search_after |

结论要点（待实测后补充）：LIKE 全表扫描耗时随数据量线性增长、与命中数无关；ES 耗时主要在召回与打分，深翻页 from+size 成本随 from 增长而 search_after 恒定（游标续扫）。
