# 博客 MVP 开发路线图（ROADMAP）

> 状态：v0.1 · 2026-09-28
> 定位：排期文档——做什么、按什么顺序做、每一步的完成标准是什么。
> 本文件不重复权威内容，只引用：
>
> - 产品与业务规则：`docs/PRD博客MVP需求文档_v0.2.docx`
> - API 契约：`docs/blog-api.md`（v0.2，已冻结）
> - 数据库：`docs/database-design-v0.1.md` + `docs/blog_schema_v0.1.sql`（物理表以 `sd_blog_` 前缀为准）
> - 编码规范：`.codex/skills/ruoyi-plus-ai-coding/` + `.claude/agents/`
> - 硬性红线与已验证坑：`AGENTS.md`

## 1. 使用规则

1. 每个切片按闭环流程推进（PRD §12）：只读分析 → 实施计划 → 编码 → 编译 → Apifox 用例（正向 / 鉴权失败 / 参数校验 / 资源不存在 / 越权）→ 联调 → 验收 → git commit。
2. 一个切片一个提交周期；开工前先建 Git checkpoint；发现设计与实现冲突，先报告再动手。
3. 切片内的「决策点」必须在该切片开工前关闭；结论写入 §6 决策记录，涉及契约的同步修订 `blog-api.md`。
4. 本文件与契约 / PRD 冲突时，以契约 / PRD 为准，并回来修正本文件。

## 2. 切片总览

| # | 切片 | 主要契约章节 | 对应 PRD 切片 | 状态 | 粗估 |
|---|------|-------------|--------------|------|------|
| S0 | 基线固化 | — | — | 🔄 部分完成 | 0.5d |
| S1 | 账号与个人资料 | §8、§9 | 1 | ⬜ | 0.5d |
| S2 | 文章 + 版本核心 | §3.1~3.5、§4 | 2 | ⬜ | 2~3d |
| S3 | 编辑器与上传 | §2.4、§18.9 | 3 | ⬜ | 1d |
| S4 | 编辑 = 新版本 | §3.4、§4.1/4.2 | 4 | ⬜ | 1d |
| S5 | 治理状态机 | §3.6~3.8、§11 | 5 | ⬜ | 1~2d |
| S6 | 标签治理 | §7、§13、§18.10 | 6 | ⬜ | 2d |
| S7 | 评论 | §10、§12 | 8（提前） | ⬜ | 1d |
| S8 | 搜索与阅读量 | §5.2、§6 | 9 | ⬜ | 1d |
| S9 | 前台站 | §15 边界 | 7（后移） | ⬜（独立前端仓库） | — |

> 与 PRD §12.1 的顺序差异：评论提前到前台站之前，保证后端 API 闭环先行；
> S9 前台可在 S5 完成后于前端仓库并行启动（首页/列表/详情依赖 S2+S6+S8 的接口）。

## 3. 切片详情

### S0 基线固化 🔄

已完成：sd-blog 模块 Maven 接入并被反应堆识别；7 张表代码生成（BO 主键齐全）；违反契约 §14 的 3 个生成 Controller 已停用；`blog-api.md` 升级 v0.2 冻结；AGENTS.md 重构。

待办：

- [ ] `application.yml` springdoc `group-configs` 增加博客分组（`packages-to-scan: org.dromara.blog`）
- [ ] 执行保留的 4 组 CRUD 菜单 SQL（已停用的 Version/TagAlias/ArticleTag 菜单不执行或清理）
- [ ] 建 Apifox 项目，冒烟保留的 4 组生成 CRUD
- [ ] git commit

完成标准：Swagger「博客模块」分组可见全部接口且可调用。

### S1 账号与个人资料 ⬜

范围：注册 / 登录 / 验证码走通（复用 RuoYi，仅确认配置）+ `GET/PUT /blog/user/profile`（契约 §8）+ `GET /blog/public/users/{userId}`（§9.1）。

决策点：注册开关 `sys.account.registerUser` 是否开启（建议开）。

完成标准：新用户注册 → 登录 → 填写 bio → 公共资料接口返回 userId/avatar/nickName/bio（头像昵称实时取自 sys_user，不复制）。

### S2 文章 + 版本核心 ⬜（全项目最难，预留缓冲）

范围：契约 §3.1 创建、§3.2 我的列表、§3.3 我的详情、§3.4 保存、§3.5 发布、§4 版本只读查询。

