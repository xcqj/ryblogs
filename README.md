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
./gen-secrets.sh              # 生成 JWT 密钥与两对接口加密密钥，写入 .env（幂等，已有值不覆盖）
docker compose up -d --build
```

应用密钥**不入库**：`jwt-secret-key` 与 `api-decrypt` 公私钥在 `application*.yml` 里只留 `${ENV}` 占位，缺失时后端启动直接失败，不会回落到默认值。

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
2. 运行 `./gen-secrets.sh`（若尚无 `.env`），并在 IDEA 运行配置的 Environment variables 里注入
   `.env` 中的 `JWT_SECRET_KEY` / `API_DECRYPT_PRIVATE_KEY` / `API_DECRYPT_PUBLIC_KEY`
   —— 缺失时 `CryptoFilter` / Sa-Token 会在启动阶段直接失败，这是刻意的 fail-fast
3. IDEA 启动 `DromaraApplication`（dev profile），地址 http://localhost:8081
4. Swagger：http://localhost:8081/swagger-ui/index.html

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
| `JWT_SECRET_KEY` | Sa-Token 签名密钥 | **无默认值**，由 `gen-secrets.sh` 生成 |
| `API_DECRYPT_PRIVATE_KEY` / `API_DECRYPT_PUBLIC_KEY` | 接口加密密钥对（私钥解密请求、公钥加密响应） | **无默认值**，由 `gen-secrets.sh` 生成 |
| `NUXT_RSA_PUBLIC_KEY` / `NUXT_RSA_PRIVATE_KEY` | 上面那对密钥的「前端那一半」，读者站运行时读取、管理后台构建期内联 | 由 `gen-secrets.sh` 自动填写 |

> 换密钥要同时做三件事：重跑 `gen-secrets.sh` 前先清空 `.env` 里对应值 → 重建后端与管理后台镜像（后台是构建期注入）→ 同步管理端仓库的 GitHub Actions Secrets（`VITE_APP_RSA_PUBLIC_KEY` / `VITE_APP_RSA_PRIVATE_KEY`）。
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

## 演示数据（S26）

库里默认是空的。两级种子任选，装载后立刻有内容：

```bash
docker compose --profile seed run --rm seed                            # min：8 篇，开发用
SEED_FILE=seed-demo.sql docker compose --profile seed run --rm seed    # demo：52 篇，演示用
```

| 文件 | 规模 | 用途 |
|---|---|---|
| `script/sql/seed/seed-min.sql` | 3 作者 / 8 篇 / 30 评论 / 10 标签 | 本地开发，覆盖有/无封面、有/无代码块、多/单标签、草稿/发布/隐藏/下线四种状态 |
| `script/sql/seed/seed-demo.sql` | 5 作者 / **52 篇** / 200 评论 / 20 标签 | 演示 / 面试，首页翻 5 页、标签云有长尾 |

**几个特性**

- **幂等**：种子先删 `22xx` 段再插入，可反复执行；不会碰框架 ID（`17xx`）与真实数据（`21xx`）
- **代码块带真实高亮**：生成时就跑过 shiki，打开详情页即彩色
- **封面克制**：约 10 篇有封面（复用 MinIO 现有图），其余走无封面降级版式
- **时间与关联自洽**：发布时间分散在近 90 天，评论时间全部晚于文章

**演示账号**：`chenyu` / `linshen` / `suwan` / `zhoumu` / `hexi`，密码统一 `admin123`。
⚠️ 仅演示用，生产禁止沿用。

### 三个 `.mjs` 是什么（种子源码）

两个 `.sql` 是**产物**，`blog-web/tooling/` 下的三个文件是**源码**：

| 文件 | 作用 |
|---|---|
| `tooling/seed/data-min.mjs` | 8 篇内容定义（标题 / 摘要 / 标签 / 代码块 / 状态 / 时间偏移） |
| `tooling/seed/data-demo.mjs` | 扩展 44 篇，与 min 合并成 52 篇 |
| `tooling/gen-demo-seed.mjs` | 跑一遍 shiki 把代码块着色，再序列化成 SQL |

**只装载不改内容时，这三个文件完全不用管。**

为什么不能只留 SQL：正文里的 `<span class="tok-1itgoe">` 是 shiki 算出来的哈希，对应「关键字」这一种颜色，手写编不出来。要改内容就必须走生成器重算一遍：

```bash
cd ../blog-web
node tooling/gen-demo-seed.mjs            # 重新生成两个 SQL
node tooling/gen-demo-seed.mjs --check    # 只看规模，不写文件
```

### 清理历史脏数据（手动、不可撤销）

`script/sql/cleanup-legacy-test-data.sql` 用于清掉 `S3测试` / `32131aaa` 这类开发期数据，只删 `22xx` 段之外的博客数据，`admin` 等系统账号不受影响。

**为什么没做成 compose 服务**：种子幂等、跑错重来即可；清理不可撤销，一键化太危险。

四步：

```bash
cd RuoYi-Vue-Plus

