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
| S0 | 基线固化 | — | — | ✅（遗留：Apifox 覆盖导入清除陈旧 ArticleTag 接口，待执行人工确认） | 0.5d |
| S1 | 账号与个人资料 | §8、§9 | 1 | ✅ 已验收（2026-09-28） | 0.5d |
| S2 | 文章 + 版本核心 | §3.1~3.5、§4 | 2 | ✅ 已验收并 commit（49a8620） | 2~3d |
| S3 | 编辑器与上传 | §2.4、§18.9 | 3 | ✅ 已验收（2026-09-30；编辑器传图待前端仓库） | 1d |
| S4 | 编辑 = 新版本 | §3.4、§4.1/4.2 | 4 | ✅ 已验收（2026-09-30） | 1d |
| S5 | 治理状态机 | §3.6~3.8、§11 | 5 | ✅ 已验收（2026-10-01，commit 9db0752） | 1~2d |
| S6 | 标签治理 | §7、§13、§18.10 | 6 | ✅ 已验收（2026-10-01，commit 7620186；含 §5.1，D14） | 2d |
| S7 | 评论 | §10、§12 | 8（提前） | ✅ 已验收（2026-10-01，commit 63e68bd） | 1d |
| S8 | 搜索与阅读量 | §5.2、§6 | 9 | 🔄 代码完成，待 Apifox 验收（含 §5.1 补参、§9.2，D18） | 1d |
| S9 | 前台站 | §15 边界 | 7（后移） | ⬜（独立前端仓库） | — |

> 与 PRD §12.1 的顺序差异：评论提前到前台站之前，保证后端 API 闭环先行；
> S9 前台可在 S5 完成后于前端仓库并行启动（首页/列表/详情依赖 S2+S6+S8 的接口）。

## 3. 切片详情

### S0 基线固化 🔄

已完成：sd-blog 模块 Maven 接入并被反应堆识别；7 张表代码生成（BO 主键齐全）；违反契约 §14 的 3 个生成 Controller 已停用；`blog-api.md` 升级 v0.2 冻结；AGENTS.md 重构；`application.yml` springdoc 已加博客分组（packages-to-scan: org.dromara.blog）。

待办：

- [x] 注册开关 `sys.account.registerUser` 已置 true（2026-09-28 后台参数设置操作）
- [x] Apifox 项目已建、Windows 侧连通验证通过；运行时 21 路径与保留 4 组 Controller 一致（2026-09-28 curl 核验）；需在 Apifox 覆盖导入清除陈旧的 ArticleTag 接口
- [ ] git commit

延后事项（不阻塞，转入 S5）：

- ⏸ 停用 Controller 的 3 组残留菜单暂不清理（点击 404 属视觉噪音，无功能影响；勿分配给非管理员角色）。S5 做正式 Admin 菜单规划时与 `docs/blog_s0_cleanup.sql` 一并执行。

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

### S3 编辑器与上传 ✅（已验收 2026-09-30）

范围：契约 §2.4（复用 `/resource/oss/upload`，已存在零后端改动）+ 富文本 HTML 净化（§18.9）。

实施结果（D6 已关闭，细化见 §6 S3-D1/D2/D3）：

- 新增 `HtmlSanitizer`（jsoup Safelist 技术博客常用集，纯逻辑类，10 条单测覆盖 script/on*/危险协议/style 表达式/幂等/排版保留/空正文边界）
- 接入点：`SdBlogArticleServiceImpl.createArticle/updateArticle` 入口统一净化（在内容比对之前，幂等不产生伪版本差异）；title/summary 纯文本化，coverUrl 协议校验
- Q4 遗留：`docs/blog_s3_cleanup.sql` 已写（命中 3 篇 S2 冒烟文章），DELETE 段默认注释，**人工确认后执行**

已知边界核实：含 content 的写入接口仅两条——契约 `/blog/article/**`（已净化）与生成的
`SdBlogArticleVersionController`（含 content 字段，但已按契约 §14 停用不注册路由）；
`SdBlogArticleController`（后台 CRUD 骨架）的 BO 本身无 content 字段，写不到正文，无绕过入口。

Apifox 验收结果（2026-09-30 全部通过）：正向净化（危险标签/属性/协议被剔、排版保留、幂等不产生伪版本）
+ 无 token 401 + coverUrl 危险协议拒绝 + 越权用例均通过。验收中修正两处（见 §6 S3-D4）：
summary 误用正文级 `sanitizeHtml` 改为纯文本化；净化后正文为空串时拒绝入库。
上传用例（§2.4）因本地 MinIO 未启动需先拉起 `blog-minio`（属环境依赖，非代码问题）；编辑器传图需前端仓库配合。

