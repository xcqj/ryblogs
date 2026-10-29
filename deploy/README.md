# deploy —— 部署域（S29.1）

> 这里是**部署机唯一需要的东西**。部署机不 clone 源码、不编译、不持构建缓存，只解压部署包 + 拉镜像。
> 方案与决策见 `docs/cloud-deployment.md` 与 `docs/ROADMAP.md` §6 D45/D46。

## 1. 目录与职责

| 文件 | 职责 | 进部署包 |
|---|---|---|
| `compose.deps.yml` | 中间件（mysql / redis / minio / es），纯 `image:` | 是 |
| `compose.app.yml` | 应用（backend / web / admin），纯 `image:` | 是 |
| `compose.target-wsl.yml` | WSL 演示栈：项目名 `sdblog-demo`、19xxx 端口、内存上限 | 是 |
| `compose.target-cloud.yml` | 云目标占位（S29.2 填） | 是 |
| `compose.build.yml` | **仅本地**：四个 `build:`（含跨仓 context） | **否** |
| `deploy.sh` | 八命令 + 预检 + 摘要记录 + 互斥锁 | 是 |
| `stage-sql.sh` | 从 `script/sql` 与 `docs/` 暂存 7 个有序 SQL + `my.cnf` | 是 |
| `smoke.sh` | 栈内冒烟（不依赖固定容器名） | 是 |
| `smoke-public.sh` | 公网冒烟（两域名 / 反代 / sitemap） | 是 |
| `bundle.sh` | 产出 `dist/deploy-<版本>.tar.gz` | 是 |
| `redis/redis.conf` | AOF everysec（补 S15-D1 ④ 遗留） | 是 |
| `sql/`、`mysql/my.cnf` | 运行时暂存，**不入库** | 打包时带上 |

## 2. 端口（容器内永不变，宿主机只绑 127.0.0.1）

| 服务 | 容器内 | 宿主机（演示） |
|---|---|---|
| backend | 8081 | 19081 |
| admin | 80 | 19080 |
| web | 3000 | 19300 |
| mysql | 3306 | 13306 |
| redis | 6379 | 16379 |
| minio | 9000 / 9001 | 19000 / 19001 |
| es | 9200 | 19200 |

开发态（8081 / 3000 / 5173）与容器化开发（18081 / 3100 / 8080）不受影响，三段互不重叠。

> **Windows 浏览器打不开 `localhost:19300` 是预期行为**：WSL2 只把绑 `0.0.0.0` 的端口转发给 Windows。本地自检在 WSL 内 `curl`，肉眼验收走公网域名。

## 3. 首次部署

```bash
# ① 机器前置（需 sudo，一次性）
sudo sysctl -w vm.max_map_count=262144
echo 'vm.max_map_count=262144' | sudo tee /etc/sysctl.d/99-sdblog.conf   # WSL 重启会重置，必须持久化

# ② 私有镜像凭据（PAT 只需 read:packages；凭据落在运行部署命令的同一用户下）
docker login ghcr.io -u <github用户名>

# ③ 部署目录
tar xzf deploy-v0.1.0.tar.gz -C ~/sdblog
cd ~/sdblog/deploy-v0.1.0
cp .env.example .env && chmod 600 .env     # 填三个口令 + NUXT_* + OAuth
./deploy.sh up --version v0.1.0
./deploy.sh seed seed-demo.sql             # 可选：52 篇演示数据（会重建 ES 索引）
```

> **部署目录约定**：流水线使用固定目录 `~/sdblog/current`（可用环境变量 `SDBLOG_DEPLOY_DIR` 覆盖），
> 每次发布把部署包**覆盖**进去——`.env` 与 `.state` 不在包内，因此天然保留。
> 从仓库里手动跑时目录是 `RuoYi-Vue-Plus/deploy`。
> ⚠️ 两个目录不要各跑一次：`flock` 只能挡同机并发进程，挡不住两份 `.state` 分叉。
> 首次在流水线目录部署前，先把 `.env` 与 `.state` 复制过去（或直接 `cp .env.example .env` 重填）。

## 4. 命令

| 命令 | 说明 |
|---|---|
| `./deploy.sh up [--version vX.Y.Z] [--db-changed] [--no-smoke]` | 拉镜像 → 记摘要 → 起栈 → 等健康 → 冒烟 |
| `./deploy.sh rollback [--force]` | 按 `.state/previous` 的 digest 回上一组；`DB_CHANGED=true` 需 `--force` |
| `./deploy.sh adopt` | 接管已在跑的容器，只记录摘要、不重启 |
| `./deploy.sh down` | 停容器，保留数据与 `.state`（永不 `-v`） |
| `./deploy.sh seed [文件]` | 装载演示种子（默认 `seed-demo.sql`）并重建 ES 索引 |
| `./deploy.sh backup` | mysqldump + MinIO 目录打包，保留最近 `KEEP_BACKUPS` 份 |
| `./deploy.sh restore <备份目录> [--yes]` | 停应用 → 重建库 → 导入 → 恢复对象存储 → 起栈 → 计数校验 |
| `./deploy.sh status` | 容器 / 摘要 / 磁盘 / 最近备份 / 自检地址 |
| `./deploy.sh gc [--apply]` | 按 `ghcr.io/xcqj/sdblog-*` 前缀清理旧镜像（默认 dry-run） |

