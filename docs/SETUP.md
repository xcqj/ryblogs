# 开发环境搭建（SETUP）

> 状态：v0.3 · 2026-10-06
> 范围：仅后端仓库（前端 PLUS-UI 仓库自己管理自己的环境文档）。各仓库的**同级目录布局前提与多根工作区**属共同前提，记在 §10（MVP 期共 2 个仓库，已规划的后续仓库见 §10.4）。
> 凭据不在此复制，一律以 `script/docker/docker-compose.dev.yml` 与 `application-dev.yml` 实际内容为准。

## 1. 前置工具链

| 工具 | 版本 | 说明 |
|---|---|---|
| OS | WSL2 Ubuntu | Java/Maven/Docker 均在 WSL2 内运行 |
| JDK | 21（Temurin，sdkman 管理） | `java -version` 应为 21.x |
| Maven | 3.9.x（sdkman）或 `./mvnw` | IDEA 的 Maven home 必须与命令行一致，见 §6 |
| Docker | 任意近期版本 | 跑 MySQL/Redis/MinIO |
| Git | — | 每个切片一个 commit（AGENTS.md §10） |

## 2. 容器与一键启动

### 2.1 一键起全套（S25 起，推荐）

```bash
cd script/docker
cp .env.example .env          # 至少改 MYSQL_ROOT_PASSWORD / REDIS_PASSWORD / MINIO_ROOT_PASSWORD
docker compose up -d --build
```

七个服务，启动顺序由 healthcheck 强制（中间件健康 → 后端健康 → 前台/后台）：

| 服务 | 镜像 | 宿主机端口 | 说明 |
|---|---|---|---|
| mysql | mysql:8.4.9 | 3306 | 主库 `ry-vue`；字符集与 `lower_case_table_names=1` 由 `mysql/my.cnf` 提供 |
| redis | redis:8.6.3 | 6379 | 缓存/会话/验证码，带 requirepass |
| minio | pgsty/minio | 9000 / 9001 | OSS 兼容存储，控制台 :9001 |
| es | 自建 `blog-es:7.17.28-ik` | 9200 | ES 7.17 + IK 分词；需宿主机 `vm.max_map_count ≥ 262144` |
| backend | 自建 `sd-backend` | 8081 | 后端 jar；数据源/Redis/ES 地址全部由环境变量注入 |
| web | 自建 `blog-web` | 3000 | Nuxt SSR；`NUXT_API_BASE=http://backend:8081` |
| admin | 自建 `sd-admin` | 8080 | nginx + 静态产物；`/prod-api` 反代到 backend |

> ⚠️ **本节是开发形态**。部署形态（WSL 演示栈：分层 compose 去 build、端口 19xxx、绝对路径数据目录、与本节开发栈**并存**）由 S29.1 提供，方案见 `docs/cloud-deployment.md`。在该切片落地前，不要把本节端口直接接隧道或当作发布环境。

### 2.2 只起中间件（本机日常开发）

```bash
cd script/docker
docker compose -f docker-compose.dev.yml up -d                  # 默认只起四个中间件
docker compose -f docker-compose.dev.yml --profile app up -d --build   # 需要容器化联调时
```

dev 形态把后端映射到 **18081**（避开 IDE 占用的 8081）、前台 3100、后台 8080。

> ⚠️ `lower_case_table_names=1` 必须在卷首次初始化时生效（已由 `mysql/my.cnf` 提供）；已有数据卷改此参数无效，需重建卷。

## 3. 数据库初始化（手工开发的最小集，就这两个文件）

> **用 compose 一键启动时不需要手工导入**——initdb 会自动执行 7 个脚本（`ry_vue` / `blog_schema_v0.1` / `blog_s9_web_client` / 四个菜单脚本），清单与"为什么不能导哪些"见仓库根 `README.md` 的「数据库初始化」一节。
> 本节只适用于「中间件用容器、后端在 IDE 里跑」的手工路径。

按顺序导入到 `ry-vue` 库：

```bash
# ① RuoYi 核心（表 + 菜单 + 字典 + 默认数据）
mysql -uroot -p ry-vue < script/sql/ry_vue.sql

# ② 博客业务表（sd_blog_ 前缀 7 张表）
mysql -uroot -p ry-vue < docs/blog_schema_v0.1.sql
```

明确**不导入**：`ry_workflow.sql`、`ry_job.sql`、`ry_ai.sql`——分别属于工作流/任务调度/AI 三个独立服务模块，博客 MVP 不使用（决策见 ROADMAP §6 D10）。

## 4. 后端启动

1. 确认 `application-dev.yml` 数据源指向 `localhost:3306/ry-vue`、Redis 密码与 compose 一致。
2. 启动 `DromaraApplication`（dev profile 默认激活），服务地址：**http://localhost:8081**（context-path `/`）。
3. 接口文档：`http://localhost:8081/swagger-ui/index.html`（springdoc 分组见 §5）。

