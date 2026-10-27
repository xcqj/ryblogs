# Stack Depth · 博客后端

多作者技术博客的服务端。在 RuoYi-Vue-Plus 6.x 基础上新增 `sd-blog` 业务模块，提供文章 / 版本 / 标签 / 评论 / 用户资料 / 搜索 / 治理七组能力。

> 前端两个仓库与本文档同级：`blog-web`（读者站 + 作者中心）、`plus-ui`（管理后台）。
> 定位、范围与决策记录见 [`docs/ROADMAP.md`](docs/ROADMAP.md)，接口契约见 [`docs/blog-api.md`](docs/blog-api.md)。

## 三仓关系

```
                    ┌──────────────────────┐
   读者/作者 ──────▶│ blog-web  Nuxt 4 SSR │──┐
                    │ :3000                │  │  Nitro 代理注入 token
                    └──────────────────────┘  │
                                              ▼
                                    ┌───────────────────┐
   管理员 ─────────▶│ plus-ui  Vue3+EP  │──▶ │ ruoyi-admin :8081 │
                    │ :80（nginx 反代）  │    │  + sd-blog 模块    │
                    └───────────────────┘    └─────────┬─────────┘
                                                       │
                                    ┌──────────┬───────┴────┬────────┐
                                    ▼          ▼            ▼        ▼
                                 MySQL       Redis        MinIO     ES
                                  :3306      :6379       :9000    :9200
```

三个仓库**必须同级 clone**——compose 的 `build.context` 跨仓引用（`../../blog-web`、`../../../plus-ui`）。

## 一键启动（推荐）

```bash
cd script/docker
cp .env.example .env          # 至少改掉 MYSQL_ROOT_PASSWORD / REDIS_PASSWORD / MINIO_ROOT_PASSWORD
docker compose up -d --build
```

七个服务全部起来后：

| 入口 | 地址 | 说明 |
|---|---|---|
| 前台 | http://localhost:3000 | 读者站 |
| 后台 | http://localhost:8080 | 管理后台，默认账号 `admin` / `admin123` |
| 后端 | http://localhost:8081 | 接口与 Swagger |
| MinIO 控制台 | http://localhost:9001 | `ruoyi` / `ruoyi123` |

首次数分钟（要下 Maven 依赖 + 构建三个镜像）；之后 `up -d` 十秒级。

**启动顺序**由 healthcheck 强制：MySQL / Redis / MinIO 健康 → 后端启动 → 健康 → 前台与后台启动。ES 启动慢且搜索会自动降级到 MySQL，因此只等「已启动」不等「健康」。

**数据库初始化**：MySQL 数据卷为空时，按文件名前缀依次执行 7 个脚本：

| 顺序 | 文件 | 作用 |
|---|---|---|
| 01 | `script/sql/ry_vue.sql` | 核心表 + 菜单 + 默认数据（`sys_client` 只有 `pc`/`app`） |
| 02 | `docs/blog_schema_v0.1.sql` | 博客 7 张表（已含"无 del_flag"的最终结构） |
| 03 | `docs/blog_s9_web_client.sql` | **`web` 客户端**——前台登录必需，`ry_vue.sql` 里没有 |
| 10~13 | `docs/blog_s5/s6/s7/s10_menu.sql` | 后台治理菜单（自带 DELETE 守卫，幂等可重跑） |

**不导入**：`ry_workflow` / `ry_job` / `ry_ai`（独立服务模块，D10）；`blog_s2_migration.sql`（结构已是最新）；`blog_s0/s3/s16_cleanup.sql`（清理用）；`blog_s16_demo_data.sql`（演示数据，归 S26）；`script/sql/blog*Menu.sql`（**另一套菜单 id**，与 10~13 不是同一套，同时导入会出现重复菜单）。

字符集由 [`script/docker/mysql/my.cnf`](script/docker/mysql/my.cnf) 提供：initdb 调用的 `mysql` 客户端默认 `latin1`，会把中文按字节拆开、撑爆 `varchar(30)` 并中止初始化（`ERROR 1406`）。该文件同时固定 `lower_case_table_names=1`。

**验收登录**：验证码存在 Redis，key 带 `global:` 前缀（不是 `captcha_codes:`）：

