# 部署方案（cloud-deployment）

> 状态：v0.1 · 2026-10-30（S29 立项，**方案冻结、代码未开工**）
> 定位：部署**方案备忘**——双目标拓扑、资源选型、发布/回滚/备份口径、待决项。
> 权威边界：切片状态与完成标准以 `docs/ROADMAP.md` 为准（S29.1 / S29.2）；决策全文见 ROADMAP §6 D45/D46；落地后的**命令权威**是 `deploy/README.md`（S29.1 产出）。本文只记「为什么这么部署」，不复制命令。

## 1. 双目标与切片

```text
S29.1 发布链 + WSL 目标（不依赖任何服务器，全部本机可验收）
  CI 产四枚不可变镜像 → GHCR(private) → 部署机只 pull
  deploy.sh up/adopt/down/rollback/backup/restore/status
  Cloudflare Named Tunnel（qdblog-demo）→ sdblog.us.ci / admin.sdblog.us.ci
        │
        ▼  阻塞：服务器 + 备案时钟
S29.2 云服务器目标（承接已停用的 S13）
```

| 切片 | 目标 | 前置 | 不做 |
|---|---|---|---|
| S29.1 | 发布链跑通 + WSL 演示栈公网可访问 | 无（本机 + GitHub/CF 账号） | 云主机、备案、证书、SSH 部署分支、监控 |
| S29.2 | 云主机上一套可访问生产环境 | S29.1 全绿 + 服务器/备案 | K8s、staging 多环境、灰度 |

### 1.1 公网入口（只有两条）

| 域名 | 隧道目标 | 说明 |
|---|---|---|
| `sdblog.qdappt.us.ci` | `127.0.0.1:19300`（blog-web） | 读者站 + 作者中心；`/api/**` 由 Nitro 内部转 `backend:8081` |
| `admin-sdblog.qdappt.us.ci` | `127.0.0.1:19080`（plus-ui） | **公网直开**（2026-10-30 决定，不加 Cloudflare Access）；`/prod-api/*` 反代后端 |
| `img-sdblog.qdappt.us.ci` | `127.0.0.1:19000`（MinIO） | 封面/正文图；桶 `ruoyi` 需匿名下载策略 |

> **为什么不用 `sdblog.us.ci` / `admin.sdblog.us.ci`**：zone 是 `qdappt.us.ci`，这两条是**两层子域**，Cloudflare 免费 Universal SSL 只覆盖「apex + 一层通配 `*.qdappt.us.ci`」，实测 TLS handshake failure（000）。改单层后全部 200。`admin.sdblog.qdappt.us.ci` 同样是两层，故用连字符 `admin-sdblog`。

- **不设 `api.` 子域**：两个前端各自反代后端；后端一旦有公网入口，鉴权面与限流面都要重新评估。
- 后端 / MySQL / Redis / MinIO / ES 一律不公网，端口只绑 `127.0.0.1`。
- 本机自检必须 `curl --noproxy '*'`：开发机 shell 里存在 `https_proxy`，走代理会对这些域名随机 TLS 抖动（假 000）。

## 2. 资源选型（承接 ROADMAP §6 待决 D37）

| 方案 | 规格/成本 | 优点 | 风险 |
|---|---|---|---|
| Oracle Always Free | 4C/24G，免费 | 内存足够跑 ES | 需信用卡、有闲置回收风险 |
| 国内轻量付费 | 2C4G 约 50~150/年 | 线路稳、可备案 | 需备案 1~2 周；4G 跑 ES 吃紧 |
| 香港节点 | 2C4G 同价 | 免备案 | 线路波动 |

建议：演示期可用白嫖方案；**挂简历前落付费轻量**（面试前被回收的代价高于一年费用）。

**S29.2 必须先拍板的资源问题**：2C4G 上是否上 ES。上则 `ES_JAVA_OPTS=-Xms512m -Xmx512m` + 后端 `-Xmx768m`，余量很小；不上则 `blog.search.engine=mysql` 降级——代价是 S16 的亮点在演示环境消失。二选一，不做「先上都上、崩了再说」。

## 3. S29.1 实测记录（2026-10-30，WSL 本机，镜像为本地既有产物打 local 别名）

