-- =========================================================
-- S16 ES 搜索演示数据（ROADMAP S16 验收清单：中文分词实测 / LIKE vs ES 对比）
--
-- 用途：
--   ① 24 篇中文技术文章（ curated ），覆盖验收用例「Python爬虫 命中 Python 爬虫」
--      与自定义词典技术词整词匹配（SpringBoot / K8s / Redis 等，见 script/docker/es/tech.dic）；
--   ② 2 万篇压测文章（默认 20000，可改下方 RECURSIVE 上界，范围 1~5 万），
--      用于 /blog/public/search 的 LIKE vs ES 延迟对比（面试材料）。
--
-- 清理：docs/blog_s16_cleanup.sql（按本文登记的 ID 段删除 + ES 文档清理步骤）。
--       幂等模式 = 先跑 cleanup 再跑本脚本（同 S3-D3 人工确认模式）。
--
-- 安全约定：
--   - 全部 ID 落在 9e18 量级演示段，与雪花 ID（约 1.7~1.9e18）不重叠；
--   - 标签 slug 一律 s16- 前缀，避开既有标签唯一键（uq_blog_tag_slug）；
--   - 文章 slug 一律 s16- 前缀，避开 uq_blog_article_slug。
--
-- ⚠ ES 同步说明：SQL 直插绕过应用事件，ES 索引不会即时更新——
--   演示对账任务（ROADMAP S16 验收「对账任务能发现并修复不同步」）时正好利用这点：
--   执行本脚本后等待 blog.index.reconcile-cron 周期（默认 30min），
--   或临时把 application.yml 的 blog.index.reconcile-cron 改成 "0 0/1 * * * ?"（每分钟）加速验证。
--
-- 执行：docker exec -i blog-mysql mysql -uroot -proot ry-vue < docs/blog_s16_demo_data.sql
-- =========================================================

SET NAMES utf8mb4;

-- ---------- 0. 演示作者（取 admin；无则取最小 user_id） ----------
SET @author_id := (
    SELECT user_id FROM sys_user
    WHERE user_name = 'admin' AND del_flag = '0'
    LIMIT 1
);
SET @author_id := COALESCE(@author_id, (SELECT MIN(user_id) FROM sys_user WHERE del_flag = '0'));
-- 验证段：应返回 1 行非 NULL（如为 NULL 先修正再继续）
SELECT @author_id AS demo_author_id;

-- ---------- ID 段登记（清理脚本按此段删除） ----------
-- sd_blog_tag.tag_id               : 9200000000000000001 ~ 9200000000000000030（30 个演示标签）
-- sd_blog_article.article_id       : 9000000000000000001 ~ 9000000000000020024
--                                     （1~24 curated；25 起 2 万篇压测）
-- sd_blog_article_version.version_id : 9100000000000000001 ~ 9100000000000020024
--                                     （= 对应 article_id + 100000000000000000）
-- sd_blog_article_tag.article_tag_id : 7000000000000000001 起

-- ---------- 1. 演示标签（30 个；name 展示用，slug 带 s16- 前缀防撞键） ----------
INSERT INTO sd_blog_tag (tag_id, name, slug, status, merged_into, is_featured, sort_order,
                         create_by, create_time, update_by, update_time, del_flag)