## 5. 数据目录与属主（bind mount 必读）

`DATA_DIR` 必须是**绝对路径**，默认 `~/sdblog/data`（不要放 `/mnt/c`）。

容器以各自镜像用户写数据，首次起栈若报 `Permission denied`，按下面改属主（用容器改，不需要 sudo）：

```bash
docker run --rm --user 0 -v "$DATA_DIR/mysql:/d" --entrypoint chown mysql:8.4.9 -R 999:999 /d
docker run --rm --user 0 -v "$DATA_DIR/redis:/d" --entrypoint chown redis:8.6.3 -R 999:999 /d
docker run --rm --user 0 -v "$DATA_DIR/es:/d"    --entrypoint chown <es镜像>  -R 1000:0  /d
# minio 默认以 root 运行，通常无需处理
```

## 6. 状态文件

`.state/current.env` / `.state/previous.env`（600，不入库）记录四枚镜像的 digest：

```bash
RELEASE_VERSION=v0.1.0
BACKEND_IMAGE=ghcr.io/xcqj/sdblog-backend@sha256:...
ADMIN_IMAGE=ghcr.io/xcqj/sdblog-admin@sha256:...
WEB_IMAGE=ghcr.io/xcqj/sdblog-web@sha256:...
ES_IMAGE=ghcr.io/xcqj/sdblog-es@sha256:...
DB_CHANGED=false
DEPLOYED_AT=2026-10-30T21:00:00+08:00
```

`DB_CHANGED=true` 的含义：本次发布改了表结构。回滚只回镜像、不回数据库，所以此时 `rollback` 需要 `--force`。

## 7. 备份与恢复口径

- 备份：MySQL（`--single-transaction --routines --triggers`）+ `${DATA_DIR}/minio` 目录
- **不备份 ES**：索引由 `ArticleIndexBootstrap` 在 alias 缺失时启动全量重建
- 恢复完成标准 = **行数一致**（`seed-demo` 基线 52 篇 / 52 版本 / 200 评论 / 20 标签 / 8 用户）+ 搜索可用 + 图片可访问

## 8. 常见报错

| 现象 | 原因 | 处置 |
|---|---|---|
| `pull` 401 | 未登录 GHCR 或 PAT 过期（90 天） | `docker login ghcr.io -u <用户名>` |
| 一键启动后库是空的 | initdb 报错中止（多为字符集） | 确认 `mysql/my.cnf` 已随包；看 `docker compose logs mysql` |
| ES 反复重启 | `vm.max_map_count` 不足 | §3 第 ① 步，并确认 `/etc/sysctl.d/99-sdblog.conf` |
| `Permission denied` 写数据目录 | bind mount 属主不符 | §5 |
| 端口被占 | 19xxx 已被别的进程/栈占用 | 改 `.env` 的端口变量，别改 compose |
| 后台 502 | 后端未健康 | `./deploy.sh status`；看 `docker compose logs backend` |
| 前台登录报「客户端不存在」 | 库里缺 `web` 客户端 | 确认 `sql/03-blog-web-client.sql` 已进 initdb |
| 公网访问 000 + `TLS handshake failure` | Hostname 是**两层子域**（如 `sdblog.us.ci.qdappt.us.ci`），Cloudflare 免费证书只覆盖「apex + 一层通配」 | 改成单层：`sdblog.<zone>` / `admin-sdblog.<zone>` / `img-sdblog.<zone>` |
| 本机自检随机返回 000、但浏览器正常 | shell 里有 `https_proxy`，代理对这些域名 TLS 抖动 | 自检一律 `curl --noproxy '*'`（`smoke-public.sh` 已内置） |
| 封面图 403（`application/xml`） | 桶是私有的；`mc` 拉不到时无法直接设策略 | OSS 配置 `access_policy=2` + 桶匿名下载；无 `mc` 时可用「复制已公开桶的 `.minio.sys/buckets/<bucket>` 元数据」实现（停 MinIO → 容器内 root 复制 → 启动） |
| 图片 URL 指向 `127.0.0.1:9000` | OSS 配置的 `endpoint` 被当成公网地址用了 | `endpoint=minio:9000`（容器内）+ `domain_url=https://img-<zone>`（公网）；已有数据需 `update ... replace(...)` 重写 |
| 轮换了 Tunnel token 后重启连不上 | 只更新了一个 replica 的 token，或旧 token 还在进程内存里 | 轮换后**每个 replica** 都要更新 token 文件并重启；`cloudflared` 只认文件里的新值 |

## 9. 纪律

- 部署机**不编译**：没有 `compose.build.yml`、没有源码、没有 `~/.m2`。
- **禁止** `docker system prune -a`（同机还有 appt 栈）；`gc` 只按前缀删。
- 同机多栈部署用 `flock` 互斥（`/tmp/sdblog-deploy.lock`），GitHub 的 concurrency 组跨仓库不生效。