## 5. 博客模块的两处必配项

1. **SpringDoc 分组**：`application.yml` → `springdoc.group-configs` 追加
   `- group: 6.博客模块` / `packages-to-scan: org.dromara.blog`，否则博客接口不出现在文档里。
2. **开放注册**（S1 起需要）：后台「系统管理 → 参数管理」将 `sys.account.registerUser` 置为 `true`（框架自带开关，`POST /auth/register` 读取该参数，零代码改动；验证码跟随全局 `captcha.enable=true`）。

## 6. IDEA 环境对齐（新机器必做）

IDEA 默认用 Bundled Maven，与命令行 sdkman Maven 不同源会出现"命令行编译通过、IDEA 报未解析插件/找不到符号"：

1. `Settings → Build Tools → Maven → Maven home path`：选 sdkman 的 Maven（如 `~/.sdkman/candidates/maven/current`）。
2. `User settings file`：确认指向 `~/.m2/settings.xml`（含华为云镜像）；`Local repository`：`~/.m2/repository`。
3. 不勾选 `Work offline`。
4. Maven 面板 → Reload All Maven Projects；仍有幻影错误 `File → Invalidate Caches / Restart`。

## 7. 已验证的坑（务必先读）

完整清单见 `AGENTS.md` §7（代码生成器）与 §8（构建排障），此处摘要最高频的三条：

1. `cannot find converter` 运行时报错 ≠ 代码问题：mapstruct-plus 生成类缺失，`mvn clean` 全量重编 + IDEA Rebuild。
2. 代码生成器导入表：`packageName=org.dromara.blog`、`moduleName=blog`、**主键列必须勾"查询"**，否则 BO 缺主键编译失败。
3. 复制模块目录永远不带 `target/` 与 `.flattened-pom.xml`。

## 8. Apifox 联调约定

- Apifox 装在 **Windows 侧即可**，无需在 WSL2 内安装：WSL2 默认 NAT 模式自带 Windows→WSL 的 localhost 端口转发。
- Base URL：`http://localhost:8081`（直连，无代理）；若个别环境转发失效，应急改用 WSL 内 `hostname -I` 得到的 IP。
- 登录接口带 `clientid` 请求头（RuoYi 多端认证要求），Token 放 `Authorization: Bearer ...`。
- 每个切片的用例五类齐全：正向 / 鉴权失败 / 参数校验 / 资源不存在 / 越权（ROADMAP §5）。
- 注意 `POST /auth/register` 带 `@ApiEncrypt`：若开启接口加密（`api-encrypt.enable`），Apifox 直测需按加密链路构造或临时关闭。

## 9. 冒烟验收清单（新环境搭建完成的定义）

- [ ] 所需仓库按 §10 同级布局 clone 完成（MVP 期 = `RuoYi-Vue-Plus` + `plus-ui`），多根工作区可打开且 `../plus-ui` 可达
- [ ] `docker ps` 四容器 Up（mysql / redis / minio / es）
- [ ] `mysql> show tables like 'sd_blog_%';` 返回 7 行
- [ ] `mvn compile -pl ruoyi-admin -am` 全绿
- [ ] 应用启动无 ERROR，`/swagger-ui/index.html` 可见「博客模块」分组
- [ ] 默认管理员登录成功，`GET /blog/blogTag/list`（生成的临时 CRUD）返回 200
- [ ] 参数管理里 `sys.account.registerUser` 状态与当前切片需要一致

## 10. 多仓库同级布局与多根工作区

### 10.1 必须同级 clone（硬性前提）

后端与各前端仓库是**各自独立的 Git 仓库**，交叉引用全部走相对路径（如 `plus-ui/AGENTS.md` 指向 `../RuoYi-Vue-Plus/docs/blog-api.md`）。
因此它们必须是**同一父目录下的兄弟目录**，布局不对则跨仓库引用直接失效（三个业务仓 + 一个只读文档仓，按下面四个 clone）：

```bash
mkdir -p ~/projects/sd-blog && cd ~/projects/sd-blog
git clone git@github.com:xcqj/ryblogs.git         RuoYi-Vue-Plus
git clone git@github.com:xcqj/ryblogs-ui.git      plus-ui
git clone git@github.com:xcqj/blogs-web.git       blog-web
git clone git@github.com:xcqj/blogs-overview.git  overview   # 纯文档仓，可选（D47）
```