VALUES
(9200000000000000001, 'Python',        's16-python',        '0', NULL, '0', 0, @author_id, NOW(), @author_id, NOW(), '0'),
(9200000000000000002, 'Redis',         's16-redis',         '0', NULL, '0', 0, @author_id, NOW(), @author_id, NOW(), '0'),
(9200000000000000003, 'MySQL',         's16-mysql',         '0', NULL, '0', 0, @author_id, NOW(), @author_id, NOW(), '0'),
(9200000000000000004, 'Spring Boot',   's16-springboot',    '0', NULL, '0', 0, @author_id, NOW(), @author_id, NOW(), '0'),
(9200000000000000005, 'Docker',        's16-docker',        '0', NULL, '0', 0, @author_id, NOW(), @author_id, NOW(), '0'),
(9200000000000000006, 'Kubernetes',    's16-kubernetes',    '0', NULL, '0', 0, @author_id, NOW(), @author_id, NOW(), '0'),
(9200000000000000007, '微服务',         's16-microservice',  '0', NULL, '0', 0, @author_id, NOW(), @author_id, NOW(), '0'),
(9200000000000000008, 'Elasticsearch', 's16-elasticsearch', '0', NULL, '0', 0, @author_id, NOW(), @author_id, NOW(), '0'),
(9200000000000000009, 'JVM',           's16-jvm',           '0', NULL, '0', 0, @author_id, NOW(), @author_id, NOW(), '0'),
(9200000000000000010, 'Vue3',          's16-vue3',          '0', NULL, '0', 0, @author_id, NOW(), @author_id, NOW(), '0'),
(9200000000000000011, 'Nuxt',          's16-nuxt',          '0', NULL, '0', 0, @author_id, NOW(), @author_id, NOW(), '0'),
(9200000000000000012, '分布式',         's16-distributed',   '0', NULL, '0', 0, @author_id, NOW(), @author_id, NOW(), '0'),
(9200000000000000013, '消息队列',       's16-mq',            '0', NULL, '0', 0, @author_id, NOW(), @author_id, NOW(), '0'),
(9200000000000000014, 'Linux',         's16-linux',         '0', NULL, '0', 0, @author_id, NOW(), @author_id, NOW(), '0'),
(9200000000000000015, 'Git',           's16-git',           '0', NULL, '0', 0, @author_id, NOW(), @author_id, NOW(), '0'),
(9200000000000000016, '算法',           's16-algorithm',     '0', NULL, '0', 0, @author_id, NOW(), @author_id, NOW(), '0'),
(9200000000000000017, '数据结构',       's16-datastruct',    '0', NULL, '0', 0, @author_id, NOW(), @author_id, NOW(), '0'),
(9200000000000000018, '操作系统',       's16-os',            '0', NULL, '0', 0, @author_id, NOW(), @author_id, NOW(), '0'),
(9200000000000000019, '计算机网络',     's16-network',       '0', NULL, '0', 0, @author_id, NOW(), @author_id, NOW(), '0'),
(9200000000000000020, '设计模式',       's16-pattern',       '0', NULL, '0', 0, @author_id, NOW(), @author_id, NOW(), '0'),
(9200000000000000021, 'Nginx',         's16-nginx',         '0', NULL, '0', 0, @author_id, NOW(), @author_id, NOW(), '0'),
(9200000000000000022, 'TypeScript',    's16-typescript',    '0', NULL, '0', 0, @author_id, NOW(), @author_id, NOW(), '0'),
(9200000000000000023, '单元测试',       's16-testing',       '0', NULL, '0', 0, @author_id, NOW(), @author_id, NOW(), '0'),
(9200000000000000024, '架构',           's16-architecture',  '0', NULL, '0', 0, @author_id, NOW(), @author_id, NOW(), '0'),
(9200000000000000025, 'Java',          's16-java',          '0', NULL, '0', 0, @author_id, NOW(), @author_id, NOW(), '0'),
(9200000000000000026, 'JavaScript',    's16-javascript',    '0', NULL, '0', 0, @author_id, NOW(), @author_id, NOW(), '0'),
(9200000000000000027, '后端',           's16-backend',       '0', NULL, '0', 0, @author_id, NOW(), @author_id, NOW(), '0'),
(9200000000000000028, '前端',           's16-frontend',      '0', NULL, '0', 0, @author_id, NOW(), @author_id, NOW(), '0'),
(9200000000000000029, '数据库',         's16-database',      '0', NULL, '0', 0, @author_id, NOW(), @author_id, NOW(), '0'),
(9200000000000000030, '缓存',           's16-cache',         '0', NULL, '0', 0, @author_id, NOW(), @author_id, NOW(), '0');

-- ---------- 2. 精选中文技术文章 24 篇（版本表 + 主表 + 标签关系） ----------
-- 第 1 篇即验收用例：查询「Python爬虫」（无空格）应命中标题「Python 爬虫入门」