```bash
# 登录页加载时请求 /auth/code 拿到 uuid，再用 uuid 读答案
docker exec blog-redis redis-cli -a ruoyi123 --no-auth-warning \
  GET "global:captcha_codes:<uuid>"
```

实测（2026-10-28，**全新数据卷**）：admin / admin123 + 该验证码 → 跳转 `/index`，localStorage 出现 `Admin-Token`，全链路涵盖 RSA 请求加密、`client_id` 校验、BCrypt 密码与验证码。

## 本地开发

不容器化应用、只在 IDE 里跑后端时：

```bash
cd script/docker
docker compose -f docker-compose.dev.yml up -d      # 只起 4 个中间件
```

然后：

1. 按 [`docs/SETUP.md`](docs/SETUP.md) §3 导入两个 SQL
2. IDEA 启动 `DromaraApplication`（dev profile），地址 http://localhost:8081
3. Swagger：http://localhost:8081/swagger-ui/index.html

需要整体联调（不想开三个终端）时：

```bash
docker compose -f docker-compose.dev.yml --profile app up -d --build
```

dev 形态把后端映射到 **18081**（避开 IDE 占用的 8081）、前台 3100、后台 8080。

## 环境变量

生产形态全部通过环境变量注入，镜像内不含任何密钥。完整清单见 [`script/docker/.env.example`](script/docker/.env.example)。

| 变量 | 作用 | 默认 |
|---|---|---|
| `SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_URL` | 数据源（dynamic-datasource 嵌套键，relaxed binding 覆盖） | `jdbc:mysql://mysql:3306/ry-vue` |
| `SPRING_DATA_REDIS_HOST` / `_PASSWORD` | Redis 地址与密码 | `redis` / `ruoyi123` |
| `BLOG_ES_ADDRESS` | ES 地址（自定义键 `blog.es.address`，**不是**标准 `spring.elasticsearch`） | `es:9200` |
| `JAVA_OPTS` | 堆大小 | `-Xms512m -Xmx1024m` |
| `MYSQL_ROOT_PASSWORD` / `REDIS_PASSWORD` / `MINIO_ROOT_PASSWORD` | 中间件凭据 | 见 `.env.example` |

## 目录结构

```
ruoyi-admin/          启动模块（DromaraApplication，打出的 jar 就是镜像入口）
ruoyi-api/            跨模块接口（OssService / UserService 等）
ruoyi-common/         公共能力（core / mybatis / redis / security ...）
ruoyi-extend/         扩展模块
ruoyi-modules/
  ├── sd-blog/        ★ 博客业务（controller / service / mapper / domain）
  ├── ruoyi-system/   系统管理（含 blog 域上传接口 BlogOssController）
  └── ruoyi-*/        demo / gen / job / workflow / ai（博客未使用）
script/
  ├── sql/            建表与菜单脚本
  └── docker/         compose（prod / dev）、es 镜像、.env.example、smoke.sh
docs/                 ROADMAP / 契约 / 架构 / 部署说明
Dockerfile            后端生产镜像（多阶段，BuildKit 缓存 Maven 依赖）
```

## 常见问题

**端口被占用**：改 `.env` 里的 `BACKEND_PORT` / `WEB_PORT` / `ADMIN_PORT`，容器内端口不用动。

**ES 起不来**：宿主机需 `sysctl -w vm.max_map_count=262144`（写入 `/etc/sysctl.conf` 永久生效）。

**上传图片后前台 404**：MinIO 地址存在**数据库表** `sys_oss_config.domain_url`（初始值 `127.0.0.1:9000`），不在配置文件里。本机演示时浏览器能直连 `127.0.0.1:9000`；**部署到远程服务器必须改这张表**，改成外部可达的地址。

**内存不够**：整套约需 3~4GB（ES 512m + 后端 1G + 两个前端 + MySQL）。可把 `ES_JAVA_OPTS` 降到 256m，或在 compose 里去掉 es 并把 `blog.search.engine` 改成 `mysql`（LIKE 搜索，功能可用但无中文分词）。

**改了源码镜像没更新**：生产形态不挂源码，必须 `docker compose up -d --build` 显式重建。

## 致谢

基于 [RuoYi-Vue-Plus](https://gitee.com/dromara/RuoYi-Vue-Plus)（Apache-2.0）构建，保留其权限、代码生成、监控等基础能力。