| 指标 | 目标 | 实测 |
|---|---|---|
| 首次起栈（initdb + 7 服务） | ≤ 180s | ~70s（镜像已在本地） |
| 增量 up（容器已在运行，含 19 项冒烟） | ≤ 90s | ~40s（本地镜像）→ **~60s（registry 镜像 v0.1.x）** |
| 回滚（摘要切换 + 冒烟） | ≤ 60s | **~55s**（真实 registry digest；19/19） |
| 前滚（再回到新版本 + 冒烟） | ≤ 60s | **~55s**（19/19） |
| 备份 | ≤ 10s | ~2s（库 52K / MinIO 8K） |
| 恢复（删库重建 + 起栈 + 冒烟） | ≤ 120s | ~65s |
| release 流水线 run | ≤ 6min | **2m18s**（preflight 1m37s + deploy 33s，Release #3 全绿） |
| 公网两域名 | 全程 200 | **200 / 200**（含图片域 200） |

> 首次 `pull` 未测：私有包需先 `docker login ghcr.io`；四枚镜像合计约 2.3GB，首次拉取时长与增量发布不可比。

### 3.1 环境事实（W0 复勘结论，2026-10-30）

| 项 | 实测 | 结论 / 待办 |
|---|---|---|
| 自托管 runner | `actions.runner.xcqj-qd-appt-showcase.PC-202408061330.service`，`User=user01`，`gitHubUrl=https://github.com/xcqj/qd-appt-showcase` | **单仓注册**，本仓用不了 → 需为 `ryblogs` 另注册一个（label `sdblog-wsl`），需一次性注册 token |
| cloudflared | `cloudflared.service` enabled+active，`--token-file /etc/cloudflared/token`；另有 `cloudflared-qdappt2.service`（appt 专用） | `/etc/cloudflared/token` **已不存在**（进程 8h 前读入内存），重启必失效 → 为 `qdblog-demo` 建**独立 unit + 独立 token 文件** |
| Tunnel 路由 | 面板 `qdblog-demo` Healthy / 1 replica / **No routes** | 待加两条 Public Hostname（apex → 19300、admin → 19080） |
| 域名 | `sdblog.us.ci` DNS 在 Cloudflare | ✓ |
| Docker | RootDir `/var/lib/docker`（ext4）、Compose v5.5.0、buildx 0.36.1 | ✓ |
| 磁盘 | 890G 可用；镜像 10.6G、构建缓存 19G | 充足，`gc` 可回收 |
| `vm.max_map_count` | 1048576，但 `/etc/sysctl.d/` 无配置 | 当前达标，**WSL 重启可能丢失** → 需持久化（要 sudo） |
| 内存 | 15G 总；容器约 4.1G、IDE 的 7 个 java 进程约 7.5G；可用 1.6~2.8G | 演示栈已按 `BACKEND_MEM=1g / JAVA_OPTS=-Xmx512m / ES 256m` 降配；验收期建议先停开发栈 |
| 端口 | 19xxx 全空闲；appt 占 18xxx；开发栈占 3306/6379/9000/9200 与 18080/18081/13000 | 三段互不冲突 ✓ |
| sudo | 非交互环境 `sudo` 需要密码 | sysctl 持久化、cloudflared unit/token 只能由用户在终端执行 |

### 3.2 W1 实测（2026-10-30）

- 空数据目录起栈：**31 表 / 7 张博客表 / 15 条博客菜单 / 1 条 `web` 客户端**，initdb 无 ERROR
- 四套 compose 项目并存：`docker`(7) + `qd-appt`(1) + `qd-appt-demo`(7) + `sdblog-demo`(7)，端口与容器名零冲突
- 栈内冒烟 **19/19**；`seed-demo` 装载后文章 52（公共 47）/ 评论 200 / 标签 20，ES 47 文档、搜索命中带 `<em>` 高亮
- 备份 → 删库 → 恢复：五项计数 **52 / 52 / 200 / 20 / 8** 与备份前一致，搜索可用（ES 由 bootstrap 重建）
- 两个已修偏差：① `DATA_DIR` 子目录首次由 Docker 创建为 root 所有 → ES 无限重启（`init_data_dir` 内用容器 chown 修复）；② 种子直插 SQL 不触发应用事件 → ES 不自动感知（`deploy.sh seed` 删除索引版本 + 重启后端做确定性重建）

### 3.3 W4 公网实测（2026-10-30 晚）