完成标准：编辑器传图成功；含 `<script>`/`on*=`/危险协议的内容保存后被剥离；正常排版保留。

### S4 编辑 = 新版本 ✅（已验收 2026-09-30）

范围：契约 §3.4 Version 规则完整化、§4.1/4.2 历史只读、`published_at` 不随编辑更新。

实际盘点：§3.4/§4 的绝大部分规则已在 S2 落地（四字段比对、无变化不建版本、新版本立即生效、
published_at 不受编辑影响、版本只读接口），S4 真正的增量是：

- D11 关闭：`updateArticle` 事务内 FOR UPDATE 锁定文章行（校验通过后加锁，后续版本读取在拿锁之后），
  消除 version_no 查-max-插竞态；DB 层双会话实证锁语义通过
- append-only 红线护栏：`SdBlogArticleVersionServiceImpl.updateByBo/deleteWithValidByIds` 改为直接抛
  ServiceException（无调用方的生成死代码，防将来误用复活 UPDATE/DELETE 路径；insertByBo 合法不改）

验收结果（Apifox 全部通过）：重复保存不产生新版本、编辑立即生效且 published_at 不变、历史版本不可变可查。

完成标准：重复保存不产生新版本；编辑保存立即生效为新版本；旧版本内容不可变可查。

### S5 治理状态机 ✅（已验收 2026-10-01）

范围：契约 §3.6 hide、§3.7 show、§3.8 delete、§11 管理员下线/恢复；publish 前置校验补全（USER_HIDDEN/ADMIN_HIDDEN 不可借 publish 公开）。

实施结果（D12/D13 已关闭，见 §6）：

- `ArticleGuards` 新增四个纯逻辑守卫（requireUserToggleVisible/requireDeletable/requireAdminHidable/requireAdminRestorable），守卫矩阵单测 5→8 条；同态幂等在 Service 层实现
- 作者三接口：`POST /blog/article/{id}/hide`、`/show`、`DELETE /{id}`（逻辑删除，版本/标签/评论保留）
- `BlogAdminArticleController` 四接口（§11.1~11.4），`blog:article:list/query/adminHide/adminRestore` 按钮权限；admin 列表 title 两段式防 N+1；恢复时清空治理字段（审计痕迹由 update_by/update_time 保留）
- publish 补全：DRAFT+USER_HIDDEN 不得借发布转公开（必须先 show）
- D12：`SdBlogArticleController` 注释停用；`docs/blog_s5_menu.sql` 新增 4 治理按钮 + 清理 blogArticle 及 S0 延后的 3 组残留菜单（幂等可重跑，含执行后验证段）

验收待办（人工）：① 执行 `docs/blog_s5_menu.sql` 并重新登录刷新权限；② 重启 8081 加载 S5 代码（IDEA 先 Rebuild，Maven 产物与 IDEA 增量混用是已知坑）；③ Apifox 状态机矩阵用例。

完成标准：状态转换矩阵（四状态 × 六动作 × 三种角色）全部符合契约 §14 动作式 API 原则；ADMIN_HIDDEN 文章作者不可编辑不可恢复、仍可删除。

### S6 标签治理 ✅（已验收 2026-10-01）

范围：契约 §7 公共标签接口组（featured/hot/list+联想/{slug}/tag articles）、§13 管理端 rename/merge/运营更新/删除、§18.10 规范化实现；另含 §5.1 公共文章列表（D14 决议并入，与 §7.4 共用查询底座）。

关键点：合并事务六步（契约 §13.5）+ 链式展平（A→B→C 归一为 A→C、B→C）；跨表 slug 唯一性由 Service 保证（database-design §9.5）；删除后重输同 slug 恢复原标签。

S2 遗留接入（已落地）：
- `ISdBlogTagService.resolveOrCreateByNames` 三件事已补：① MERGED 沿 merged_into 跟随；② 已软删标签同 slug 恢复原记录（新增绕过 @TableLogic 的 selectAnyBySlug/restoreByTagId）；③ 新建前 tag.slug 与 alias_slug 跨表冲突检查；并消除旧版并发撞软删键回查 null 进结果的 NPE 隐患
- 标签合并迁移 ArticleTag 关系：重复关系按纯物理删除语义处理（S2 决议）