INSERT INTO sd_blog_article_version (version_id, article_id, version_no, title, summary, cover_url, content, create_by, create_time, del_flag)
VALUES
(9100000000000000001, 9000000000000000001, 1, 'Python 爬虫入门：从请求到解析',
 '用 requests 加正则完成第一个爬虫，讲清请求头、编码与解析三个关键点。', NULL,
 '<p>爬虫的第一步是发出一个"像人"的请求：带上 User-Agent，处理 gzip 与编码。</p><p>拿到 HTML 后用正则或选择器提取目标字段，再用管道把结果写进文件，一个最小可用的 Python 爬虫就完成了。</p>',
 @author_id, NOW(), '0'),
(9100000000000000002, 9000000000000000002, 1, 'Redis 高并发缓存的三种模式',
 'Cache Aside、Read/Write Through 与 Write Behind 的取舍，附一致性注意事项。', NULL,
 '<p>Cache Aside 最简单：读先查缓存，写先更新数据库再删缓存，适合绝大多数读多写少场景。</p><p>Write Behind 用内存队列合并写请求，吞吐最高但宕机丢数据，只适合可丢失的计数类数据。</p>',
 @author_id, NOW(), '0'),
(9100000000000000003, 9000000000000000003, 1, 'MySQL 索引设计与执行计划分析',
 '从 B+ 树原理到 EXPLAIN 实战，讲清最左前缀与覆盖索引。', NULL,
 '<p>索引不是越多越好：每个索引都是写放大。先用 EXPLAIN 看 type 与 rows，再决定加什么索引。</p><p>组合索引遵循最左前缀，把区分度高的列放前面；SELECT 只取需要的列，才有机会走覆盖索引避免回表。</p>',
 @author_id, NOW(), '0'),
(9100000000000000004, 9000000000000000004, 1, 'Spring Boot 自动配置原理拆解',
 '从 @SpringBootApplication 到条件装配，一遍读懂 Starter 机制。', NULL,
 '<p>自动配置的核心是条件注解：@ConditionalOnClass 命中类路径才生效，@ConditionalOnMissingBean 给用户留覆盖口。</p><p>理解 spring.factories 与 AutoConfiguration.imports 的加载顺序，就能自己写一个 Starter。</p>',
 @author_id, NOW(), '0'),
(9100000000000000005, 9000000000000000005, 1, 'Docker 容器化部署实战',
 '镜像分层、多阶段构建与 compose 编排的最小实践。', NULL,
 '<p>镜像层是复用的：把变动最少的依赖层放前面，能显著加快构建与拉取。</p><p>多阶段构建把编译环境留在构建期，运行镜像只带 JRE 和产物，体积能小一个数量级。</p>',
 @author_id, NOW(), '0'),
(9100000000000000006, 9000000000000000006, 1, 'Kubernetes 入门：Pod 与 Service',
 '用最小概念理解 K8s：Pod 是调度单位，Service 是稳定的访问入口。', NULL,
 '<p>Pod 是 K8s 最小的调度单位，一个 Pod 里的容器共享网络与存储。</p><p>Pod 会重建、IP 会变，Service 通过标签选择器提供稳定域名，把流量均匀分到后端 Pod。</p>',
 @author_id, NOW(), '0'),
(9100000000000000007, 9000000000000000007, 1, '微服务网关限流与熔断设计',
 '网关层统一限流，服务层熔断降级，两层防线怎么配合。', NULL,
 '<p>限流守在入口：按 IP 或用户维度令牌桶排队，超过阈值的请求快速失败，保护后端不被打穿。</p><p>熔断守在出口：下游连续超时就打开断路器，直接走降级逻辑，给依赖服务恢复的时间窗口。</p>',
 @author_id, NOW(), '0'),