关键点：

- 保存事务链：Article upsert → 内容比对决定是否建 Version（title/summary/coverUrl/content 四字段）→ 回写 `current_version_id` → 标签同步（1~6、规范化、自动入库、恢复历史关系）
- `author_id` 一律取登录上下文，越权用例必须进 Apifox
- 列表字段在 Version 表，联查 `current_version_id`（契约 §18.11，防 N+1）
- 公共可见三条件（§1.3）在本切片首次落地，供 S9 复用

决策点：新 Controller 类命名与分层方案（见 §6 待决 D5）。

完成标准：创建→草稿→发布→我的列表→详情全链路通过；非作者操作他人文章一律 403/业务失败。

### S3 编辑器与上传 ⬜

范围：契约 §2.4（复用 `/resource/oss/upload`）+ 富文本 HTML 净化（§18.9）。

决策点（开工前必须关闭，见 §6 待决 D6）：净化库与 Safelist 白名单范围；RuoYi 全局 XssFilter 对富文本字段的豁免方式（避免双重转义）。

完成标准：编辑器传图成功；含 `<script>`/`on*=`/危险协议的内容保存后被剥离；正常排版保留。

### S4 编辑 = 新版本 ⬜

范围：契约 §3.4 Version 规则完整化、§4.1/4.2 历史只读、`published_at` 不随编辑更新。

决策点：`version_no` 并发策略（§18.12：唯一键兜底 + 重试或提示）。

完成标准：重复保存不产生新版本；编辑保存立即生效为新版本；旧版本内容不可变可查。

### S5 治理状态机 ⬜

范围：契约 §3.6 hide、§3.7 show、§3.8 delete、§11 管理员下线/恢复；publish 前置校验补全（USER_HIDDEN/ADMIN_HIDDEN 不可借 publish 公开）。

决策点：生成的 Article/Tag/Comment CRUD Controller 的去留（保留为管理员临时工具，或被 `/blog/admin/*` 契约接口取代后删除）。

完成标准：状态转换矩阵（四状态 × 六动作 × 三种角色）全部符合契约 §14 动作式 API 原则；ADMIN_HIDDEN 文章作者不可编辑不可恢复、仍可删除。

### S6 标签治理 ⬜

范围：契约 §7 公共标签接口组（featured/hot/list+联想/{slug}/tag articles）、§13 管理端 rename/merge/运营更新/删除、§18.10 规范化实现。

关键点：合并事务六步（契约 §13.5）+ 链式展平（A→B→C 归一为 A→C、B→C）；跨表 slug 唯一性由 Service 保证（database-design §9.5）；删除后重输同 slug 恢复原标签。

完成标准：合并后无重复 ArticleTag、无多级合并链、alias 全部直指 canonical；热门标签统计只算公共可见文章。

### S7 评论 ⬜

范围：契约 §10 发表/公共列表、§12 管理员列表/删除。

完成标准：仅 PUBLISHED+PUBLIC+未删 文章可评；`parent_id` 恒为 NULL；评论不可修改；仅管理员可逻辑删除。

### S8 搜索与阅读量 ⬜

范围：契约 §6 搜索（MySQL LIKE，经 ArticleSearchService 抽象隔离）、§5.2 详情 view_count+1。

决策点：阅读量实现（建议同步原子 UPDATE，MVP 不上 Redis，见待决 D8）。

完成标准：搜索仅返回公共可见文章；详情打开计数 +1；ES 替换不改变 API 契约。

### S9 前台站 ⬜（独立仓库，本表仅登记依赖）

依赖：S2（文章接口）+ S6（标签）+ S8（搜索）为最低可启动集；alias→canonical 的 301 由前台/边缘层负责（契约 §7.3）。

## 4. 关键路径与并行

```text
S0 → S1 → S2 → S3 → S4 → S5 → S6 → S7 → S8   （后端关键路径，粗估 10~12 人日）
                              └── S9 前台（S5 后可并行启动）
```

- S2 是最大风险点（事务编排 + 权限 + 版本），如超 3 天先拆解再继续。
- 切片间不允许功能倒灌：S5 之前的 Article 保存不得夹带 hide/admin-hide 逻辑。

## 5. 通用完成定义（每个切片都要满足）