# ① 备份（必做）
docker exec blog-mysql mysqldump -uroot -proot --single-transaction \
  --default-character-set=utf8mb4 --databases ry-vue > ~/backup-ry-vue-$(date +%F).sql

# ② 预览（只读，安全）——输出各类行数与 20 条标题样本
docker exec -i blog-mysql mysql -uroot -proot --default-character-set=utf8mb4 ry-vue \
  < script/sql/cleanup-legacy-test-data.sql

# ③ 确认无误后，编辑该文件，去掉「步骤 2」整段的 `--` 注释，再执行同一条命令

# ④ 核对（应等于种子规模）
docker exec blog-mysql mysql -uroot -proot -N -e "
SELECT '文章', COUNT(*) FROM \`ry-vue\`.sd_blog_article
UNION ALL SELECT '版本', COUNT(*) FROM \`ry-vue\`.sd_blog_article_version
UNION ALL SELECT '评论', COUNT(*) FROM \`ry-vue\`.sd_blog_comment;"
```

> ⚠️ **清理必须按各表「自身主键」判断，不能按 `article_id` 之类的外键列判断。**
> 实测踩过：库里有一条 `version_id` 在 `21xx` 段、但 `article_id = 9000000000000020024` 的孤儿版本
> （对应文章早已删除），按 `article_id` 过滤会漏掉它，导致「版本 53 / 文章 52」对不上。
> 脚本里三张表已改为 `version_id` / `article_tag_id` / `tag_alias_id`。

## 常见问题

**端口被占用**：改 `.env` 里的 `BACKEND_PORT` / `WEB_PORT` / `ADMIN_PORT`，容器内端口不用动。

**ES 起不来**：宿主机需 `sysctl -w vm.max_map_count=262144`（写入 `/etc/sysctl.conf` 永久生效）。

**上传图片后前台 404**：MinIO 地址存在**数据库表** `sys_oss_config.domain_url`（初始值 `127.0.0.1:9000`），不在配置文件里。本机演示时浏览器能直连 `127.0.0.1:9000`；**部署到远程服务器必须改这张表**，改成外部可达的地址。

**内存不够**：整套约需 3~4GB（ES 512m + 后端 1G + 两个前端 + MySQL）。可把 `ES_JAVA_OPTS` 降到 256m，或在 compose 里去掉 es 并把 `blog.search.engine` 改成 `mysql`（LIKE 搜索，功能可用但无中文分词）。

**改了源码镜像没更新**：生产形态不挂源码，必须 `docker compose up -d --build` 显式重建。

## 致谢

基于 [RuoYi-Vue-Plus](https://gitee.com/dromara/RuoYi-Vue-Plus)（Apache-2.0）构建，保留其权限、代码生成、监控等基础能力。