(9100000000000000008, 9000000000000000008, 1, 'Elasticsearch 分词原理与 IK 实践',
 '倒排索引 + 中文分词，讲清 ik_max_word 与 ik_smart 的选型。', NULL,
 '<p>ES 的检索速度来自倒排索引：词项到文档的映射，写入时分析、查询时分析。</p><p>中文分词用 IK：索引侧 ik_max_word 细粒度最大化召回，查询侧 ik_smart 减少歧义，再配自定义词典兜底技术词。</p>',
 @author_id, NOW(), '0'),
(9100000000000000009, 9000000000000000009, 1, 'JVM 垃圾回收器演进之路',
 '从 Serial 到 ZGC，吞吐与延迟的权衡史。', NULL,
 '<p>Serial 单线程停顿但简单；Parallel 用多线程换吞吐，适合后台批处理。</p><p>G1 把堆拆成 Region 做增量回收，ZGC 更是把停顿压到毫秒级，选型先看业务对延迟的要求。</p>',
 @author_id, NOW(), '0'),
(9100000000000000010, 9000000000000000010, 1, 'Vue3 组合式 API 设计思路',
 '从 Options 到 Composition，逻辑复用方式的转变。', NULL,
 '<p>组合式 API 把同一逻辑的 state 与行为收拢在一起，组件大了之后可读性优势明显。</p><p>配合 ref 与 computed 做细粒度响应式，模板里几乎不需要再写计算属性名。</p>',
 @author_id, NOW(), '0'),
(9100000000000000011, 9000000000000000011, 1, 'Nuxt SSR 渲染机制浅析',
 '服务端渲染首屏、客户端水合，以及 SSR 安全的红线。', NULL,
 '<p>Nuxt 在服务端跑一遍组件产出首屏 HTML，浏览器端再水合成交互应用。</p><p>SSR 阶段没有 window 与 document，浏览器 API 必须包进 ClientOnly 或客户端守卫。</p>',
 @author_id, NOW(), '0'),
(9100000000000000012, 9000000000000000012, 1, '分布式 ID 生成方案对比',
 'UUID、雪花与号段模式，工程上怎么选。', NULL,
 '<p>UUID 无序且长，做主键放大索引；雪花趋势递增但依赖时钟回拨处理。</p><p>号段模式一次取一段 ID 在内存分配，性能最好，代价是要处理号段耗尽的批量申请。</p>',
 @author_id, NOW(), '0'),
(9100000000000000013, 9000000000000000013, 1, '消息队列削峰填谷实践',
 '异步化提升吞吐，但要接受最终一致与消息重复。', NULL,
 '<p>把写请求丢进队列由消费者匀速处理，峰值再高也不会压垮数据库。</p><p>消费者必须幂等：同一条消息重投两次，结果要和一次相同，否则削峰变成数据错乱。</p>',
 @author_id, NOW(), '0'),
(9100000000000000014, 9000000000000000014, 1, 'Linux 性能排查命令速查',
 'CPU、内存、磁盘、网络四个维度的一套排查动作。', NULL,
 '<p>top 看整体负载，vmstat 看上下文切换与内存压力，iostat 看磁盘等待。</p><p>网络问题先用 ss 看连接状态分布，再用 tcpdump 抓包定位，不要一上来就猜。</p>',
 @author_id, NOW(), '0'),
(9100000000000000015, 9000000000000000015, 1, 'Git 分支管理策略讨论',
 '主干开发与 Git Flow 的适用场景。', NULL,
 '<p>小团队高频发布适合主干开发加功能开关，长生命周期的版本线才需要 Git Flow。</p><p>分支策略本质是发布节奏的倒影，先想清楚多久发一次版，再定分支模型。</p>',
 @author_id, NOW(), '0'),
(9100000000000000016, 9000000000000000016, 1, '算法图解：二分查找的边界',
 '区间开闭与循环条件，一次讲透边界错误。', NULL,
 '<p>二分查找写错几乎都在边界：左闭右开区间里，right 取 mid 而非 mid-1。</p><p>把区间定义写在注释里，再照 definition 推循环体，边界错误能少一半。</p>',
 @author_id, NOW(), '0'),