| 本地目录 | origin | 当前分支 | upstream（跟官方） |
|---|---|---|---|
| `RuoYi-Vue-Plus` | `github.com:xcqj/ryblogs` | `6.X` | `gitee.com/dromara/RuoYi-Vue-Plus` |
| `plus-ui` | `github.com:xcqj/ryblogs-ui` | `6.X-Vue` | `gitee.com/JavaLionLi/plus-ui` |
| `blog-web` | `github.com:xcqj/blogs-web` | `main` | —（自建仓，不跟上游） |
| `overview` | `github.com:xcqj/blogs-overview` | `main` | —（自建纯文档仓，**Public**，不参与构建） |

### 10.2 工作区文件

**唯一权威副本：`tooling/blog.code-workspace`（随本仓库版本化）。**

打开方式：IDE 菜单 `File → Open Workspace from File…`，选中该文件；当前四个 folder 显示为
`backend · RuoYi-Vue-Plus`、`admin · plus-ui`、`web · blog-web`、`overview · blogs-overview`（新增仓库时按 §10.4 追加）。
其中 `overview` 是**公开的项目总览文档仓**（D47），只提供仓库与演示导航，**不参与构建、不进 CI、不被 compose 引用**：
动它不会触发任何业务构建，反之亦然。

- `folders[].path` 是**相对该 workspace 文件所在目录**解析的（本文件在 `tooling/` 下，故为 `..` 与 `../../plus-ui`），
  且要求 §10.1 的同级布局。换布局就改这些路径，新增仓库（前端或文档仓）则在此**追加 folder**（§10.4），不要在别处再存第二份副本 —— 多份副本必然漂移。
- 文件内另带 `search.exclude`（排 `**/target`、`**/node_modules`、`**/dist`、`*.log`、`*.log.gz`）与
  `files.watcherExclude`（同族 + `**/logs/**`），只是 IDE 检索/监听体验配置，**不影响构建与 `.gitignore` 语义**。
- 采用严格 JSON（无注释），便于任何工具直接解析；配置理由记在本节，不写进配置文件。

### 10.3 为什么没有 meta repo

父目录（`sd-blog/`）**不是** Git 仓库，这是有意决定而非遗漏：各仓库各自独立提交、独立分支、独立上游，
一个后端切片通常只动 `RuoYi-Vue-Plus`，后台切片只动 `plus-ui`，前端之间无交叉代码依赖，不需要原子提交，也就不需要 meta repo 或 git submodule。
治理内容（AGENTS.md、`.codex`/`.claude`/`.qoder` skills 链接、docs 权威文档）全部版本化在各仓库内；
跨仓库共享物只有这一个 workspace 文件，**由后端仓库托管**——因为后端是所有前端的公共依赖，它不会依赖任何前端而存在。

> ⚠️ 由此带来一条约束：**不要在父目录执行 `mvn`**。那会在父目录落下无人管理的游离 `target/`（mapstruct-plus 编译期产物），
> 而父目录不受任何 `.gitignore` 保护，会长期残留成噪音。构建一律在 `RuoYi-Vue-Plus/` 目录内执行（`./mvnw` 或 `mvn`）。此约束已升格为 `AGENTS.md` §8 构建红线。

### 10.4 新增前端仓库时的约定（PC 博客前台已确认待建仓；移动端已立项 S11、MVP 后启动）

后端 API 已按三层边界设计（契约 §1：`/blog/public/**` 公共、`/blog/article|user/**` 登录用户、`/blog/admin/**` 管理），
**公共接口不区分客户端**，因此新增第 N 个前端不需要后端改接口。新增仓库时只需遵三条：

1. **同级 clone**（§10.1），向 §10.1 的仓库对照表补一行；
2. **向 `tooling/blog.code-workspace` 的 `folders` 追加一条** `"path": "../../<目录名>"`，仍然只保持一份权威副本；
3. **新仓库自带 `AGENTS.md`**：写清它用哪一层 API（PC 博客前台属公共 / 登录用户两层，不得调 `/blog/admin/**`），
   并声明到 `../RuoYi-Vue-Plus/docs/` 的权威源引用（照 `plus-ui/AGENTS.md` 的形式）。

各端与仓库登记表：

