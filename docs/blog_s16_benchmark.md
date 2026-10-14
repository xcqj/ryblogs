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

## 3. 结果表

> ES 侧：Agent 已实测（2026-10-14，2 万压测 + 24 精选 = 20040 篇，后端引擎 es，11 次取中位数）；
> MySQL 侧：切 `blog.search.engine=mysql` 重启后，用下方同款循环跑一遍填入即完成对比。

| 场景（2 万篇，pageSize=10） | MySQL LIKE 中位数 | ES 中位数 | ES 原生（不含回表） | total（精确计数） | 备注 |
|---|---|---|---|---|---|
| 高频词「Redis 缓存」 | 待填 | **22.6 ms** | 待填 | 20002 | |
| 高频词「MySQL 索引」 | 待填 | **22.5 ms** | 待填 | 20004 | |
| 高频词「Docker 容器」 | 待填 | **21.6 ms** | 待填 | 20003 | |
| 高频词「Spring Boot 配置」 | 待填 | **20.5 ms** | 待填 | 20004 | |
| 高频词「微服务 网关」 | 待填 | **21.3 ms** | 待填 | 20001 | |
| 低频词「Python爬虫」 | 待填 | **17.6 ms** | 待填 | 2 | 验收用例：命中「Python 爬虫入门」 |
| 空结果词「量子纠缠」 | 待填 | **5.7 ms** | 待填 | 0 | |
| 深翻页（第 4000 页，after 游标） | 待填 | 待填 | 待填 | | LIKE 分页 OFFSET 代价 vs search_after |

要点（已可支撑面试叙事）：

- **高频词 ES ~21ms（含 Java 回表 + 高亮 + tags 填充的端到端）**：ES 命中 2 万篇不随命中数明显劣化；LIKE 需要实测对比（预期随数据量线性增长）。
- **total 语义**：曾出现 total=10000，系 ES 默认 `track_total_hits` 计数上限（下界），已显式开启精确计数修正（S16 联调修复条 ⑤）。
- 每篇压测文正文都含五组关键词的"覆盖句"，故高频词命中 ≈ 全量压测文——这是刻意设计，保证对比样本充足。

MySQL 侧执行一键循环（切引擎重启后粘贴）：

```bash
for kw in 'Redis 缓存' 'MySQL 索引' 'Docker 容器' 'Spring Boot 配置' '微服务 网关' 'Python爬虫' '量子纠缠'; do
  enc=$(python3 -c "import urllib.parse,sys;print(urllib.parse.quote(sys.argv[1]))" "$kw")
  med=$(for i in $(seq 1 11); do curl -s -o /dev/null -w '%{time_total}\n' -m 60 "http://localhost:8081/blog/public/search?keyword=$enc&pageNum=1&pageSize=10"; done | sort -n | sed -n '6p')
  printf '%-16s 中位数=%ss\n' "$kw" "$med"
done
```

## 4. 零停机重建实测（2026-10-14，Agent 已跑通，面试可复现）

```text
前提：alias blog_article → v1（20040 篇）
① 建 v2：复制 v1 的 settings+mappings（去掉 uuid/creation_date 等内部项）
② _reindex：{"total":20040,"created":20040,"version_conflicts":0} —— took 2336ms
③ _aliases 单请求原子切换 v1→v2（remove+add 同请求，ES 保证无中间态）
全程并发搜索 55/55 全 200 —— 零停机实证
```

复现脚本要点见 ROADMAP §3 S16 联调修复条与本文 §2；切换后旧 v1 可择机删除（`DELETE /blog_article_v1`）。