(9100000000000000017, 9000000000000000017, 1, '数据结构：链表与数组的取舍',
 '连续内存与指针串联，缓存友好性的差异。', NULL,
 '<p>数组随机访问 O(1) 且缓存友好，链表插入删除 O(1) 但要付出指针开销。</p><p>现代 CPU 上缓存命中的权重很高，小数据量下数组往往比链表更快，别默认链表优。</p>',
 @author_id, NOW(), '0'),
(9100000000000000018, 9000000000000000018, 1, '操作系统进程调度概述',
 '时间片、优先级与完全公平调度器。', NULL,
 '<p>调度器在可运行进程间分配 CPU，目标兼顾吞吐、响应与公平。</p><p>Linux 的 CFS 用虚拟运行时间模拟"完全公平"，交互式进程因为睡眠能获得更高优先级。</p>',
 @author_id, NOW(), '0'),
(9100000000000000019, 9000000000000000019, 1, '计算机网络 TCP 三次握手',
 '为什么是三次，不是两次。', NULL,
 '<p>三次握手确认双方的收发能力：客户端发、服务端发+确认、客户端确认。</p><p>两次无法确认客户端的接收能力，也无法让服务端确认客户端收到了自己的初始序列号。</p>',
 @author_id, NOW(), '0'),
(9100000000000000020, 9000000000000000020, 1, '设计模式：观察者模式实例',
 '一对多依赖的解耦，事件驱动的基础。', NULL,
 '<p>观察者模式让主题只维护订阅列表，状态变化时广播通知，不关心谁在听。</p><p>Spring 的事件机制就是观察者模式：发布者与监听者零直接依赖，扩展点自然开放。</p>',
 @author_id, NOW(), '0'),
(9100000000000000021, 9000000000000000021, 1, 'Nginx 反向代理与负载均衡',
 'proxy_pass、upstream 与健康检查的基础配置。', NULL,
 '<p>反向代理把请求按 location 分发给不同上游，动静分离与灰度发布都靠它。</p><p>upstream 里配权重与 backup，再开健康检查，单点故障时流量能自动切走。</p>',
 @author_id, NOW(), '0'),
(9100000000000000022, 9000000000000000022, 1, 'TypeScript 类型体操入门',
 '从工具类型到条件类型，渐进掌握类型编程。', NULL,
 '<p>先掌握 Pick/Omit/Partial 这类内置工具类型，再学 infer 与条件类型。</p><p>类型是编译期的，别为了炫技把运行时才能确定的逻辑塞进类型系统。</p>',
 @author_id, NOW(), '0'),
(9100000000000000023, 9000000000000000023, 1, '单元测试为什么只测纯逻辑',
 '数据库与容器留给集成测试，单测专注可算对错的部分。', NULL,
 '<p>单测的价值在快速反馈：不碰数据库、不起容器，几毫秒内给出对错。</p><p>把可测逻辑抽成独立类，Service 只做编排，测试覆盖率才有意义。</p>',
 @author_id, NOW(), '0'),
(9100000000000000024, 9000000000000000024, 1, '我的技术博客架构设计',
 'Spring Boot 加 Nuxt SSR，一次全栈练手的技术选型记录。', NULL,
 '<p>后端 RuoYi-Vue-Plus 二次开发，模块化拆出博客域；前台 Nuxt SSR 保证首屏与 SEO。</p><p>搜索用 Elasticsearch 替换 MySQL LIKE，索引只收公共可见文章，事件驱动同步加定时对账兜底。</p>',
 @author_id, NOW(), '0');

INSERT INTO sd_blog_article (article_id, author_id, slug, status, visibility, current_version_id,
                             view_count, published_at, create_by, create_time, update_by, update_time, del_flag)
