# 系统架构（architecture）

> 状态：v0.3 · 2026-10-14（S16：新增 ES 搜索组件与索引同步链路）
> 定位：一张图 + 一条请求链 + 一套部署拓扑。只讲"系统长什么样"，不讲"业务是什么"。
> 业务规则见 PRD，接口见 blog-api.md，表结构见 database-design/schema，红线见 AGENTS.md——本文一律不重复。

## 1. 系统定位

```text
┌─────────────────────────── 前端（两个独立仓库，同级 clone）───────────────────────────┐
│   博客前台站 blog-web（读者/作者）  管理后台 plus-ui（作者 + 管理员共用）              │
│   首页/文章/标签/搜索/用户/作者中心  我的文章 / 博客治理菜单 / RuoYi 系统管理           │
└──────────────┬───────────────────────────────┬───────────────────────────────────────┘
               │ /blog/public/**（匿名）＋作者自助 │ /blog/article|user|oss/**、/auth/**、/system/**（Sa-Token）
               ▼                               ▼
┌──────────────────────────── 本仓库：RuoYi-Vue-Plus 6.X 模块化单体 ────────────────────────────┐
│  ruoyi-admin（唯一启动入口，聚合所有模块）                                                      │
│  ├── sd-blog（博客业务模块，包 org.dromara.blog）                                              │
│  ├── ruoyi-modules/*（system、gen、workflow、job、ai、demo）                                   │
│  └── ruoyi-common/*（mybatis、satoken、redis、oss、excel、log、web… 横切能力）                  │
└──────────┬──────────────────┬─────────────────────┬────────────────────┬────────────────┘
           ▼                  ▼                     ▼                    ▼
      MySQL 8.4（主存储）  Redis 8.x（缓存/会话/限流） OSS/MinIO（对象存储）  Elasticsearch 7.17（全文检索，仅公共文章）
```

- 架构形态：**单体多模块 Maven 工程**，唯一部署单元 `ruoyi-admin`；模块间通过 Service 接口调用，无 RPC。
- 博客不新建用户/认证/RBAC/文件体系，全部复用 RuoYi 既有基础设施（PRD §13）。
- 前端是**两个独立仓库**（与后端同级 clone）：博客前台 `blog-web`（仓库 `blogs-web`，S9 起建）与管理后台 `plus-ui`（仓库 `ryblogs-ui`）；
  blog-web 调公共层 `/blog/public/**` 与登录作者自助层 `/blog/article|user/**`（作者中心，D29），plus-ui 调登录用户/管理层；平台治理 `/blog/admin/**` 仅 plus-ui。仓库与工作区布局见 `docs/SETUP.md` §10 与 ROADMAP D21/D29。

## 2. sd-blog 内部结构

```text
org.dromara.blog
├── controller
│   ├── BlogPublicXxxController    匿名接口（security.excludes 白名单）
│   ├── BlogXxxController          作者自助接口（登录即可，author_id 服务端过滤）
│   ├── BlogAdminXxxController     管理员治理接口（@SaCheckPermission blog:*）
│   └── SdBlogXxxController        生成器 CRUD 骨架（过渡期临时工具，被契约接口取代后删除）
├── domain          Entity（继承 BaseEntity，@TableLogic String delFlag）
│   ├── bo / vo     入参 / 出参（@AutoMapper，mapstruct-plus 编译期生成转换器）
├── mapper          BaseMapperPlus<Entity, Vo>
├── es              ES 索引生命周期（IndexManager/Bootstrap：mapping/alias/reindex，S16）
├── event           文章变更事件 → ES 增量同步（AFTER_COMMIT 监听，S16）
└── service / impl  业务规则、状态机、事务边界；search/ 下搜索抽象（Mysql/Es 双实现，S16）
resources/mapper/blog/  自定义 SQL（仅在 wrapper 不够用时）
```

类命名与分层决策见 ROADMAP §6（D5）。

## 3. 一次请求的链路