1. `mvn compile` 全绿 + 应用可启动。
2. Apifox 用例五类齐全（正向 / 鉴权失败 / 参数校验 / 资源不存在 / 越权）。
3. 不违反 AGENTS.md §5 红线与 `blog-api.md` 契约；违反契约的临时行为已登记 §6。
4. 关键权限/删除逻辑经人工复核（PRD §12.3）。
5. git commit，提交信息标注切片号。

## 6. 决策记录

### 已确认

| 日期 | 决策 | 出处 |
|------|------|------|
| 2026-09-28 | 模块 `sd-blog`，包 `org.dromara.blog`，物理表前缀 `sd_blog_` | 实际代码/schema |
| 2026-09-28 | 契约优先路线：Version/TagAlias/ArticleTag 生成 Controller 停用，仅留 Service/Mapper | blog-api.md §14 |
| 2026-09-28 | 管理员不可编辑/代发布/删除文章；治理手段仅有下线与恢复 | blog-api.md v0.2 §3/§11 |
| 2026-09-28 | 作者自助接口只要求登录不设按钮权限；管理员接口 `blog:资源:动作`；公共接口走 `security.excludes` | blog-api.md v0.2 §17 |
| 2026-09-28 | 分页字段 `rows/total`；上传复用 `/resource/oss/upload`；新增公共标签列表兼作联想 | blog-api.md v0.2 |
| 2026-09-28 | 内容变化判定 = title+summary+coverUrl+content 四字段比对 | blog-api.md §3.4 |
| 2026-09-28 | D1：不单独建 rules.md，硬性规则唯一权威 = AGENTS.md §5/§9/§10（避免双源漂移，且 docs 下文件不会被 AI 自动加载） | 本文件 |
| 2026-09-28 | D2：建 architecture.md 轻量版（模块图+请求链+部署拓扑，≤120 行，只引用不重复） | docs/architecture.md |
| 2026-09-28 | D3：建 docs/SETUP.md 环境文档（待补信息见 D10） | 本文件 |
| 2026-09-28 | D4：不单独建 DECISIONS.md，决策记录统一在本文件 §6 | 本文件 |
| 2026-09-28 | D5：实体保留 `SdBlog*`；契约 Controller 按 API 层命名（`BlogPublicXxxController` / `BlogXxxController` / `BlogAdminXxxController`）；生成 CRUD 被契约接口取代后删除 | 本文件 |
| 2026-09-28 | D6：HTML 净化 = jsoup Safelist 白名单，Service 层保存前清洗，全局 XssFilter 对富文本字段豁免 | S3 落地时细化 |
| 2026-09-28 | D8：阅读量 = 同步原子 UPDATE，MVP 不上 Redis/异步 | S8 落地 |
| 2026-09-28 | D7：单测只覆盖 4 类纯逻辑（标签规范化 / 状态转换矩阵 / 内容变化比对 / 合并展平去重），且必须实现为独立可测类；CRUD/HTTP 链路靠 Apifox。策略已固化到 AGENTS.md §8 | S2 起执行 |
| 2026-09-28 | D9：注册用框架自带能力（`POST /auth/register` + sys_config 参数 `sys.account.registerUser`），S1 时在后台参数管理置 `true`，零代码改动 | S1 |
| 2026-09-28 | D10：SQL 最小集 = `ry_vue.sql` + `docs/blog_schema_v0.1.sql`（workflow/job/ai 三份属独立服务，博客不导）；前端仓库自管环境文档；Apifox 直连 `http://localhost:8081` | docs/SETUP.md |

### 待决策（当前无）

| # | 决策 | 建议 | 截止 |
|---|------|------|------|
| — | 暂无未决事项；新决策出现时按 D11 起编号登记在本表，关闭后移入上表 | — | — |

> **D7 说明（摘要，全文见 AGENTS.md §8）**：单测只覆盖"不碰数据库、不启 Spring 容器就能算对错的纯逻辑"——
> ① 标签规范化（`"Python 爬虫"` → `python-爬虫` 等契约 §18.10 规则）；
> ② 状态转换矩阵（四状态 × 动作 × 角色 → 允许/拒绝，契约 §14）；
> ③ 内容变化比对（四字段，决定要不要建新 Version）；
> ④ 合并展平与去重的纯计算部分。
> CRUD/HTTP 链路不写 Java 单测（Apifox 五类用例已覆盖），Controller 层不做 mock 测试（ROI 低）。
> 依赖已就绪：根 pom 统一引入 spring-boot-starter-test（JUnit5 + Mockito），common 模块已有单测先例。