VALUES
(9000000000000000001, @author_id, 's16-demo-01', '1', '0', 9100000000000000001, 128, NOW() - INTERVAL 24 DAY, @author_id, NOW(), @author_id, NOW(), '0'),
(9000000000000000002, @author_id, 's16-demo-02', '1', '0', 9100000000000000002, 356, NOW() - INTERVAL 23 DAY, @author_id, NOW(), @author_id, NOW(), '0'),
(9000000000000000003, @author_id, 's16-demo-03', '1', '0', 9100000000000000003, 89,  NOW() - INTERVAL 22 DAY, @author_id, NOW(), @author_id, NOW(), '0'),
(9000000000000000004, @author_id, 's16-demo-04', '1', '0', 9100000000000000004, 212, NOW() - INTERVAL 21 DAY, @author_id, NOW(), @author_id, NOW(), '0'),
(9000000000000000005, @author_id, 's16-demo-05', '1', '0', 9100000000000000005, 67,  NOW() - INTERVAL 20 DAY, @author_id, NOW(), @author_id, NOW(), '0'),
(9000000000000000006, @author_id, 's16-demo-06', '1', '0', 9100000000000000006, 45,  NOW() - INTERVAL 19 DAY, @author_id, NOW(), @author_id, NOW(), '0'),
(9000000000000000007, @author_id, 's16-demo-07', '1', '0', 9100000000000000007, 178, NOW() - INTERVAL 18 DAY, @author_id, NOW(), @author_id, NOW(), '0'),
(9000000000000000008, @author_id, 's16-demo-08', '1', '0', 9100000000000000008, 534, NOW() - INTERVAL 17 DAY, @author_id, NOW(), @author_id, NOW(), '0'),
(9000000000000000009, @author_id, 's16-demo-09', '1', '0', 9100000000000000009, 91,  NOW() - INTERVAL 16 DAY, @author_id, NOW(), @author_id, NOW(), '0'),
(9000000000000000010, @author_id, 's16-demo-10', '1', '0', 9100000000000000010, 156, NOW() - INTERVAL 15 DAY, @author_id, NOW(), @author_id, NOW(), '0'),
(9000000000000000011, @author_id, 's16-demo-11', '1', '0', 9100000000000000011, 73,  NOW() - INTERVAL 14 DAY, @author_id, NOW(), @author_id, NOW(), '0'),
(9000000000000000012, @author_id, 's16-demo-12', '1', '0', 9100000000000000012, 38,  NOW() - INTERVAL 13 DAY, @author_id, NOW(), @author_id, NOW(), '0'),
(9000000000000000013, @author_id, 's16-demo-13', '1', '0', 9100000000000000013, 264, NOW() - INTERVAL 12 DAY, @author_id, NOW(), @author_id, NOW(), '0'),
(9000000000000000014, @author_id, 's16-demo-14', '1', '0', 9100000000000000014, 119, NOW() - INTERVAL 11 DAY, @author_id, NOW(), @author_id, NOW(), '0'),
(9000000000000000015, @author_id, 's16-demo-15', '1', '0', 9100000000000000015, 47,  NOW() - INTERVAL 10 DAY, @author_id, NOW(), @author_id, NOW(), '0'),
(9000000000000000016, @author_id, 's16-demo-16', '1', '0', 9100000000000000016, 82,  NOW() - INTERVAL 9 DAY,  @author_id, NOW(), @author_id, NOW(), '0'),
(9000000000000000017, @author_id, 's16-demo-17', '1', '0', 9100000000000000017, 58,  NOW() - INTERVAL 8 DAY,  @author_id, NOW(), @author_id, NOW(), '0'),
(9000000000000000018, @author_id, 's16-demo-18', '1', '0', 9100000000000000018, 134, NOW() - INTERVAL 7 DAY,  @author_id, NOW(), @author_id, NOW(), '0'),
(9000000000000000019, @author_id, 's16-demo-19', '1', '0', 9100000000000000019, 96,  NOW() - INTERVAL 6 DAY,  @author_id, NOW(), @author_id, NOW(), '0'),
(9000000000000000020, @author_id, 's16-demo-20', '1', '0', 9100000000000000020, 61,  NOW() - INTERVAL 5 DAY,  @author_id, NOW(), @author_id, NOW(), '0'),
(9000000000000000021, @author_id, 's16-demo-21', '1', '0', 9100000000000000021, 43,  NOW() - INTERVAL 4 DAY,  @author_id, NOW(), @author_id, NOW(), '0'),
(9000000000000000022, @author_id, 's16-demo-22', '1', '0', 9100000000000000022, 187, NOW() - INTERVAL 3 DAY,  @author_id, NOW(), @author_id, NOW(), '0'),
(9000000000000000023, @author_id, 's16-demo-23', '1', '0', 9100000000000000023, 72,  NOW() - INTERVAL 2 DAY,  @author_id, NOW(), @author_id, NOW(), '0'),
(9000000000000000024, @author_id, 's16-demo-24', '1', '0', 9100000000000000024, 309, NOW() - INTERVAL 1 DAY,  @author_id, NOW(), @author_id, NOW(), '0');