| 端 | 本地目录（仓库名） | 当前状态 | 依据 |
|---|---|---|---|
| 后端 | `RuoYi-Vue-Plus`（`ryblogs`） | ✅ 已存在，已验收至 S8 | ROADMAP §2 |
| 管理后台 | `plus-ui`（`ryblogs-ui`） | ✅ 已存在 | ROADMAP S1~S8 |
| PC 博客前台 | `blog-web`（`blogs-web`，2026-10-06 定名） | ✅ **已建仓并同级 clone（2026-10-06 S9 开工：Nuxt 4.5.2 脚手架 + Nitro 代理/cookie 认证 + 首页对接 §5.1/§7.1/§7.2 已跑通）** | ROADMAP S9 + D21；**范围=响应式（PC + 移动浏览器一套代码，D27）；技术栈=Nuxt 4 SSR（D28）**；依赖 S2+S6+S8，只调公共层 + 登录作者自助层（`/blog/article|user/**`）；作者中心（发文/编辑/自助治理自己的文章）在 blog-web（D29 修订 D24），平台治理仍属 plus-ui |
| 项目总览（文档） | `overview`（`blogs-overview`） | ✅ 已建仓并同级 clone（2026-11-11，D47）；**Public、纯文档** | 只做仓库 / 演示入口 / 技术方案导航，不含业务代码；**不参与构建、不进 CI、不被 compose 引用**；公开前必须过其 `SECURITY_REVIEW.md` 的四条红线（无 env/配置/脚本/SQL/日志、无账号口令、无个人路径与基础设施标识、措辞不越级） |
| 移动端 | —（仓库名待建仓时定） | 🔖 **已立项 S11，MVP 后启动**（2026-10-06 决定，D27；接回 D21 预留口子） | uni-app，目标平台微信小程序 / H5 / App；范围基线=读者套件（列表/详情/搜索/评论），无发文；复用公共接口零后端改动，详见 ROADMAP S11 |

> 客观事实备案：`script/sql/ry_vue.sql` 的 `sys_client` 已预置两条客户端记录——`pc`（clientKey `e5cd7e48...`，
> `plus-ui/.env.development` 的 `VITE_APP_CLIENT_ID` 即此值）与 `app`（clientKey `428a8310...`、`clientType=android`、
> `grantType=password,sms,social`）。纯浏览的公共页面走 `security.excludes` 匿名接口不需要 client；
> 前台一旦要登录 / 发文 / 评论，就得携 `clientid` 头（本文 §8），复用 `pc` 记录或按需新增一条；**D28 已定：blog-web 新建独立 `web` 记录**（clientKey `web`、grantType 仅 password，与 `pc` 会话隔离），SQL 见 `docs/blog_s9_web_client.sql`（幂等含验证段，待 S9 建仓时执行）；plus-ui 继续用 `pc` 不变；
> S11 移动端可直接复用既有 `app` 记录（`grantType=password,sms,social`，是否按短信/社交收缩其授权类型属 S11 开工时定，纯配置零代码，D27 备案）。

### 10.5 多 harness 的 Agent 编排层（D32）

接入新的 Agent 工具 / harness 时，**不要为每个工具复制一套资产，也不要把 §10.2 的工作区文件迁走**。三类东西各归其位：

| 概念 | 谁在用 | 位置 |
|---|---|---|
| IDE 多根工作区 | VS Code / Cursor 可视化同时打开三业务仓 + 总览文档仓 | `RuoYi-Vue-Plus/tooling/blog.code-workspace`（唯一权威副本，由本仓库托管） |
| Agent 会话根 | WorkBuddy 等 harness 的工作目录 | 父目录 `sd-blog/`（非 Git 仓库，见 §10.3） |
| 跨仓路由枢纽 | 会话根落在父目录时的入口 | `sd-blog/AGENTS.md`（hub，只做路由） |
| 各仓领域权威 | 单独打开某一个仓库时 | 该仓 `AGENTS.md` + `.codex/skills/` + `.claude/agents/` |

四条硬约束：

1. **hub 只做路由**：仓库地图 + 权威源指针 + 跨仓环境红线。**禁止复制子仓 `AGENTS.md` 里的规则正文**（D1「避免双源漂移」同一原则）；与子仓冲突时一律以子仓为准，并回头修订 hub。
2. **技能一律用转发 shim**：父目录 `.workbuddy/skills/<name>/SKILL.md` 只含 frontmatter + 必读清单 + 跨工具补充纪律，正文唯一存在于各仓 `.codex/skills/`。**禁止产出第二份 SKILL.md 实体**。`.qoder/skills` 沿用既有 symlink 口径**保留不删**（它只是历史 harness 的加载入口，非事实源）。
3. **hub 与 shim 不入库**：父目录不是 Git 仓库（§10.3 的有意取舍），代价是换机器需重建；因此其每条约定必须在 `docs/ROADMAP.md` §6（D32）与本仓库 `AGENTS.md` §3 / §11 **双向登记，登记缺失即视为该约定失效**。
4. **一个 Agent 会话只动一个仓库**：跨仓联调拆成多步、各自独立提交，避免单次改动横跨多个 Git 仓库而无法原子回滚。构建命令沿用 §10.3：`mvn` 只在 `RuoYi-Vue-Plus/` 内跑，`npm` / `pnpm` 只在对应前端仓目录内跑，**一律不在父目录执行**。

> 边界澄清：单独打开任一子仓（比如只开 `plus-ui`）时，该仓 `AGENTS.md` 已自洽；hub 只服务跨仓场景——两者互补而非替代，**不要把 hub 写成第四个仓库**。