实施结果（D14/D15 已关闭，见 §6）：
- 新增 `TagMergeResolver` 纯逻辑（链跟随/冲突与迁移集合，4 条单测）；TagService 另三个生成写方法加护栏抛异常（同 S4 模式）
- `BlogPublicTagController` §7 五接口 + `BlogPublicArticleController` §5.1（D14 并入）+ `BlogAdminTagController` §13 六接口（blog:tag:* 按钮，blog_s6_menu.sql 含 blogTag 裸 CRUD 菜单清理）
- 公共文章底座 `selectPublicPage`（MPJ 联版本表，三条件内建不可绕过，§5.2/S8/S9 复用）；§7.4 实现在 article 侧避免 tag→article 循环依赖
- `SdBlogTagAlias.delFlag` Boolean→String（S2-D1 尾巴）；`SdBlogTagController` 注释停用（D12）
- 自决项：§7.4/§5.1 排序 published_at DESC, article_id DESC；§7.2 hot 默认 10 封顶 50；featured/hot 为保留路径（同名 slug 标签不可按 §7.3 访问，契约既定取舍）
- 冒烟发现并修复：hot 统计 `count(at.article_id)` 在 left join 下把草稿/隐藏计入（实测 6 vs 2），改 `count(a.article_id)` 后与 §7.3 一致

验收待办（人工）：① 执行 `docs/blog_s6_menu.sql` 并重新登录；② 重启 8081；③ Apifox 矩阵用例（含合并六步/alias 解析/软删恢复）。

完成标准：合并后无重复 ArticleTag、无多级合并链、alias 全部直指 canonical；热门标签统计只算公共可见文章。

### S7 评论 🔄

范围：契约 §10 发表/公共列表、§12 管理员列表/删除。

实施结果（D12/D16/D17 已关闭）：
- 发表 `POST /blog/article/{id}/comments`（BlogArticleController）：目标文章必须 PUBLISHED+PUBLIC+未删（复用 ArticleGuards.isPublicVisible，软删自然落入"文章不存在"）；content 过 sanitizePlainText（评论纯文本口径）、净化后空拒绝；归属全部服务端固定，parent_id 恒 NULL；@RepeatSubmit 防双击
- 公共列表 `GET /blog/public/articles/{id}/comments`：先验文章可见性防评论探测；时间升序分页（D17）；昵称翻译不复制
- 管理端 `BlogAdminCommentController`：list（articleId/userId/keyword，无 Map params）+ 单条逻辑删；`blog:comment:list/remove` 按钮
- Comment 生成 insert/update/delete 三写方法护栏抛异常（§10.2 不可改/不可自删）；`SdBlogCommentController` 停用（D12）；`SdBlogUserProfileController` 停用（D16）；`blog_s7_menu.sql` 含两组菜单清理，清理后全模块生成裸 CRUD 菜单归零
- 类型治理收尾：`SdBlogComment`/`SdBlogUserProfile` 的 delFlag Boolean→String（S2-D1 最后尾巴）
- 冒烟踩坑复现：IDEA 后台增量编译把带错 class 写进 target 使 jar 混入坏产物（运行期 Unresolved compilation problem），按 AGENTS.md §8 处方 clean 全量重编后 7/7 用例全过

验收待办（人工）：① 执行 `docs/blog_s7_menu.sql` 并重新登录；② 重启 8081（IDEA 先 Rebuild）；③ Apifox 用例。

完成标准：仅 PUBLISHED+PUBLIC+未删 文章可评；`parent_id` 恒为 NULL；评论不可修改；仅管理员可逻辑删除。

### S8 搜索与阅读量 🔄

范围：契约 §6 搜索（MySQL LIKE，经 ArticleSearchService 抽象隔离）、§5.2 详情 view_count+1；
另含 §5.1 参数补齐（S6 时只做了分页，漏了 tagSlug/authorId 契约参数，本切片修正）与 §9.2 用户公开文章（D18 并入）。

实施结果（D18/D19 已关闭）：
- §5.2 详情：独立 `BlogPublicArticleDetailVo`（不复用作者视角 VO，不泄露 status/visibility/治理字段）；
  非公共可见一律"文章不存在"；阅读量原子 UPDATE 自增（D8），返回库内原值+1 不二次查询
- §6 搜索：`ArticleSearchService` 接口 + `MysqlArticleSearchService`（仅当前生效版本 title/summary/content
  三字段 OR LIKE，D19）；ES 接入时替换实现不改契约；keyword 缺失/空串统一走 @NotBlank
  （required=false 防止 MissingServletRequestParameterException 落入未处理 500）