INSERT INTO sd_blog_article_tag (article_tag_id, article_id, tag_id, create_by, create_time)
VALUES
(7000000000000000001, 9000000000000000001, 9200000000000000001, @author_id, NOW()),
(7000000000000000002, 9000000000000000002, 9200000000000000002, @author_id, NOW()),
(7000000000000000003, 9000000000000000002, 9200000000000000030, @author_id, NOW()),
(7000000000000000004, 9000000000000000003, 9200000000000000003, @author_id, NOW()),
(7000000000000000005, 9000000000000000003, 9200000000000000029, @author_id, NOW()),
(7000000000000000006, 9000000000000000004, 9200000000000000004, @author_id, NOW()),
(7000000000000000007, 9000000000000000004, 9200000000000000025, @author_id, NOW()),
(7000000000000000008, 9000000000000000005, 9200000000000000005, @author_id, NOW()),
(7000000000000000009, 9000000000000000006, 9200000000000000006, @author_id, NOW()),
(7000000000000000010, 9000000000000000007, 9200000000000000007, @author_id, NOW()),
(7000000000000000011, 9000000000000000008, 9200000000000000008, @author_id, NOW()),
(7000000000000000012, 9000000000000000008, 9200000000000000002, @author_id, NOW()),
(7000000000000000013, 9000000000000000009, 9200000000000000009, @author_id, NOW()),
(7000000000000000014, 9000000000000000010, 9200000000000000010, @author_id, NOW()),
(7000000000000000015, 9000000000000000011, 9200000000000000011, @author_id, NOW()),
(7000000000000000016, 9000000000000000012, 9200000000000000012, @author_id, NOW()),
(7000000000000000017, 9000000000000000013, 9200000000000000013, @author_id, NOW()),
(7000000000000000018, 9000000000000000014, 9200000000000000014, @author_id, NOW()),
(7000000000000000019, 9000000000000000015, 9200000000000000015, @author_id, NOW()),
(7000000000000000020, 9000000000000000016, 9200000000000000016, @author_id, NOW()),
(7000000000000000021, 9000000000000000017, 9200000000000000017, @author_id, NOW()),
(7000000000000000022, 9000000000000000018, 9200000000000000018, @author_id, NOW()),
(7000000000000000023, 9000000000000000019, 9200000000000000019, @author_id, NOW()),
(7000000000000000024, 9000000000000000020, 9200000000000000020, @author_id, NOW()),
(7000000000000000025, 9000000000000000021, 9200000000000000021, @author_id, NOW()),
(7000000000000000026, 9000000000000000022, 9200000000000000022, @author_id, NOW()),
(7000000000000000027, 9000000000000000023, 9200000000000000023, @author_id, NOW()),
(7000000000000000028, 9000000000000000024, 9200000000000000024, @author_id, NOW()),
(7000000000000000029, 9000000000000000006, 9200000000000000014, @author_id, NOW()),
(7000000000000000030, 9000000000000000005, 9200000000000000014, @author_id, NOW());

-- ---------- 3. 压测文章（默认 20000 篇；改 RECURSIVE 上界即可调 1~5 万） ----------
-- 关键词五组轮换，LIKE 与 ES 对比查询统一用：Redis 缓存 / MySQL 索引 / Docker 容器 /
-- Spring Boot 配置 / 微服务 网关（每组约 4000 篇命中，样本足够）
SET SESSION cte_max_recursion_depth = 100000;

