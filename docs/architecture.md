# 系统架构（architecture）

> 状态：v0.2 · 2026-10-03
> 定位：一张图 + 一条请求链 + 一套部署拓扑。只讲"系统长什么样"，不讲"业务是什么"。
> 业务规则见 PRD，接口见 blog-api.md，表结构见 database-design/schema，红线见 AGENTS.md——本文一律不重复。

## 1. 系统定位

```text
┌─────────────────────────── 前端（两个独立仓库，同级 clone）───────────────────────────┐
│   博客前台站 blog-web（读者/作者）  管理后台 plus-ui（作者 + 管理员共用）              │
│   首页/文章/标签/搜索/用户/作者中心  我的文章 / 博客治理菜单 / RuoYi 系统管理           │
└──────────────┬───────────────────────────────┬───────────────────────────────────────┘
               │ /blog/public/**（匿名）＋作者自助 │ /blog/article|user|admin/**、/auth/**、/system/**（Sa-Token）
               ▼                               ▼
┌──────────────────────────── 本仓库：RuoYi-Vue-Plus 6.X 模块化单体 ────────────────────────────┐
│  ruoyi-admin（唯一启动入口，聚合所有模块）                                                      │
│  ├── sd-blog（博客业务模块，包 org.dromara.blog）                                              │
│  ├── ruoyi-modules/*（system、gen、workflow、job、ai、demo）                                   │
│  └── ruoyi-common/*（mybatis、satoken、redis、oss、excel、log、web… 横切能力）                  │
└──────────┬──────────────────────────┬──────────────────────────┬──────────────────────────────┘
           ▼                          ▼                          ▼
      MySQL 8.4（主存储）        Redis 8.x（缓存/会话/限流）   OSS/MinIO（对象存储）
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
└── service / impl  业务规则、状态机、事务边界
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

## 5. 部署拓扑（开发环境）

```text
docker-compose.dev.yml（script/docker/）
├── blog-mysql   mysql:8.4.9    :3306   库 ry-vue（utf8mb4_general_ci, lower_case_table_names=1）
├── blog-redis   redis:8.6.3    :6379   requirepass 见 compose
└── blog-minio   minio          :9000/:9001  OSS 兼容存储（RuoYi oss 配置指向它）
本机（WSL2）
├── DromaraApplication（ruoyi-admin，dev profile，端口见 application.yml）
└── IDEA / Maven 3.9.x / JDK 21（sdkman）
```

生产拓扑与 CI/CD 属 MVP 后议题（PRD §15），暂不设计。

## 6. 扩展边界（预留不改契约）

| 未来能力 | 预留方式 |
|---|---|
| 搜索换 Elasticsearch | `ArticleSearchService` 抽象隔离（契约 §6） |
| 阅读量去重/异步 | 计数点收敛在详情接口一处（ROADMAP D8） |
| 版本恢复/差异 | Version append-only 模型天然支持（PRD §8.5） |
| 旧 URL 301 | 后端只负责 alias→canonical 解析，301 归前台/边缘层（契约 §7.3） |
| Markdown 编辑器 | 正文只约定为"净化后的 HTML 快照"，来源不限 |
