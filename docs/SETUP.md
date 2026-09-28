# 开发环境搭建（SETUP）

> 状态：v0.1 · 2026-09-28
> 范围：仅后端仓库（前端 PLUS-UI 仓库自己管理自己的环境文档）。
> 凭据不在此复制，一律以 `script/docker/docker-compose.dev.yml` 与 `application-dev.yml` 实际内容为准。

## 1. 前置工具链

| 工具 | 版本 | 说明 |
|---|---|---|
| OS | WSL2 Ubuntu | Java/Maven/Docker 均在 WSL2 内运行 |
| JDK | 21（Temurin，sdkman 管理） | `java -version` 应为 21.x |
| Maven | 3.9.x（sdkman）或 `./mvnw` | IDEA 的 Maven home 必须与命令行一致，见 §6 |
| Docker | 任意近期版本 | 跑 MySQL/Redis/MinIO |
| Git | — | 每个切片一个 commit（AGENTS.md §10） |

## 2. 基础设施容器

```bash
cd script/docker
docker compose -f docker-compose.dev.yml up -d
```

三个容器（详细配置与密码见 compose 文件）：

| 容器 | 镜像 | 端口 | 用途 |
|---|---|---|---|
| blog-mysql | mysql:8.4.9 | 3306 | 主库，compose 已自动建库 `ry-vue`（utf8mb4、`lower_case_table_names=1`） |
| blog-redis | redis:8.6.3 | 6379 | 缓存/会话/验证码，带 requirepass |
| blog-minio | minio | 9000 / 9001 | OSS 兼容对象存储，控制台 :9001 |

> ⚠️ `lower_case_table_names=1` 必须在卷首次初始化时生效（已在 compose command 中）；已有数据卷改此参数无效。

## 3. 数据库初始化（最小集，就这两个文件）

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

- [ ] `docker ps` 三容器 Up
- [ ] `mysql> show tables like 'sd_blog_%';` 返回 7 行
- [ ] `mvn compile -pl ruoyi-admin -am` 全绿
- [ ] 应用启动无 ERROR，`/swagger-ui/index.html` 可见「博客模块」分组
- [ ] 默认管理员登录成功，`GET /blog/blogTag/list`（生成的临时 CRUD）返回 200
- [ ] 参数管理里 `sys.account.registerUser` 状态与当前切片需要一致