INSERT INTO sd_blog_article_version (version_id, article_id, version_no, title, summary, cover_url, content, create_by, create_time, del_flag)
WITH RECURSIVE seq(n) AS (
    SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 20000
)
SELECT
    9100000000000000024 + n,
    9000000000000000024 + n,
    1,
    CONCAT('压测 ', n, '：', ELT(1 + (n % 5),
        'Redis 高并发缓存设计实践',
        'MySQL 索引优化实践总结',
        'Docker 容器编排部署笔记',
        'Spring Boot 配置调优记录',
        '微服务网关设计与落地'), '（第 ', n, ' 篇）'),
    CONCAT('压测文章第 ', n, ' 篇：围绕 ', ELT(1 + (n % 5), 'Redis 缓存', 'MySQL 索引', 'Docker 容器', 'Spring Boot 配置', '微服务网关'), ' 展开的实践记录，用于搜索延迟对比。'),
    NULL,
    CONCAT(
        '<p>本文是搜索压测样本第 ', n, ' 篇，主题为 ', ELT(1 + (n % 5), 'Redis 缓存', 'MySQL 索引', 'Docker 容器', 'Spring Boot 配置', '微服务网关'), '。</p>',
        '<p>背景：线上服务在高峰期的响应延迟出现波动，需要系统性地复盘 ', ELT(1 + (n % 5), 'Redis 缓存穿透与热点 key', 'MySQL 索引选择与执行计划', 'Docker 镜像分层与编排策略', 'Spring Boot 连接池与自动配置', '微服务网关的限流熔断链路'), '。</p>',
        '<p>做法：先压测定位瓶颈，再针对性调整参数，最后回归验证。关键词覆盖 Redis 缓存、MySQL 索引、Docker 容器、Spring Boot 配置、微服务网关、限流、熔断、执行计划、连接池。</p>',
        '<p>结论：样本编号 ', n, '，延迟数据已留档，用于 MySQL LIKE 与 Elasticsearch 的检索延迟对比。</p>'),
    @author_id, NOW(), '0'
FROM seq;

INSERT INTO sd_blog_article (article_id, author_id, slug, status, visibility, current_version_id,
                             view_count, published_at, create_by, create_time, update_by, update_time, del_flag)
WITH RECURSIVE seq(n) AS (
    SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 20000
)
SELECT
    9000000000000000024 + n,
    @author_id,
    CONCAT('s16-bench-', n),
    '1', '0',
    9100000000000000024 + n,
    FLOOR(RAND() * 500),
    NOW() - INTERVAL (n % 90) DAY - INTERVAL (n % 24) HOUR - INTERVAL (n % 60) MINUTE,
    @author_id, NOW(), @author_id, NOW(), '0'
FROM seq;

INSERT INTO sd_blog_article_tag (article_tag_id, article_id, tag_id, create_by, create_time)
WITH RECURSIVE seq(n) AS (
    SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 20000
)
SELECT
    7000000000000000030 + n,
    9000000000000000024 + n,
    9200000000000000001 + (n % 8),
    @author_id, NOW()
FROM seq;

-- ---------- 4. 验证段（执行后核对） ----------
SELECT COUNT(*) AS demo_public_articles FROM sd_blog_article
WHERE article_id BETWEEN 9000000000000000001 AND 9000000000000020024
  AND status = '1' AND visibility = '0' AND del_flag = '0';
-- 期望：20024

SELECT COUNT(*) AS demo_versions FROM sd_blog_article_version
WHERE version_id BETWEEN 9100000000000000001 AND 9100000000000020024;
-- 期望：20024

SELECT COUNT(*) AS demo_tags FROM sd_blog_tag
WHERE tag_id BETWEEN 9200000000000000001 AND 9200000000000000030;
-- 期望：30

-- ES 侧核对（应用启动且对账/初始同步后）：curl -s 'localhost:9200/blog_article/_count'
-- 期望文档数 ≈ MySQL 公共文章数（含既有 16 篇 + 20024 篇演示）