- 三条 Public Hostname 改单层子域后：前台 200 / 后台 200 / 图片域 200；`smoke-public.sh` **8/8**
- 后台 `/prod-api/auth/code` 返回 envelope 200（隧道 → nginx → backend 全链路通）
- sitemap 内链接为 `https://sdblog.qdappt.us.ci`，robots 声明 sitemap，详情页 SSR 正常
- **图片链路**：OSS 配置改为 `endpoint=minio:9000`（容器内）+ `domain_url=https://img-sdblog.qdappt.us.ci`（公网）+ `access_policy=2`；10 条种子封面 URL 重写为图片域名；桶改为匿名下载后封面 200（2MB jpeg）
- 桶策略的落地方式（环境限制用不了 `mc`，Docker Hub 镜像源与 dl.min.io 均不可达）：**把开发栈已公开桶的 `.minio.sys/buckets/ruoyi` 元数据复制到演示栈**（停 MinIO → 容器内 root 复制 → 启动）
- `cloudflared`：token 已落盘 `/etc/cloudflared/token`（249B / 600，轮换后的新 token），`systemctl restart` 后 `enabled`+`active`，三条域名仍 200
- 发布流水线 e2e：三仓 `v0.1.1` tag → 四枚镜像 → `release.yml`（preflight 1m37s + deploy 33s）**全绿**；`deploy.sh up` 对同版本幂等（镜像 digest 未变时不会重建容器）

## 4. S29.2 待办与待决

1. **入口二选一**：Caddy 自动 HTTPS（大陆推荐，需 80/443 + 备案）vs 复用 Cloudflare Tunnel（免证书、免公网 IP）。**不两套并存**。
2. **备案**：大陆节点必须；页脚展示备案号。
3. **OSS/CDN**（D31）：`access_policy=2` 公有读 + 域名回源，否则匿名读者破图。
4. **ES 内存取舍**：见 §2 末段。
5. **监控**：UptimeRobot 免费档 + 备份定时（systemd timer）。
6. **小程序合法域名**：S11 启动前在平台后台登记。
7. **社交登录**：`GITHUB_REDIRECT_URI` / `GITEE_REDIRECT_URI` 换成生产域名并回填平台后台（**含 query 的整串精确匹配**，S14 实测）。

## 5. 发布口径（冻结）

### 5.1 端口三段（容器内端口永不变）

| 服务 | 容器内 | 开发态（宿主机直跑） | 开发态（容器化，`--profile app`） | 部署/演示态 |
|---|---|---|---|---|
| backend | 8081 | 8081（IDE） | 18081 | 19081（127.0.0.1） |
| admin | 80 | 5173（vite） | 8080 | 19080（127.0.0.1） |
| web | 3000 | 3000（nuxt dev） | 3100 | 19300（127.0.0.1） |
| mysql | 3306 | 3306 | 3306 | 13306（127.0.0.1） |
| redis | 6379 | 6379 | 6379 | 16379（127.0.0.1） |
| minio | 9000/9001 | 同 | 同 | 19000/19001（127.0.0.1） |
| es | 9200 | 同 | 同 | 19200（127.0.0.1） |

> 与参考项目（开发 8xxx / 演示 18xxx）不同：18xxx 已被 S25 的容器化开发端口占用（18081/3100/8080），整体搬迁的收益低于文档与脚本改动成本。**真正的不变量是三段互不重叠 + 部署态只绑 127.0.0.1。**

### 5.2 备份对象

| 对象 | 方式 | 备份 |
|---|---|---|
| MySQL `ry-vue` | `mysqldump --single-transaction --routines --triggers` | 是 |
| MinIO | `${DATA_DIR}/minio` 目录打包（bind mount 后即普通目录） | 是 |
| Redis | —（AOF 仅保阅读量增量，丢可接受） | 否 |
| Elasticsearch | **不备份**：alias 缺失时 `ArticleIndexBootstrap` 启动即全量重建 | 否 |
| `.env` / `.state` | 随机器，不进备份包 | 否 |

恢复完成标准 = **行数一致**（S26 基线：52 篇 / 52 版本 / 200 评论 / 20 标签 / 8 用户）+ 搜索命中 + 图片可访问，不是「容器起来了」。

### 5.3 回滚边界

- 回滚按 **digest**（`.state/{current,previous}` 记四枚镜像），不按 tag。
- **数据库只前滚**：本项目无迁移框架，`rollback` 只回镜像。发版时若有 schema 变更，必须置 `DB_CHANGED=true`，否则 rollback 需 `--force` 才放行。

## 6. 已知边界与不做

- **部署机不持源码、不编译**：SQL 与 `my.cnf` / `redis.conf` 由部署包携带（initdb 必须拿到有序脚本，否则空库 → 后端 crash loop）。
- **备份与数据同盘**（`~/sdblog`），只防误删不防磁盘损坏；异地备份属 S29.2 之后。
- **两个项目共用一台宿主机与一个 runner**：`deploy.sh gc` 只按 `sdblog-*` 前缀删镜像，**禁止 `docker system prune -a`**；部署加 `flock` 互斥（GitHub concurrency 组跨仓不生效）。
- **不做的**：K8s、staging 多环境、灰度发布、蓝绿、CDN 多级缓存、镜像多 registry。