- §5.1 补 tagSlug（无效 slug → 空页，列表过滤语义）/authorId 参数；§9.2 复用同入口
- 标签填充抽为 `ArticleTagFiller` 组件，列表/详情/搜索同源；`selectPublicPage` 底座加 authorId 参数
- 冒烟：阅读量两次打开 1→3 与 DB 一致；搜索命中/空页/校验文案全过

验收待办（人工）：重启 8081（IDEA 先 Rebuild）+ Apifox 用例（无需菜单 SQL，全部匿名接口）。

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

### Agent 自测协议（每次编码完成后 Agent 自动执行，人不用催）

| 档位 | 内容 |
|------|------|
| 每次必跑 | ① `mvn compile`（受影响模块 -am）② 静态断言 grep 自查 ③ `git diff` 自审越界改动 |
| 切片按需 | ④ `mvn test`（D7 四类纯逻辑单测，Agent 写且 Agent 跑）⑤ 后台启动应用 + curl 冒烟（公共接口正向、鉴权接口负向 401）⑥ `docker exec blog-mysql` 只读 SELECT 核验状态流转/版本数等落库结果 |
| 明确不做 | 带真实登录态的 Apifox 五类全量用例、前端联调、视觉验收（留给人工） |

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
| 2026-09-28 | S1-D1：MVP 管理员不停用用户（**裁剪 PRD USER-02 P0 项，产品负责人决定**）。公共资料直接复用 `UserService.selectById`（带 status 过滤，不停用前提下无害），仅增量扩展 `UserDTO.avatar` 字段；不新增 api 方法。⚠️ 到期提醒：将来若恢复"停用用户"功能，停用者的公共资料接口会 404（其文章仍可见），届时须为 api 补无状态过滤的查询方法 | S1 实施 |
| 2026-09-28 | S2 决议：`sd_blog_article_tag` 转纯关系表——物理删除、去 `del_flag`、唯一键 `(article_id, tag_id)`，对齐 `sys_user_role` 框架惯例；schema/database-design §2.3、§4、§10 已同步修订，存量库迁移脚本 `docs/blog_s2_migration.sql` 已执行（0 行数据，安全） | S2 实施 |
| 2026-09-28 | S2-D1：char(1) 状态列类型治理 = String + 枚举（`BlogArticleStatus/BlogVisibility/BlogTagStatus`），本切片覆盖 Article/Version/ArticleTag/Tag 四表及连带 Bo/Vo；Comment/UserProfile 的 `delFlag` 在各自切片治理 | S2 实施 |
| 2026-09-28 | S2-Q1~Q5：发布前校验 currentVersionId 非空；作者自助接口管理员判定 = `LoginHelper.isSuperAdmin()`（治理走 S5 admin 接口）；我的列表排序 `update_time DESC, article_id DESC` 且 tags 两段式批量填充；HTML 净化确认后置 S3；MERGED 标签跟随确认后置 S6 | S2 实施 |
| 2026-09-30 | S3-D1（关闭 D6）：Safelist = 技术博客常用集（relaxed + 列表/表格/h4-h6/hr/kbd 等；style/id 逐标签放开，pre/code 额外允许 class 保语法高亮；img 允许 data: 内联图；不放开 iframe）；jsoup 15+ 不再过滤 style 值，自补 expression(/behavior: 等关键字剔除 | HtmlSanitizer |
| 2026-09-30 | S3-D2：XssFilter 豁免方式 = `xss.excludeUrls` 路径级豁免 `/blog/article/**`（框架既有机制，同 `/system/notice` 先例）；豁免后 title/summary 防注入由 Service 层净化兜底 | application.yml |
| 2026-09-30 | S3-D3：S2 裸 HTML 测试数据处置 = SQL 脚本删除（docs/blog_s3_cleanup.sql，按 article_id+标题双重定位，人工确认后执行）；测试自动建的 sd_blog_tag 保留（S6 同 slug 恢复语义依赖历史记录） | Q4 决议执行 |
| 2026-09-30 | S3-D4（Apifox 验收修正）：① summary 是纯文本字段，净化从正文级 `sanitizeHtml` 改为 `sanitizePlainText`（避免白名单内 `<p>/<b>` 被保留）；② 净化后正文 HTML 串 `StringUtils.isBlank` 时拒绝入库，防止整段 `<script>` 被剔后空串入库（注：仅拦空串，`<p></p>` 类空段落不拦，MVP 够用） | S3 验收 |
| 2026-09-30 | D11（关闭，S4）：version_no 并发策略 = 悲观锁——`updateArticle` 事务内 `SELECT ... FOR UPDATE` 锁定文章行，同文章并发保存串行排队、拿锁后重读最新版本再比对（彻底消除竞态且不会重复建版本）；否决“撞键重试”（事务内 catch 后继续用同一事务有风险）与“撞键提示刷新”（双击即报错体验差）；DB 唯一键 `uq_blog_article_version_no` 保留作最后兜底；同时 VersionService 生成死代码 update/delete 改为抛异常护栏（append-only 红线） | S4 实施 |
| 2026-09-30 | D12（关闭，S5）：生成 CRUD Controller 去留 = 停用 `SdBlogArticleController`（裸 edit/delete 绕过作者校验与状态机，已被 §3+§11 契约接口 100% 取代）；Tag/Comment 暂留作临时管理工具，S6/S7 契约接口就位后按同一模式停用；菜单清理并入 `blog_s5_menu.sql`（含 S0 延后的 3 组残留） | S5 实施 |
| 2026-09-30 | D13（S5 产品决议，收窄契约未写死部分）：草稿不参与隐藏/下线——作者 hide/show 仅对 PUBLISHED 生效（草稿本不进公共列表），管理员下线仅对 PUBLISHED+PUBLIC 生效；同态幂等放行（重复 hide/show/下线返回成功不改数据，adminHide 对已下线文章允许更新下线记录）；治理动作的可见性转换不改 status | S5 开工前用户确认 |
| 2026-09-30 | D14（关闭，S6）：§5.1 公共文章列表未分配切片，经确认并入 S6 实现（与 §7.4 共用 selectPublicPage 底座，三条件内建不可绕过）；S8 只剩 §5.2 详情+§6 搜索 | S6 开工前用户确认 |
| 2026-09-30 | D15（S6 自决项备案）：§7.4/§5.1 排序 published_at DESC, article_id DESC；§7.2 hot 默认 10 封顶 50；featured/hot 为保留路径；adminDelete 仅允许 ACTIVE 标签（MERGED 历史节点不可删，保护 alias/合并链）；重命名后同 slug 再输入经 resolveOrCreate 的 alias 分支复用 canonical | S6 实施 |
| 2026-10-01 | D16（关闭，S7）：UserProfile 生成 CRUD 无契约接口取代——停用 `SdBlogUserProfileController`（裸 edit 可改任意用户 bio，MVP 无此场景无审计），菜单随 blog_s7_menu.sql 清理；将来若需后台查资料再按契约补接口；Service/Mapper 保留供 S1 自助链路复用 | S7 开工前用户确认 |
| 2026-10-01 | D17（S7 自决项备案）：公共评论列表排序 create_time ASC, comment_id ASC（一级评论流时间序）；管理列表 create_time DESC；adminDelete 对已删评论报"评论不存在"（@TableLogic 过滤后不可见，不做幂等伪成功）；评论内容按纯文本 sanitizePlainText 处理（非富文本链路，与 title 同口径） | S7 实施 |
| 2026-10-01 | D18（关闭，S8）：§9.2 用户公开文章未分配切片，经确认并入 S8（与 §5.1 共用 queryPublicPageList 入口的 authorId 过滤）；用户不存在时返回空列表（列表类语义）；同时补 S6 漏实现的 §5.1 tagSlug/authorId 参数 | S8 开工前用户确认 |
| 2026-10-01 | D19（S8 自决项备案）：搜索字段 = 当前生效版本的 title/summary/content 三字段 OR LIKE（仅命中 current_version_id 对应版本，不含历史版本）；结果排序同 §5.1 published_at 倒序；§5.1 tagSlug 无效返回空页（区别于 §7.4 报错）；详情 viewCount 返回库内原值+1；@RequestParam 必传参数一律 required=false + @NotBlank（防参数缺失落入未处理 500） | S8 实施 |
| 2026-10-01 | D20（S8 后死代码治理，含第二批执行完毕）：7 组生成 Controller + BlogHelloController 已物理删除；第二批瘦身已执行：Version/Alias Service 整类删、ArticleTagService 仅留 syncArticleTags、Article/Tag/Comment/UserProfile 生成 CRUD 方法及其 impl 删除（Tag.queryById 保留§13.2）、6 个 SdBlog*Bo 删除（TagBo 保留）；**例外：mapper/blog/ 下 7 个 XML 按用户决定保留**（头注释改为说明性）；Entity/Mapper/Vo 因 BaseMapperPlus 泛型绑定保留；护栏方法已随删除由编译期阻断接替。回归：单测 32/32、clean package、冒烟公共 200/鉴权 401/swagger 31 paths 接口面零变化 | S8 后清理 |

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