```text
HTTP → Jetty → Sa-Token 拦截器
  ├─ 命中 security.excludes → 匿名放行（仅 /blog/public/**）
  └─ 否则校验 token/clientid → 解析 LoginUser（userId/deptId/roles）
      → Controller（参数校验分组 / @SaCheckPermission / @Log / @RepeatSubmit）
      → Service（业务规则、状态转换、@Transactional 边界）
      → BaseMapperPlus / QueryWrapper（PlusDataPermissionInterceptor 可注入数据权限 SQL）
      → MySQL；Redis 用于缓存、验证码、分布式锁；审计写 sys_oper_log
响应：R<T> 统一包装，分页 PageResult{rows,total}；VO 字段可经 @Translation 翻译、脱敏注解处理

搜索链路（S16）：/blog/public/search → ArticleSearchService（engine=es|mysql 切换）
  ├─ ES 实现：multi_match（IK 分词，title^3）→ 命中 id 打分序 → 回表 MySQL（selectPublicPage 三条件内建）
  │          → 按 ES 序重排 + 合并服务端高亮（<em>，D36）+ ArticleTagFiller 填 tags
  │          └─ ES 异常 → 降级 MySQL LIKE（响应无 highlight/nextAfter）
  └─ MySQL 实现：三字段 OR LIKE（兜底 + 对比基线）

索引同步链路（S16）：文章 8 个写方法成功后发 ArticleChangedEvent（事务 AFTER_COMMIT）
  → ArticleIndexService.indexOrRemove：按 DB 现状重推导（公共可见三条件内建于索引生命周期）
  → upsert / 删除 ES 文档；失败仅记日志，由定时对账（MySQL 公共文章集合 vs ES 文档集合 diff）兜底
```

## 4. 博客域模型（简图）

```text
sys_user 1─0..1 blog_user_profile        （头像/昵称实时取 sys_user，不复制）
sys_user 1─N  sd_blog_article 1─N sd_blog_article_version（append-only，current_version_id 指向生效版本）
sd_blog_article N─M sd_blog_tag via sd_blog_article_tag（1~6 个/篇）
sd_blog_tag 1─N sd_blog_tag_alias（旧 slug → canonical，合并链必须展平）
sd_blog_article / sys_user 1─N sd_blog_comment（一级，parent_id 恒 NULL）
```

字段、索引、状态机全图见 `database-design-v0.1.md` §5~§15，本文不重复。

## 5. 部署拓扑

### 5.1 生产形态（S25 起，一键启动）

```text
docker-compose.yml（script/docker/）
├── blog-mysql   mysql:8.4.9            :3306   库 ry-vue（字符集/lower_case 由 mysql/my.cnf）
├── blog-redis   redis:8.6.3            :6379   requirepass 见 .env
├── blog-minio   pgsty/minio            :9000/:9001  OSS（RuoYi oss 配置指向它）
├── blog-es      自建 blog-es:7.17.28-ik :9200  ES 7.17 + IK + tech.dic；512m 堆
├── blog-backend 自建 sd-backend        :8081   数据源 → mysql:3306；Redis → redis:6379；ES → es:9200
│                                               全部环境变量注入，镜像内不含密钥
├── blog-web     自建 blog-web          :3000   NUXT_API_BASE → http://backend:8081
└── blog-admin   自建 sd-admin          :8080   nginx；/prod-api/* → http://backend:8081/
```

容器间用**服务名**互访（无 `host.docker.internal`）。启动顺序由 healthcheck 强制：
`mysql/redis/minio` 健康 → `backend` 启动 → 健康 → `web`/`admin` 启动；ES 用 `service_started`
（启动慢，且搜索异常会自动降级 MySQL，不必阻塞）。

生产形态不挂源码、密钥走 `.env`（`env_file`）、镜像固定 tag。

### 5.2 开发形态

```text
docker-compose.dev.yml（script/docker/）
├── 四个中间件（同 5.1，默认 `up -d` 只起这些）
└── backend / web / admin —— profile: app 才启动，端口 18081 / 3100 / 8080
本机（WSL2）
├── DromaraApplication（ruoyi-admin，dev profile，:8081）← 与容器 backend 二选一
├── blog-web `npm run dev`（:3000）
└── IDEA / Maven 3.9.x / JDK 21（sdkman）
```

### 5.3 CI / CD 现状

三仓各有 `.github/workflows/ci.yml`（构建级：后端 `mvn compile`、两个前端 `install + build`）。
**自动部署是唯一依赖目标服务器的一环**，S25 明确列为非目标；镜像构建与一键编排已就绪（见 S25）。

## 6. 扩展边界（预留不改契约）

| 未来能力 | 预留方式 |
|---|---|
| 搜索换 Elasticsearch | ✅ 已落地（S16）：`ArticleSearchService` 双实现 + `blog.search.engine` 切换 |
| 阅读量去重/异步 | ✅ 已落地（S15）：计数点收敛在详情接口一处（D35） |
| 版本恢复/差异 | Version append-only 模型天然支持（PRD §8.5） |
| 旧 URL 301 | 后端只负责 alias→canonical 解析，301 归前台/边缘层（契约 §7.3） |
| Markdown 编辑器 | 正文只约定为"净化后的 HTML 快照"，来源不限 |
