# 博客 MVP 开发路线图（ROADMAP）

> 状态：v0.4 · 2026-10-15（新增 S13~S21 面试亮点切片；MVP S0–S12 已全部关闭；S14~S20 已关闭，S13 仍阻塞 D37，S21 可选待定）
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

| # | 切片 | 主要契约章节 | 对应 PRD 切片 | 涉及端 | 状态 | 粗估 |
|---|------|-------------|--------------|--------|------|------|
| S0 | 基线固化 | — | — | 后端 | ✅（遗留已清：Apifox 陈旧 ArticleTag 接口已覆盖导入清除；git commit = c96e59d） | 0.5d |
| S1 | 账号与个人资料 | §8、§9 | 1 | 后端 | ✅ 已验收（2026-09-28） | 0.5d |
| S2 | 文章 + 版本核心 | §3.1~3.5、§4 | 2 | 后端 | ✅ 已验收并 commit（49a8620） | 2~3d |
| S3 | 编辑器与上传 | §2.4、§18.9 | 3 | 后端 | ✅ 已验收（2026-09-30；编辑器传图待前端仓库） | 1d |
| S4 | 编辑 = 新版本 | §3.4、§4.1/4.2 | 4 | 后端 | ✅ 已验收（2026-09-30） | 1d |
| S5 | 治理状态机 | §3.6~3.8、§11 | 5 | 后端 | ✅ 已验收（2026-10-01，commit 9db0752） | 1~2d |
| S6 | 标签治理 | §7、§13、§18.10 | 6 | 后端 | ✅ 已验收（2026-10-01，commit 7620186；含 §5.1，D14） | 2d |
| S7 | 评论 | §10、§12 | 8（提前） | 后端 | ✅ 已验收（2026-10-01，commit 63e68bd） | 1d |
| S8 | 搜索与阅读量 | §5.2、§6 | 9 | 后端 | ✅ 已验收（2026-10-03，commit 0dfa282；含 §5.1 补参、§9.2，D18） | 1d |
| S9 | 前台站（读者） | §15 边界 | 7（后移） | 前端 | ✅ 已验收（2026-10-08 用户签收；独立仓库 `blogs-web` → 本地 `blog-web`，D21；范围=响应式含移动浏览器适配，D27；作者发文见 S12/D29） | — |
| S10 | 管理后台前端（博客） | §3、§7、§11、§12、§13 | 5/6/8 前端面 | 前端 | ✅ 已验收（2026-10-06，浏览器验收通过；治理页×3 + 作者发文/编辑/详情/版本 + 个人资料 tab + merge，D23/D24/D26） | 3~5d |
| S11 | 移动端套件（uni-app） | §15 边界（复用） | — | 移动端 | 🔖 立项待启动（MVP 后，D27） | — |
| S12 | blog-web 作者中心（发文/编辑/自助治理） | §3、§4、§11（作者侧动作） | 2/4/5 前端面 | 前端 | ✅ 已验收（2026-10-08 用户确认；发文/编辑/发布/版本链路跑通，三条交互修正回归通过；blog-web `47b9e40`/`366488d`/`28ffc36`，零后端改动） | 2~4d |
| S13 | 线上部署与生产化 | — | — | 后端+前端 | ⏳ 待启动（D0 采购/备案先行，等待期并行 S14/S15） | 1~2d+备案 |
| S14 | GitHub/Gitee 社交登录 | §2.8（登录，复用） | — | 后端+前端(blog-web) | ✅ 已验收（2026-10-14；后端 4 commit + blog-web 5 commit，详 §3 S14；Apifox 用例补录留人工） | 2d |
| S15 | 阅读量 Redis 异步计数 | §5.2 | — | 后端(+blog-web 小改) | ✅ 已验收（2026-10-14 人工实测通过；后端 4 commit + blog-web 3 commit，联调期三联坑详见 §3 S15；Apifox 语义回归 2026-10-16 人工补录通过；JMeter 压测留作面试演示素材） | 1d |
| S16 | ES 搜索替换 MySQL LIKE | §6 | — | 后端(+blog-web 高亮 UI) | ✅ 验收关闭（2026-10-14 实测 + 2026-10-16 收尾：分词/越权红线/编辑同步/对账双向/零停机/LIKE vs ES 对比/深翻页游标全部实测通过；演示数据灌入实测后已执行清理，详 §3 S16） | 3~5d |
| S17 | PWA + SEO 三件套 | — | — | 前端(blog-web) | ✅ 已验收（2026-10-15 人工实测通过：Lighthouse SEO/Perf/A11y 100、SW 安装/预缓存/断网可读/回落/`/me` 红线全过；blog-web 6 commit 零后端改动，详 §3 S17） | 1~1.5d |
| S18 | 接口限流 | — | — | 后端 | ✅ 已验收（2026-10-15；三点位超限实感/无感回归/Redis 键核验人工实测通过；Apifox 三组超限用例 2026-10-16 补录通过，详 §3 S18） | 0.5d |
| S19 | plus-ui 登录页/后台首页改版 | — | — | 前端(plus-ui) | ✅ 已完成（2026-10-15；plus-ui 5 commit 零后端改动，详 §3 S19；2026-10-16 人工视觉验收与菜单隐藏均已完成） | 0.5~1d |
| S20 | Lenis 平滑滚动（动效收缩，D38） | — | — | 前端(blog-web) | ✅ 已验收（2026-10-15 人工确认：桌面手感/移动端跟手/reduced-motion 切换/PWA 安装态叠加全过；blog-web `bde6043` 零后端改动，详 §3 S20） | 0.5d |
| S21 | CI/CD 补全（可选） | — | — | 后端+前端 | ⏸ 可选 | 1d |
| S22 | 注册即登录（去掉注册后手动登录中间态） | §2.8（登录，复用） | — | 后端+前端(blog-web) | ✅ 已验收（2026-10-16；Agent E2E 六用例 + 人工浏览器验收均通过；后端 1 commit + blog-web 1 commit，详 §3 S22） | 0.5d |
| S23 | blog-web 全站视觉改版（方向 C） | — | — | 前端(blog-web) | ⏳ 待启动（D41；P1 补屏中，首页 1.1 版式已冻结） | 6~8d |

> 与 PRD §12.1 的顺序差异：评论提前到前台站之前，保证后端 API 闭环先行；
> S9 前台可在 S5 完成后于前端仓库并行启动（首页/列表/详情依赖 S2+S6+S8 的接口）。
> S9 范围含移动浏览器响应式适配（D27），独立移动端不再以网页形态重复建设，由 S11（uni-app）承接、MVP 后启动。

## 3. 切片详情

### S0 基线固化 🔄

已完成：sd-blog 模块 Maven 接入并被反应堆识别；7 张表代码生成（BO 主键齐全）；违反契约 §14 的 3 个生成 Controller 已停用；`blog-api.md` 升级 v0.2 冻结；AGENTS.md 重构；`application.yml` springdoc 已加博客分组（packages-to-scan: org.dromara.blog）。

待办：

- [x] 注册开关 `sys.account.registerUser` 已置 true（2026-09-28 后台参数设置操作）
- [x] Apifox 项目已建、Windows 侧连通验证通过；运行时 21 路径与保留 4 组 Controller 一致（2026-09-28 curl 核验）；已在 Apifox 覆盖导入清除陈旧的 ArticleTag 接口（2026-10-03）
- [x] git commit（c96e59d S0 状态收尾）

延后事项（不阻塞，转入 S5）：

- ⏸ 停用 Controller 的 3 组残留菜单暂不清理（点击 404 属视觉噪音，无功能影响；勿分配给非管理员角色）。S5 做正式 Admin 菜单规划时与 `docs/blog_s0_cleanup.sql` 一并执行。

完成标准：Swagger「博客模块」分组可见全部接口且可调用。

### S1 账号与个人资料 ✅（后端切片，2026-09-28 已验收）

范围：注册 / 登录 / 验证码走通（复用 RuoYi，仅确认配置）+ `GET/PUT /blog/user/profile`（契约 §8）+ `GET /blog/public/users/{userId}`（§9.1）。

决策点：注册开关 `sys.account.registerUser` 是否开启（建议开）。

完成标准：新用户注册 → 登录 → 填写 bio → 公共资料接口返回 userId/avatar/nickName/bio（头像昵称实时取自 sys_user，不复制）。

> 补记（2026-10-12）：本切片为**后端**切片（总览表第 25 行早已 ✅）。前台注册入口 `/register`
> 此前从未被任何切片显式承接——S1 只要求"确认后端配置"，S9 只交付登录弹层——已由 blog-web
> `03f926e` 补齐（账密 + 图形验证码，注册成功不自动登录，见该仓提交说明）。

### S2 文章 + 版本核心 ✅（后端切片，2026-09-30 已验收）

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
- Q4 遗留：`docs/blog_s3_cleanup.sql`（命中 3 篇 S2 冒烟文章）DELETE 段已于 2026-10-03 人工确认后执行

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

### S8 搜索与阅读量 ✅（已验收 2026-10-03）

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

验收（人工，2026-10-03 已完成）：重启 8081（IDEA 先 Rebuild）+ Apifox 五类用例全部通过（§5.1 补参 / §5.2 详情 / §6 搜索 / §9.2，全部匿名接口，无需菜单 SQL）。

完成标准：搜索仅返回公共可见文章；详情打开计数 +1；ES 替换不改变 API 契约。

### S9 前台站 ✅（2026-10-08 用户签收；独立仓库，本表仅登记依赖）

仓库形态（D21 已确认）：与后端、`plus-ui` **同级 clone** 的第三个独立仓库，GitHub 仓库名 `blogs-web`、本地目录 `blog-web`；
建仓后需向 `tooling/blog.code-workspace` 的 `folders` 追加 `"../../blog-web"`，并照 `plus-ui/AGENTS.md` 形式自带 AGENTS.md（只调公共与登录用户两层 API）。布局约定见 `docs/SETUP.md` §10。

端范围（D27 已确认）：**响应式单套代码**，PC 与移动浏览器通吃；移动浏览器适配在 S9 内一并交付，不另起移动 Web 切片。

依赖：S2（文章接口）+ S6（标签）+ S8（搜索）为最低可启动集；alias→canonical 的 301 由前台/边缘层负责（契约 §7.3）——选 Nuxt SSR 后由 Nitro server middleware 原生兑现（D28，边缘层方案作废）。

技术栈定案（D28）：**Nuxt 4**（官方 starter v4 模板）+ Vite/Vue3/TS/文件路由/自动导入/SSR 内置 + UnoCSS（照 plus-ui uno.config 裁剪）+ `@nuxtjs/seo` + `@nuxt/image` + shiki（服务端着色）+ `@nuxt/eslint`；**无组件库**，正文排版 preset-typography + 薄组件。

> 范围收编（D30）：上述集成清单中的 `@nuxtjs/seo`/`@nuxt/image`/shiki/`@nuxt/eslint` 与 three.js 装饰（桶B）已调为 **⏸ 按需 / MVP 后再定**；S9 实装 = `@unocss/nuxt` + `@nuxtjs/color-mode`，SEO 基线由 unhead 原生 `useSeoMeta`（title/description/og/robots/lang）承担。清单见 `blog-web/docs/plan.md` §2.6 与 §2.7。

开工准备清单（新会话/建仓首日照此执行）：

- [x] GitHub 建仓 `blogs-web`，同级 clone 为 `blog-web`（workspace 已预登记 folder，clone 后即生效）
- [x] `npm create nuxt@latest` 初始化（create-nuxt v4.0.0 已无独立 `-t v4` tag，官方 v4 starter 即默认 `minimal` 模板，装得 nuxt 4.5.2）；`.env`：`NUXT_APP_CLIENT_ID` = docs/blog_s9_web_client.sql 验证段中的 client_id
- [x] 执行 `docs/blog_s9_web_client.sql`（幂等，含验证段）新建 `web` 客户端记录（2026-10-06，见 SQL 状态表）
- [x] 挂 `@unocss/nuxt`（unocss 66.10.5）：uno.config 以 plus-ui 为蓝本裁剪（去 Element Plus 相关 `--el-*` 与 auto-import/`presetWebFonts`，Nuxt 原生；留 wind3/attributify/typography + 补 icons(presetIcons/mdi)，transformers 保留）
- [x] 自带 AGENTS.md（SETUP §10.4 三条：只调公共/登录读者两层、禁 `/blog/admin/**`、声明对 `../RuoYi-Vue-Plus/docs/` 权威源引用）+ 一个 `nuxt-frontend-coding` skill（`.codex/skills` 实体 + `.qoder/skills` symlink，覆盖新页面/SSR 数据获取、$fetch/Nitro 代理与 cookie 认证、alias 301、UnoCSS+prose、SSR 安全）；不预先建 .claude/agents 子代理
- [x] 页面清单（D28）：`/` `/page/{n}` `/post/{slug}` `/tag/{slug}` `/search` `/user/{userId}` + 登录/评论弹层；内容页 SSR，评论提交/登录交互 CSR（六条路由均已接真实数据；另加 `/tags` §7.5 标签页与 `/me` 作者中心入口占位；登录弹层含验证码，评论发表随 `/post` 落地）
- [x] 认证：cookie + Nitro server route 代理 `/auth/login`（httpOnly，SSR 可读物；顺带规避 CORS）——`server/api/auth/{login,logout,session}` 就位，token 只落 httpOnly cookie，clientId/grantType 服务端注入；`useAuth` SSR 读登录态（验证码链路属后续切片）
- [x] 开发环境：runtimeConfig 后端基址 `http://localhost:8081`（`NUXT_API_BASE` 覆盖）；`.env` 的 `NUXT_APP_CLIENT_ID` 映射 `runtimeConfig.appClientId`；Windows 浏览器访问注意 WSL2 localhost 转发既有处理（dev 日志实测 WSL IP `172.31.124.129`，转发失效时直连该 IP）
- [x] 部署：`nitro.preset: 'node-server'` + blog-web `Dockerfile`（多阶段，`.dockerignore` 排除 `.env` 密钥），docker-compose 新增 `web` 服务（`profiles: ["web"]` 门控，默认 `up -d` 仍只基础设施三件套），architecture.md 部署图同步（2026-10-07 实测：镜像 build EXIT=0、容器内首页 200 且 9 篇 SSR、`/tag/js` 容器内 301、镜像层无密钥残留）
- [x] 验收：alias 301 链路实测完成（`js`/`thinkphp`/`spring-boot` → canonical 301，canonical 自身 200 无重定向环）；移动视口与 SEO 首屏已经浏览器实测通过（M5：42 组宽度×路由零真实横向溢出、断点 16 观测点对齐 `design/responsive.html`、`lang`/`og`/`description`/正文均在 SSR 首屏）。**2026-10-08 用户已签收，S9 转 ✅**

实施结果（2026-10-07，blog-web 仓库，提交标 `S9-*`；2026-10-08 用户签收）：

- **读者功能（M1–M3）**：`/post` 详情(§5.2)+评论列表(§10.1)+发表评论(§10.2)、`/search`(§6)、`/user`(§9.1+§9.2 复用 §5.1 `authorId`) 全部 SSR 跑通；登录走 cookie + Nitro 代理并**复刻后端 api-decrypt 加密链路**（`api-decrypt.enabled=true` + `@ApiEncrypt`，AES-256-ECB + RSA/PKCS1，见 `server/utils/apiCrypto.ts`），用户人工验收登录与发评论通过。
- **桶A 视觉落地（8 步各一 commit）**：token 单一来源 `app/assets/css/tokens.css` + `uno.config` 全 `var(--…)`；`@nuxtjs/color-mode` 深浅与首屏无闪烁；内联双色 SVG 品牌与 favicon；全站 shell（容器两档 1080/68ch、登录态才显示作者中心）；静态 Hero（three.js 属桶B 保留 `<ClientOnly>` 槽位）；`prose` 正文；状态组件一套（骨架/空/错误/404）；响应式与全站数字分页。新增 `/tags`（§7.5）与 `/me` 入口占位；**TOC 与上一篇/下一篇因契约无接口未做**（已备案）。
- **验收期修的两条根因**：`@unocss/nuxt` 不注入 preflight → 缺 `box-sizing` 造成手机全站横向溢出；preset-typography 的 `.prose` 后注入覆盖 → 深色正文对比度仅 1.83:1（修复后 16.35:1）。
- **部署与文档**：`nitro.preset: node-server` + 多阶段 Dockerfile（镜像层实测无密钥）+ compose `web` profile 服务 + `architecture.md` §5 同步；本切片**零后端代码改动**（后端仅 infra 与文档）。
- 页面/组件粒度进度与偏差记录见 `blog-web/docs/plan.md`（本 ROADMAP 仍为状态权威源）。
- ⏸ 未纳入 S9 承诺的增强项（已按 D30 收编，只此登记不重复展开）：`@nuxtjs/seo`（sitemap/结构化数据）、`@nuxt/image`、shiki、`@nuxt/eslint`、three.js 装饰（桶 B，hero 已留 `<ClientOnly>` 槽位）。

### S10 管理后台前端（博客） ✅（plus-ui 仓库，2026-10-06 已验收）

背景（D23）：plus-ui 现有 `src/views/blog/*` 与 `src/api/blog/*` 是 S0 期（2026-09-28）代码生成器产出的 7 表 CRUD 骨架，调用 `/blog/blogXxx` 生成端点；该批 Controller 已按 D12/D16/D20 停用并物理删除，**这些页面当前全部失效（404）**，对应菜单也已被 s5/s6/s7 SQL 清理。后端 S2–S8 只交付到「契约 API + Apifox」，管理后台前端从未被任何切片认领。

范围（按真实契约路由重建，非普通 CRUD）：

- 管理员治理：`/blog/admin/articles`（下线/恢复/列表）、`/blog/admin/tags`（rename/merge/运营）、`/blog/admin/comments`（列表/逻辑删）
- 作者自助：`/blog/article`（我的列表/详情/保存/发布/hide/show/delete、发文编辑器 + 传图）、`/blog/user`（个人资料）
- 版本只读：嵌入文章详情的历史版本查看（`/blog/article` 侧），不做独立 CRUD 页
- 清理失效骨架：`blogArticleTag`（纯关系表）、`blogTagAlias`（并入标签管理）等无独立页价值的骨架

依赖：S2–S8 后端 API（已全部就绪）。细粒度实现清单见 `plus-ui/docs/frontend-plan.md`（派生自本切片，状态以本 ROADMAP 为准）。

边界（D24 原确认，经 D29 修订）：作者「发文/编辑」体验落 **plus-ui**（本切片已交付，保留），复用框架后台能力（AGENTS.md §5：所有注册用户天然可发文）；**D29 起 blog-web 亦增设作者中心**，登录用户可在前台自助发文/编辑/隐藏/删除自己的文章（走 `/blog/article/**`，见 S12）。两端共用同一作者自助接口，平台级治理仍仅 plus-ui。

红线：三层 API 边界不得混用（治理走 `/blog/admin/**`、作者自助走 `/blog/article|user/**`）；不信任前端权限判断，按钮隐藏仅为体验（契约 §1/§15、plus-ui/AGENTS.md）。

完成标准：管理员可完成下线/恢复/标签改名合并/评论删除，作者可发文/编辑/发布/隐藏，全部走对应契约接口且越权/校验用例通过；无残留失效骨架。

实施结果（2026-10-06 浏览器验收通过）：
- 失效骨架全部下线（ArticleTag/TagAlias/Article/Tag/Comment/UserProfile/ArticleVersion），无残留；plus-ui `vue-tsc` 全项目 0 错。
- 管理员治理页×3：文章（list/hide/restore + 详情）、标签（list/运营/rename/merge/remove）、评论（list/逻辑删），权限 `blog:*` 走菜单驱动（`blog_s10_menu.sql` 建 C 菜单）。
- 作者功能：我的文章列表、全屏发文/编辑页（Editor + 封面 + 标签 1~6 随文提交）、发布/hide/show/delete、历史版本只读；作者页走静态路由 `/blog-article`（免后端注册钩子，D24 落地）。
- 共享组件 `ArticleDetailDrawer`（详情 + 版本切换 + 标签），治理文章/评论页与作者列表复用。
- 衍生修复：D26 运营属性 MERGED 拒绝 + merge 精选身份转移；HtmlSanitizer 放行 img `oss://` 协议修复正文图片丢失。
- 遗留：D25（非超管治理角色看版本，条件触发，已挂账）；封面 URL 时效属平台层、非 S10。

### S11 移动端套件（uni-app）🔖（MVP 后启动，细节待展开）

立项决策 D27：uni-app 一套代码产出微信小程序 / H5 / App，补足小程序生态触点；不替代 S9 响应式 Web 的移动浏览器场景。

- 范围基线：读者套件——列表 / 详情 / 搜索 / 评论；不做发文与治理（S11 自身基线；D29 只把作者中心加到 blog-web，未纳入移动端，移动端发文入口仍走 plus-ui）
- API 复用 S9 同一套公共 + 登录用户接口，**零后端改动**（D21 公知事实）
- 认证：统一 `/auth/login` 携 `clientid`，`sys_client` 已预置 `app` 记录（android），纯配置零代码（SETUP §10.4 备案）
- 内容渲染：后端净化后 HTML 在小程序端需富文本适配（如 mp-html），启动时定实现
- 待启动时定：GitHub 仓库名与本地目录、页面级范围切分、建仓后工作区 folders 追加

### S12 blog-web 作者中心 ✅（blog-web 仓库，D29 修订 D24 后新增；2026-10-08 用户验收通过）

背景（D29）：原 D24 将作者发文/编辑只落在 plus-ui，导致 blog-web 读者想发文必须跳后台二次登录。用户决定反转：**blog-web 增设作者中心**，登录用户在前台自助管理**自己的**文章。

范围（全部走 `/blog/article/**` 作者自助层，**零后端改动**，接口 S2–S5 已就绪）：

- 我的文章列表：`GET /blog/article/my`（status/visibility/keyword 过滤 + 分页），行内动作 发布/hide/show/delete
- 新建/编辑：`POST /blog/article`（草稿）、`PUT /blog/article/{id}`（内容变化则建新版本）；**编辑器复用 WangEditor `@wangeditor-next/editor` + `@wangeditor-next/editor-for-vue` 6.4.1**，`<ClientOnly>` 包裹 + 按需加载（`ssr:false`），产出净化前 HTML（后端 jsoup 净化），**不上 Markdown**
- 封面/正文传图：复用 `/resource/oss/upload`（契约 §2.4，S3 已就位）+ `oss://` 引用解析（照 plus-ui `src/utils/ossContent.ts`）
- 发布 / 隐藏 / 重新公开 / 删除：`POST /blog/article/{id}/publish|hide|show`、`DELETE /{id}`（服务端按 `author_id` 校验）
- 版本只读（可选子视图）：`GET /blog/article/{id}/versions[/vId]`
- 认证：复用 blog-web 已登录 `web` 会话（cookie + Nitro 代理），**无需二次登录**

边界（红线不变）：

- **平台级治理仍只在 plus-ui `/blog/admin/**`**（他人文章下线/恢复、标签 rename/merge、评论审核），blog-web 禁止调用；作者自助管理自己的文章 ≠ 平台治理
- 不信任前端 `authorId`，归属与权限一律后端强制校验（AGENTS.md §5、契约 §1.2/§1.3）
- SSR 安全：WangEditor 是纯浏览器组件，作者编辑页必须 `<ClientOnly>` / `ssr:false`，不进服务端渲染；作者中心页面 CSR 为主，不参与读者首屏/SEO

依赖：S2–S8 后端接口（全部就绪）+ S9 blog-web 仓库与 `web` 认证链路（已就位）。

完成标准：登录用户可在 blog-web 完成发文→编辑→发布→隐藏→重新公开→删除全链路，越权（操作他人文章）被后端拒绝；编辑器仅客户端加载、SSR 不报错；不触碰 `/blog/admin/**`。

> 落地方式：本轮仅同步文档（本 ROADMAP、blog-api.md §15、blog-web/AGENTS.md、blog-web/docs/plan.md、docs/architecture.md、AGENTS.md §5）；**代码实现在独立开发对话进行**（新切片 S12，小步 commit 标 S12/D29）。

实施结果（2026-10-08，blog-web 仓库，零后端代码改动）：

- **S12-1 `47b9e40`**：作者自助代理层 `server/api/blog/article/[...path].ts`（catch-all + 方法/路径白名单，覆盖 §3.2~3.8 与 §4.1/4.2）；`/me` 我的文章列表：5 个状态 tab + 并发取计数 + 数字分页 + 骨架/空/错误态；`AuthorArticleRow` 动作可见性严格按契约状态机与 D13（ADMIN_HIDDEN 编辑禁用、不可自恢复）；`useRequireAuth` 客户端守卫复用 `LoginModal`（§2.8 锁定，不做 RBAC 动态路由）。
- **S12-2 `366488d`**：WangEditor 6.4.1（与 plus-ui 同版本）`ArticleEditor` + `TagChipsInput`(1~6) + `CoverField` + `ArticleForm`；`/me/new` 与 `/me/{id}/edit` 均 `ssr:false`+`noindex`；新建后自动转编辑路由避免重复创建；图片上传经本站 OSS 代理，前端不碰 token/clientid。实测 WangEditor 为独立 chunk（661KB）**不进读者首屏 bundle**。
- **S12-3 `28ffc36`**：`/me/profile`（§8.1/8.2，仅 bio 可改≤500，头像/昵称只读）+ `VersionHistory`（§4.1/4.2 折叠面板与快照弹层，**append-only 不提供恢复**）。
- **两个已备案偏差**：① 图片存**真实 URL** 而非 `oss://` 标记（D31，根因是解析接口需登录致匿名读者破图）；② mockup 的「更换头像」未做——属 `/system/user/**`，超出 blog-web 允许 API 层（红线），改为文案提示。
- 实测：`npm run build` 全绿；`/me`、`/me/profile`、`/me/new`、`/me/{id}/edit` 均 200；作者侧代理（my / detail / create / save / versions / profile）无 cookie 均 **401**，白名单外 **405**；读者首页 SSR 无回归。
- **验收结论（2026-10-08，用户确认）**：发文→保存→发布→编辑→历史版本→个人资料链路跑通；后续三轮为**样式与交互打磨**（对照掘金列表/写作页，见 `blog-web/docs/plan.md` §2.10），不再属于 S12 功能范围；MVP 至此 S0–S12 全部关闭，余下仅 S11（移动端，MVP 后）。

### S13 线上部署与生产化 ⏳（2026-10-13 规划；D0 采购/备案先行）

背景（D33/D37）：面试项目需要"可访问"的线上环境，用户倾向优先白嫖方案。**本切片最先启动的只有采购与备案时钟**（1~2 周等待），实际配置工作在等待期内或之后进行。方案细节（规格/带宽/成本/清单）见 `docs/cloud-deployment.md`。

依赖：无（S16 的 ES 上线需 4G+ 内存，选型时预留）。

范围与步骤：

1. 服务器 + 域名到位（白嫖 Oracle / 国内轻量，D37 拍板），提交 ICP 备案（大陆）或选香港节点；
2. 按 `docs/cloud-deployment.md` 执行：生产 compose 化（mysql / redis / minio 或云 OSS、nginx、ruoyi-admin、blog-web；S16 后加 ES）、nginx 反代 + Let's Encrypt HTTPS、安全组仅暴露 80/443、env 密钥注入、mysqldump 定时 + 一次恢复演练；完成后同步 `architecture.md` §5 部署拓扑（补生产形态，S9 同例）；
3. 后端镜像：`ruoyi-admin/Dockerfile` 已存在，构建验证；blog-web 镜像 S9 已交付，补生产 `NUXT_API_BASE`（内网服务名）与密钥 env；
4. OSS 决策落地（D31 生产约定：公有读 + CDN 域名，或 MinIO + CDN 回源）；
5. 上线冒烟：首页 / 详情 / 搜索 / 登录 / 注册 / 作者中心 / 后台登录；uptime 监控（UptimeRobot 免费档）。

验收清单：

- [ ] 域名 HTTPS 200，HTTP 跳转 HTTPS；
- [ ] 仅 80/443 暴露；3306/6379/9000/9200 内网 + 强密码（开发环境口令不带入生产）；
- [ ] 镜像层无密钥（`docker history` 抽验，S9 同法）；
- [ ] 冒烟七条全过；图片经 CDN/OSS 域无破图；
- [ ] 备份任务在跑 + 恢复演练一次；
- [ ] 备案号页脚展示（大陆）。

> D32 纪律：产物横跨三仓（后端 compose/镜像、blog-web env、plus-ui 构建），按仓分步提交，每仓独立可回滚。

### S14 GitHub/Gitee 社交登录 ✅（2026-10-14 验收关闭；2d）

背景（D34）：框架 JustAuth 全链路已内置，但**默认只支持"已绑定账号登录"**（`SocialAuthStrategy` 未绑定直接抛错，绑定入口又要求先登录）。读者场景需要"首次 GitHub 登录自动注册"，需小改造。**微信/QQ/钉钉因企业资质门槛不接入**（已评估边界，写进 README）。

依赖：无硬依赖（GitHub OAuth App callback 支持 localhost，本地可开发演示；上线后体验更完整，故排 S13 之后）。

范围与步骤（跨仓，按 D32 分两个提交周期）：

1. 后端（ruoyi-admin）：
   - `SocialAuthStrategy` 扩展：未绑定 → 自动建号（随机不可用密码 / 禁账密登录 / 默认角色 / 昵称冲突处理）→ 写 `sys_social` 绑定 → 直接登录（约 100 行）；**安全边界按红线人工复核**（AGENTS.md §9/§10）；
   - `application.yml` justauth 配置 GitHub/Gitee（client id/secret 走 env；redirectUri 指向 blog-web 落地页）；
   - `web` 客户端 `grant_types` 加 `social`（plus-ui 后台客户端管理修改，**不落 SQL 脚本**；`AuthController` 登录时会校验客户端 grantType，漏加直接拒）。
2. blog-web（细项见 `blog-web/docs/plan.md` §2.15）：
   - `LoginModal` 加 GitHub/Gitee 按钮，经服务端代理取 `/auth/binding/{source}` 授权 URL 跳转；
   - Nitro 落地路由收 `code/state` → 调 `/auth/login`（grantType=social，clientid=web）→ 种 httpOnly cookie → 跳回原页面；
   - 失败分支：授权失败 / 平台错误 / 账号停用的统一错误提示。

验收清单：

- [x] 新用户 GitHub 首次登录自动建号并进入登录态，`sys_user` / `sys_social` 记录正确（2026-10-13~14 实测：Gitee 首登建号+登录态全通；GitHub 首登在回调地址配为带 `?source=github` 后通过——平台对 redirect_uri 做含 query 的整串精确匹配，见下）
- [x] 二次 GitHub 登录直达已绑定账号（已绑定分支为框架原逻辑、本切片零改动；GitHub/Gitee 首登均走通全链路，同平台二次登录为同代码路径退化情况，人工可抽验）
- [x] 自动创建的账号**不能账密登录**（安全边界人工复核通过：随机 UUID 的 BCrypt 密码永不下发、任何途径不可获知，实现与复核点见 `SysLoginService#socialAutoRegister` 方法注释）
- [ ] 解绑（`DELETE /auth/unlock/{socialId}`）后该社交账号登录走本地账号（框架原逻辑零改动，待人工抽验；语义口径见 S14-D2：解绑后同平台再登录会**新建账号**而非回到旧账号。注：解绑入口仅 plus-ui「个人资料 → 第三方账号」，blog-web 无 UI、社交自动建号账号也无法登录 plus-ui——非用户可达路径，2026-10-16 备案为已知边界 D40；本项测的是框架原逻辑本身）
- [ ] Apifox 五类用例更新（正向 / 鉴权失败 / 参数校验 / 资源不存在 / 越权）——留人工（Agent 自测协议明确不做带真实登录态的 Apifox 全量用例）
- [x] plus-ui 登录页账密登录可用、SocialCallback 路由保留（本切片未触及 plus-ui，S19 改版纪律不变）

契约：无新增端点（复用 `/auth/login`、`/auth/binding/{source}`、`/auth/unlock/{socialId}`），`blog-api.md` 不动。

实施结果（2026-10-13~14，后端仓 4 commit + blog-web 仓 5 commit，均标 S14；D32 偏差备案：blog-web 侧经用户指示在本会话连带完成，仅动 blog-web 一个仓、独立 commit，后端仓未因之前端部分产生额外改动）：

- `SocialAuthStrategy.login` 未绑定分支由抛错改为调用 `SysLoginService.socialAutoRegister`（自动建号 + 绑定 + 直接登录）；已绑定分支、`/auth/social/callback` 绑定流程、`/auth/unlock/{socialId}` 解绑流程零改动
- `socialAutoRegister`：`@Lock4j(keys = authId)` + `@Transactional` 同方法（已核 lock4j-core 2.2.7：`LockAnnotationAdvisor` 为 HIGHEST_PRECEDENCE，**锁包裹事务**，锁内双查绑定后并发首登只建一号；与 workflow 模块既有用法一致）；随机 UUID 的 BCrypt 密码（禁账密登录）；不分配角色/部门（与 `/auth/register` 的 `registerUser` 同口径）；账号清洗 + 数字后缀 + `source_uuid` 兜底、昵称截断 + 随机后缀消歧（`sys_user.user_name` 与 `sys_social.auth_id` 均**无唯一约束**，已核 `ry_vue.sql`，冲突必须应用层消歧）
- 纯逻辑 `SocialAccountNamer`（`org.dromara.web.service.support`）+ 7 条 JUnit5 单测（ruoyi-admin 新建 `src/test`，根 pom 已统一 spring-boot-starter-test，无需改 pom）
- `application-dev.yml`：新增 `github` 条目、`gitee` 由模板假凭据改为 env 占位（`${GITHUB_CLIENT_ID:}` / `${GITHUB_CLIENT_SECRET:}` / `${GITEE_CLIENT_ID:}` / `${GITEE_CLIENT_SECRET:}`），redirect-uri = `http://localhost:3000/social-callback?source=xxx`（与平台注册的回调地址一致）；**prod 的 justauth 配置留 S13**（生产域名未定，避免空 redirect-uri）
- blog-web 侧（细项见该仓 `docs/plan.md` §2.15）：`server/api/auth/binding/[source].get.ts` 授权 URL 代理（source 白名单 + return cookie 防开放重定向）+ `server/routes/social-callback.get.ts` 落地路由（code/state 换 token 种 httpOnly cookie，302 回原页面，失败带 `social_error` 后端原文）+ `server/utils/authLogin.ts` 登录提交共用链路（login.post.ts 重构复用）+ LoginModal GitHub/Gitee 按钮 + `socialLanding.client.ts` 落地 toast
- 自测协议已跑：后端 `mvn compile -pl ruoyi-admin -am` 绿 + 单测 7/7 + 静态断言 + git diff 自审；blog-web `npm run build` 绿 + **编译产物抽验**（新增 server/utils 漏落盘时 esbuild 不报错、产物留裸标识符，纯 build 绿不足为凭）+ 生产产物实测（授权 URL/白名单 400/开放重定向防护/cookie 四例）
- ⏸ 人工待办（不在本切片代码内）：① plus-ui 后台「客户端管理」给 `web` 客户端 grant_types 加 `social`（**已完成**，用户 2026-10-13 操作）；② 开发工具为 **CodeBuddy（VS Code 内核 + Extension Pack for Java）**，非 IDEA——`.vscode/launch.json` 由 Spring Boot Dashboard 自动生成且自带 `envFile: ${workspaceFolder}/.env`，**仓库根 `.env` 填四个 client id/secret 变量**（两种启动方式均生效；`.env` 已入 `.gitignore`，不入库）；③ **账号创建逻辑人工复核**（红线 §9/§10，复核点已标注在方法注释）；④ 真实 OAuth 链路联调（**已完成**，双平台首登实测通过）与 Apifox 用例补录（留人工）
- 联调期实测修复（均已回归）：① GitHub 对 redirect_uri 做**含 query 的整串精确匹配**，OAuth App 回调地址必须注册为 `http://localhost:3000/social-callback?source=github`（Gitee 同理，用户已配）；② **SSR 登录态丢失根因修复**——SSR 阶段全局 `$fetch` 内部请求不转发浏览器 Cookie，session 端点恒返回未登录（影响社交登录落地/刷新/直达 `/me`；密码登录走 CSR 一直被掩盖），`useAuth` + `/me` + `/me/profile` 登录态 fetch 统一改 `useRequestFetch()`（blog-web `870ddd2`/`9c96847`）；③ dev 模式新增 `server/utils` 文件不刷新自动导入注册表，须重启 dev（已登记 blog-web plan）
- 已备案边界（S14-D1）：社交头像 URL 不落 `sys_user.avatar`（bigint OSS id）仅存 `sys_social.avatar`，社交注册用户的博客公共资料头像为空；同名软删用户不阻塞账号名复用（@TableLogic 过滤）
- 已备案边界（S14-D2）：社交自动注册**不写 `sys_user.email`**、不按邮箱归并账号——平台邮箱未验证，隐式归并有账号接管风险；同人多平台授权视为多个独立账号（身份键 `authId=source+uuid`，与框架绑定语义一致）；将来如需归并走「登录后 `/auth/social/callback` 主动绑定」显式功能，不做登录时隐式合并；GitHub scope 当前为空（仅公开邮箱返回），需要邮箱时再评估 `user:email`

### S15 阅读量 Redis 异步计数 ⏳（2026-10-13 规划；1d）

背景（D35，修订 D8 的"MVP 不上 Redis/异步"）：详情读接口不再每次写 DB 行锁，改 Redis 内存自增 + 定时批量回写。

依赖：Redis 已在 compose 运行；blog-web 需在详情代理补 visitorId cookie（跨仓小改，同切片第二个提交周期）。

范围与步骤：

1. `ViewCountService`（新；纯逻辑部分独立成类便于单测）：
   - 计数：Lua 脚本原子执行「窗口去重 SETNX（登录 userId / 匿名 visitorId，TTL 30min 可配）+ HINCRBY 待增量」；
   - 读：详情返回 `DB view_count + 待增量`（契约响应结构不变，语义登记 §6）；
   - flush：`@Scheduled(fixedDelay=10s)` HGETALL → 逐条 `UPDATE ... SET view_count = view_count + delta` → HDEL；
   - 降级：Redis 不可用 → catch 后直连 DB `UPDATE +1`（维持 S8 行为）。
2. blog-web：详情代理种 visitorId cookie（httpOnly，随机 UUID）；
3. redis 配置 `appendonly yes`（compose 挂载的 conf 目录）；
4. 单测（纯逻辑）：delta 合并 → 批量 SQL 构造、去重 key 生成与 TTL 参数。

验收清单：

- [x] 连续刷新：同一访客 30min 内只 +1；换访客 / 过期后 +1（2026-10-14 人工实测通过：Chrome 未登录首访 +1、之后不加；换浏览器 +1；登录态走 userId 去重正常）；
- [x] flush 后 DB 值与预期一致（人工实测：页面显示 12 / DB 11 → flush 后 DB 追平；`docker exec blog-mysql` 只读核验）；
- [ ] 杀进程重启：阅读量不回退、不跳变（DB 为唯一真相源、增量 UPDATE 语义上不回退；依赖 redis AOF 的极致场景留 S13 生产环境验证）；
- [x] 停 Redis：详情仍 200，降级直写 DB（codec 故障期间全量请求实际走过降级路径、详情持续 200，等效验证）；
- [ ] 并发刷新无负值（HINCRBY/HGETALL+DEL/`view_count + delta` 均为原子增量语义，无减法路径；JMeter 压测留作面试演示素材）；
- [x] Apifox 详情用例语义更新并回归（2026-10-16 人工完成：viewCount = DB + 待增量，列表页仍只读 DB、展示可能 ≤10s 滞后，语义已按契约登记）。

实施结果（2026-10-14，后端仓 + blog-web 仓各一提交周期，均标 S15；D32 偏差备案：经用户指示两半场同会话完成，blog-web 仅动详情代理、middleware 与 utils，独立 commit）：

- 后端新增 `ViewCountService`/`Impl`（sd-blog）：两段 Lua——计数「`SETNX NX EX` + `HINCRBY`」同 RTT 原子完成去重与计数；flush「`HGETALL`+`DEL`」同脚本原子 drain，避免先读后删丢增量；10s 定时逐条 `view_count = view_count + delta`（`del_flag='0'` 守卫，**人工复核点**）；Redis 异常 catch 降级直写 DB +1（S8 行为）；无访客标识同样直写
- 读路径零 DB 写：`queryPublicDetail` 改调 `countAndGetViewCount(articleId, userId, visitorId, dbViewCount)`——库内值由调用方已读文章行传入，**不新增 SELECT**；返回 `DB + 待增量`；登录态经 `LoginHelper.isLogin()` 取 userId 优先（公开端点转发 token 时可用）
- 访客标识：controller §5.2 加可选 `visitorId` 参数（D19 的 required=false 防 500 约定）；blog-web 详情代理种 `blog-web-visitor` httpOnly cookie（`crypto.randomUUID()`，1 年）并 query 转发，已登录时经 `blogGet` 新增可选 headers 转发 token
- 纯逻辑 `ViewCountSupport`（support 包，零框架依赖）：键生成/TTL 换算/扁平配对（畸形逐条跳过）/批量排序/读值合并/访客键优先级 + 7 条 JUnit5 单测全绿（sd-blog 新建 src/test，根 pom 已统一 test 依赖）
- 基建：`BlogSchedulingConfig` 最小 `@EnableScheduling`（全仓此前无该基建）；`blog.view.dedup-ttl-minutes:30`/`flush-interval-ms:10000` 入 application.yml（`@Value`/`fixedDelayString` 同名默认值兜底）；`SdBlogArticleMapper.addViewCount` 增量 UPDATE（`increaseViewCount` 保留供降级复用）
- 契约 `blog-api.md` §5.2 已按 D35 修订（**先报告冲突再修订**：原「每次打开 view_count +1」及「MVP 不做 Redis 热点计数/异步刷盘」与新语义冲突）：新增可选 `visitorId` 参数表、重写「阅读量」条款（窗口去重/异步回写/降级/滞后说明）、收窄「不做」清单为 IP 去重与设备指纹；响应结构不变，头部版本说明补记
- 自测协议：`mvn compile -pl ruoyi-admin -am` 绿 + 单测 7/7 + 静态断言 + git diff 自审 + **redis-cli EVAL 实证两段 Lua 语义**；blog-web `npm run build` 绿
- **联调期修复（2026-10-14，Redisson Lua codec 陷阱，必修）**：首轮联调「每次刷新都 +1」——后端日志 `ERR value is not an integer or out of range` 全部走降级。根因：本仓 Redisson 全局配置为 `CompositeCodec(StringCodec, TypedJsonJackson3Codec, ...)`（`RedisConfig`），而 `RedissonClient#getScript()` 默认继承全局 codec，**脚本 VALUE 参数按 JSON 编码**（"1800" → 带引号的 6 字节垃圾值），Lua 的 `SET ... EX ARGV[2]` 直接报错；**key 走 CompositeCodec 首位 StringCodec 看似正常，极具迷惑性**（生产日志里键名完全正确）。修法：所有 Lua eval 显式 `getScript(StringCodec.INSTANCE)`；并把 pending 读取合并进计数脚本（`HGET` 收尾，单次 RTT），顺带消除 RMap 读取的同源 codec 隐患。已用「Redisson 直连真实 Redis 的临时复现测试」四变体对比实证（String/Long/Integer 参数均失败、StringCodec 全过），复现测试已删；**全仓仅本功能使用 Lua，无其他历史中招点**。今后任何 Redisson Lua 调用必须先显式指定 StringCodec
- **联调期第二/三坑（2026-10-14，blog-web 侧，均已修复验证）**：② SSR 取数用全局 `$fetch`，内部请求不转发浏览器 Cookie（S14 useAuth 同款坑第二现场）→ 详情代理读不到 visitorId/token，刷新页面去重永不命中；修：`/post/[slug].vue` 文章 useAsyncData 改 `useRequestFetch()`；③ **Nitro 内部请求响应里种的 Set-Cookie 不冒泡到外层 HTML 响应**（实测 SSR 文档响应无 set-cookie、直连 API 有）→ 在详情代理里种 cookie 浏览器永远收不到（Edge 未登录正常只因曾 SPA 导航时被 API 响应种下 cookie，Chrome 未登录从未 SPA 导航故全中）；修：visitor cookie 改由 `server/middleware/visitor-cookie.ts` 在最外层种植（`server/utils/visitor.ts` 种/读单点），代理只读。**三坑共性（面试叙事）：每层单元都正常、curl 直打都正常，坏的全是层间链路（codec 序列化方向、内部请求 cookie 方向、Set-Cookie 冒泡方向）——联调期查 Redis 键形态与响应头比任何单测都直接**
- 备案边界（S15-D1）：① 列表页 viewCount 仍只读 DB（滞后 ≤10s，与掘金等站行为一致）；② 无访客标识（未登录且无 visitorId，如裸调 API）→ 每请求 +1 无去重（与 S8 一致）；③ 单条 flush DB 失败丢该 delta（已 drain 无法回退，D35 接受取舍，记 error 日志）；④ 宿主 `/docker/redis/conf/redis.conf` **未开 AOF**（仓库外文件，用户手动开 `appendonly yes`+`appendfsync everysec` 并重启容器，会丢内存中未 flush 待增量，dev 可接受）；⑤ 生产 redis AOF 属 S13 部署清单

### S16 ES 搜索替换 MySQL LIKE ✅（2026-10-13 规划；3~5d；D36 已关闭：服务端高亮；2026-10-14 两半场代码完成，2026-10-16 验收关闭）

背景（D19 预留）：`ArticleSearchService` 接口注释已写明"引入 ES 时替换实现即可，不改变契约"。本切片兑现该预留。

依赖：S13（线上 ES 需 4G+；本地 WSL2 16G 无压力）。D36 已关闭：服务端高亮（含 `blog-api.md` §6 契约变更，实现前先修订）。

范围与步骤：

1. 基础设施：compose 加 ES 8 单节点（`ES_JAVA_OPTS=-Xms512m -Xmx512m`，xpack.security 按需关闭或设密码，宿主机 `vm.max_map_count` 调够）；IK 分词插件用自建 Dockerfile 安装（版本匹配，**第一步先验证网络/安装可行性**，失败退 OpenSearch）；
2. 索引：mapping（`articleId` keyword / `title` text(ik_max_word, boost) / `summary` / `content`（净化后纯文本）/ `tags` keyword / `publishedAt` date）+ alias `blog_article`；**只索引公共可见文章**（发布/重新公开入索引，隐藏/下线/删除删文档——三条件内建为索引生命周期，查询端不再过滤）；
3. 同步：文章 Service 事务成功后发 Spring 事件 → 同步 ES；对账定时任务（比对公共文章集合）兜底；
4. 查询：`EsArticleSearchService` 新实现——ES 返回 id + score → 批量回表 MySQL 拼 VO（复用 `ArticleTagFiller`）→ 深翻页用 `search_after`；
5. 零停机重建演示：建 v2 → reindex → 原子切 alias；
6. 演示数据：20~30 篇中文技术文章（脚本化、标注可清理）+ 灌 1~5 万篇压测数据跑 LIKE vs ES 延迟对比（**用完清理，清理方式随切片登记**）；
7. 高亮（D36 服务端方案）：**先修订 `blog-api.md` §6**（`/blog/public/search` 响应新增可选 `highlight` 字段，向后兼容）再实现——ES `highlight` 查询取 title/summary/content 匹配片段（`<em>` 包裹；ES 默认编码器会转义片段内其余 HTML，前端可信任）；blog-web 搜索页渲染片段（`ArticleCard` 摘要位，命中词高亮）。

验收清单：

- [x] 中文分词实测："Python爬虫"命中"Python 爬虫"；自定义词典技术词整词匹配（2026-10-14 Agent 实测：Python爬虫→「Python 爬虫入门」、tech.dic 词条 K8s 整词命中）；
- [x] 草稿 / 用户隐藏 / 管理员下线 / 逻辑删除文章**不出现在搜索**（红线核验：三条件内建于索引生命周期 + 回表三条件同源；Apifox 越权类用例 2026-10-14 用户验收通过）；
- [x] 编辑内容后索引同步更新（title/summary/content 变化均触发）（8 写方法事件全覆盖 + 对账兜底；2026-10-14 实测：直改 DB visibility 被对账自动修正，活体演示见人工待办 ④）；
- [x] alias 切换零停机；对账任务能发现并修复不同步（2026-10-14 实测：建 v2 → reindex 20040 篇 2.3s → 原子切 alias，全程并发 55/55 请求全 200）；
- [x] LIKE vs ES 对比数据留档（2026-10-16 用户确认 MySQL 侧跑数完成；ES 侧数据 2026-10-14 已填入 `blog_s16_benchmark.md`：高频词 ~21ms/2 万篇、低频 17.6ms、空结果 5.7ms）；
- [x] **契约先行**：实现前完成 `blog-api.md` §6 修订（响应新增可选 `highlight` 字段，D36）；Apifox 五类用例全过（含高亮片段抽查：命中词 `<em>` 包裹、其余内容转义）（2026-10-14 用户验收通过）；

实施结果（2026-10-14，后端仓 7 commit + blog-web 仓 1 commit，均标 S16；两半场同会话经用户确认，S14/S15 先例）：

- **契约先行（先于全部代码）**：`blog-api.md` §6 重写——响应新增可选 `highlight`（title/summary/content 片段数组，`<em>` 包裹，ES 默认编码器转义其余 HTML）与顶层可选 `nextAfter` 深翻页游标、请求新增可选 `after`；ES 同步语义/排序口径/降级行为一并登记（2 commit：b977daf6/9cbbf970）。
- **ES 基建**：`script/docker/es/`（ES 7.17.28 + IK 7.17.28 经 ghproxy 镜像安装——直连 GitHub release 实测 404；IKAnalyzer.cfg.xml ext_dict + tech.dic 79 条技术词典）+ dev compose `es` 服务（512m 堆/单节点/9200）；`blog.search.engine`（默认 es）/`blog.es.address`/`blog.index.reconcile-cron` 等配置段。
- **索引管理**：`BlogEsConfig` 裸 `elasticsearch-java:7.17.28` 客户端 bean（S16-D2）+ `ArticleIndexManager`（mapping：articleId keyword / title ik_max_word / summary、content ik_smart / tags keyword / publishedAt date；settings：1 shard / max_result_window=50000；alias `blog_article`；建 v2 → `_reindex` → 单请求原子切 alias 的零停机三步）。
- **同步链路**：`SdBlogArticleServiceImpl` 8 个写方法成功后发 `ArticleChangedEvent` → `ArticleIndexListener`（AFTER_COMMIT + `fallbackExecution=true` 兼容 5 个无事务注解方法）→ `ArticleIndexService.indexOrRemove` 按 DB 现状重推导（公共可见三条件内建于索引生命周期；`@TableLogic` 使软删文章 selectById 为 null → 直接删文档）；keyset 分页 `initialFullSync` + `@Scheduled` 对账（MySQL 公共文章 id 集合 vs ES 文档 id 集合 diff，search_after 全量拉 id）。
- **查询**：`EsArticleSearchService`（@Primary + @ConditionalOnProperty(engine=es)）——multi_match（title^3，查询侧 ik_smart）+ `_score desc, publishedAt desc, articleId desc` 排序 + 服务端高亮（title/summary 整字段、content 3 片段×200 字符）→ 命中 id 回表 `selectPublicPage`（**三条件内建，堵 ES→DB 竞态窗口**）→ `EsSearchSupport` 纯逻辑按 ES 序重排（防御性丢弃命中序外行）+ 合并高亮 + ArticleTagFiller 填 tags；深翻页 = from+size ≤ 5 万窗口，越窗用 `after` 游标（`nextAfter` 透传，编码 score|publishedAt(epochMilli)|articleId）；ES 异常降级 MySQL LIKE（限频告警，响应无 highlight）。
- **演示数据**：`blog_s16_demo_data.sql`（24 篇中文技术文 + 2 万压测，recursive CTE 生成，ID 全落 9e18 标记段、slug 带 s16- 前缀防撞键）+ `blog_s16_cleanup.sql`（含 ES 清理两法）+ `blog_s16_benchmark.md`（LIKE vs ES 对比方法与命令，结果表留人工实测填写）。
- **blog-web 半场**：`types/blog.ts` 增 `ArticleHighlight`/`ArticleSummary.highlight?`；`ArticleCard` 标题/摘要位渲染命中片段（摘要未中降级渲染正文片段；`v-html` 安全依据 = 契约"ES 已转义"，样式走 `--accent-fg` token + scoped `:deep(em)`）；`npm run build` 绿。
- **自测协议已跑**：`mvn compile -pl ruoyi-admin -am` 绿；sd-blog 单测 55/55 全绿（新增 EsSearchSupportTest 7 条 / IndexReconcileSupportTest 5 条 / ArticleIndexDocBuilderTest 4 条）；静态断言（8 写方法 publishEvent 齐全、三条件三处同源、5 个 ES bean 条件注入）；git diff 自审无越界改动。
- **联调期实测修复（2026-10-14，均必修）**：① **refresh-immediate 默认值反转**（true→false）——单文档写 + `refresh=true` 同步等 refresh（最长 ~1s/篇），2 万篇初始同步被拖到 20 分钟级（表象="一直循环打印 SQL"，实为慢速推进非死循环）；修 = 批量走 `_bulk` 单请求一批 + 批间不 refresh，`indexBatch`/对账补索引改造后 2 万篇秒级，事件路径靠 ≤1s 自然刷新（顺带消除 AFTER_COMMIT 同步监听对写接口的 +1s 延迟）；② **AGENTS.md §8 陷阱再现（CodeBuddy 变体，凶器=JDT 非 IDEA）**——`EsArticleSearchService` 回表段把 MyBatis-Plus `Page`（getRecords）误写成 RuoYi `PageResult`（getRows），JDT 把带错 class 写进 target/classes 且时间戳新于源码 → **mvn 增量编译跳过该文件、坏产物混入 jar**，运行期 `java.lang.Error: Unresolved compilation problems`（500 的真因）；`mvn compile` 多次"全绿"均因跳过而失真，**clean 全量重编后才暴露**；修 = 源码类型改正 + clean package，jar 内 `Unresolved compilation` 抽验归零（教训：IDE 开着时 mvn"全绿"不足为凭，排查以 clean 产物为准；另注意编辑器旧缓冲区回写会静默回退外部编辑，提交前先核对 git diff）；③ **tag_id 段越界**：9.5e18 超 BIGINT 上限（9.22e18）致 SQL 1264，降 9.2e18 段（a346fb22）；④ 高亮 VO 类级 `@JsonInclude(NON_NULL)` 在 Jackson 3（tools.jackson）下未按预期省略未命中字段（字段级生效、类级不生效，未深挖）——契约 §6 措辞放宽为"null 或不出现"，前端 `?.[0]` 空安全；⑤ **total=10000 系 ES 默认 `track_total_hits` 计数上限**（relation=gte 下界，非真实命中数），查询已显式 `track_total_hits=true` 修为精确总数（同词实测 20001，eaf9dcaa4）。修复后实测：搜索 200 + 打分序 + `<em>` 高亮 + tags 回填全链路通；「Python爬虫」命中「Python 爬虫入门」、tech.dic 词条（K8s）整词命中，两项分词验收提前通过。
- ⏸ **人工待办**（不在本切片代码内，按序）：① ~~ES 容器搭建~~（已完成：blog-es 运行中，20040 篇已入索引）；② ~~IK 中文分词实测~~（已完成：Python爬虫/K8s 两用例 Agent 实测通过，人工浏览器复验可选）；③ ~~演示数据灌入~~（已完成：blog_s16_demo_data.sql 执行成功，验证段 20024/20024/30 符合预期）；④ ~~对账兜底验证~~（已完成：用户直改 DB visibility——隐藏后被对账删文档、恢复后等下个 :00/:30 周期自动补回，MySQL 20040 vs ES 20039 恰差 1 即待补项，最终一致 S16-D4 活体演示；需重启后端使 track_total_hits 修复生效后再看恢复结果）；⑤ LIKE vs ES 对比：ES 侧 Agent 已实测并填入 `blog_s16_benchmark.md`（高频词 ~21ms/2 万篇命中、低频 17.6ms、空结果 5.7ms，total 已精确计数）；**MySQL 侧待人工**：切 `blog.search.engine=mysql` 重启后用该文档 §3 的一键循环跑数填表；⑥ ~~深翻页 after 游标实测~~（已完成 2026-10-14：越窗守卫生效 + 游标时区偏差修复后，游标第 2/3 页与 pageNum 第 2/3 页完全一致、无重叠、可续翻）；⑦ ~~零停机重建演示~~（已完成：建 v2 → reindex 20040 篇 2.3s → _aliases 原子切 v1→v2，全程并发 55/55 请求全 200，实证留档 benchmark.md §4；旧 v1 可择机 `DELETE /blog_article_v1`）；⑧ ~~Apifox 五类用例~~（2026-10-14 用户验收通过）；⑨ ~~blog-web 浏览器验收~~（2026-10-14 用户验收通过：高亮渲染 + 视口）；⑩ ~~演示收尾跑 `blog_s16_cleanup.sql`~~（已完成 2026-10-16：用户确认演示数据已执行清理——查库 `sd_blog_article` s16- 前缀 0 条；SQL 状态表已登记）。

### S17 PWA + SEO 三件套 ✅（blog-web；1~1.5d，2026-10-15 验收关闭）

依赖：S13（HTTPS 下完整体验）；无后端改动。细项见 `blog-web/docs/plan.md` §2.16。

范围与步骤：

1. `@vite-pwa/nuxt`：manifest + Workbox 策略——app shell precache / 文章页 stale-while-revalidate / API network-first；离线回退页；**`/me/**` 与需登录 API 不进 SW 缓存**（红线）；
2. SEO 三件套（不引整模块，Nitro route 手写更轻）：`sitemap.xml`（查公共列表生成，含 lastmod）、`rss.xml`（RSS 2.0 最新 N 篇）、JSON-LD `Article`（`/post`，在现有 `useSeoMeta` 基线上扩展）；`robots.txt` 声明 sitemap；
3. 验收：Lighthouse（本地 Chrome）PWA + SEO 分数留档；断网刷新已加载文章可读；view-source 抽验结构化数据；移动视口回归；iOS 无 SW 的限制写进 README（实事求是）。

实施结果（2026-10-14 实施 / 2026-10-15 人工验收并修复，blog-web 仓 6 commit 均标 S17：`1437ad2` SEO / `7237ea1` PWA / `d6979ec` 文档 / `9195294`+`0f0a57e` 验收期修复 / `fa065f1` 留档更新；零后端改动）：

- **SEO 三件套**：`/sitemap.xml` 为 sitemapindex + 分块 `/sitemap/{n}.xml`（500/块，2 万演示数据 → 41 块，`blog_s16_cleanup.sql` 执行后自然收缩；服务端 `defineCachedEventHandler` 10min + `Cache-Control`）；`/rss.xml`（RSS 2.0 最新 20 篇，RFC822 pubDate + XML 转义）；`/post` 页 JSON-LD `Article` + canonical + ogUrl/ogImage（useSeoMeta 基线只增不改）；`robots.txt` 静态文件改 Nitro 动态 route（注入运行时 `siteUrl` 的 Sitemap 绝对行 + `Disallow: /me/`）；`runtimeConfig.public` 新增 `siteUrl`（`NUXT_PUBLIC_SITE_URL`，S13 部署配真实域名）。
- **PWA 架构决策**：`@vite-pwa/nuxt@1.1.1`（→ vite-plugin-pwa 1.3.0，兼容本仓 vite 8）+ **injectManifest 手写 `app/sw/sw.ts`**——generateSW 的 navigateFallback 是 SPA app-shell 语义，与 SSR 站「文章页 SWR + 离线回退」互相遮蔽，手写 SW 才能逐条审计红线。SW 规则：① precache 清单（app shell + offline.html + 图标 + `_nuxt/builds`）；② 文档导航**白名单** SWR（`/`、`/page/`、`/post/`、`/tag/`、`/tags/`、`/user/`、`/offline.html`——白名单构造天然排除 `/me/**`、`/register`、`/social-callback`）；③ `/api/blog/public/**` NetworkFirst；④ 认证 API 不注册规则直通（cookie 不落缓存）。
- **验收期两处模块生态坑（人工首测发现 SW 全部不生效：DevTools redundant、零缓存、断网全失败；无头 Chrome 复现 + 打桩 dump + 二分定位，均修复并全链路复验）**：① **`@vite-pwa/nuxt` 生产默认 `manifestTransforms` 面向预渲染路由页把 `.html` 条目按 clean-URL 改写**——`offline.html` → `offline` 致预缓存 404、SW 安装被 Chrome 静默丢弃；修法=自定义 `manifestTransforms`（保留 `.html` 原 URL，复刻模块对 `_nuxt/builds` 的 UUID-json 去 revision 处理）。② **顶层 `self.skipWaiting()/clientsClaim()` 致 Chrome「script evaluation failed」、注册同样被静默丢弃**（Node 桩求值却正常，极具迷惑性）；修法=移入 `activate` 事件。③ 离线回退 `caches.match` 匹配不到带 `?__WB_REVISION__` 的预缓存键 → 改 `matchPrecache`。
- **M11 移动视口回归（人工截图发现）**：窄屏顶栏下滑隐藏后 `top-16` 悬浮元素孤悬——阅读进度条经包裹层随顶栏同步上移+淡出（内层 scaleX 与外层位移 transform 分离）；作者中心编辑器吸顶操作条 `top` 收回 0 贴住视顶；桌面媒体查询零回归（1440px 实测）。
- **验收结果（2026-10-15 人工实测）**：Lighthouse（Windows Chrome）**SEO 100 / Performance 100 / Accessibility 100 / Best Practices 96**——**PWA 类别自 Lighthouse 12.0（2024-05）起已移除**，可安装性改由 Chrome 安装图标 + DevTools Application 核验；SW activated and is running、49 条预缓存（workbox-precache/sd-pages/sd-public-api 三缓存）；断网刷新已读文章 200 完整渲染、未读回落 offline.html、直开 offline.html 200；`/me` 与 `/me/new` 停留后缓存仅 3 条公共匿名端点（红线人工复核通过）；view-source JSON-LD 抽验通过；SEO 端点（sitemap 索引/分块/rss/robots）人工复核通过。Agent 侧另以无头 Chrome 全链路复验（安装→激活→断网三场景→`/me` 零泄漏）。
- ⏸ **人工待办**（不在切片代码内）：① Chrome 地址栏安装图标按钮级安装验收；② iOS Safari 限制描述人工确认（无设备时以 README 措辞为准）；③ S13 部署时配 `NUXT_PUBLIC_SITE_URL` 真实域名；④ 面试前执行 `blog_s16_cleanup.sql` 后 sitemap 从 41 块自然收缩的验证（可选）。iOS 无 SW 限制已写入 `blog-web/README.md` S17 章节。

### S18 接口限流 ✅（后端切片，2026-10-15 已验收）

依赖：无（Redis 已在位；框架 `@RateLimiter` / `RedisUtils.rateLimiter` 零 setup 开箱可用）。

范围与步骤（照单完成）：

1. 限流点：`/auth/login`（IP+username 键，防暴力破解，与 S14 成对）、发表评论（用户键）、`/blog/public/search`（IP 键）；
2. 规则配置化（application.yml 或 sys_config，不硬编码）；
3. 超限行为确认（框架默认拒绝码与文案）并在 Apifox 登记用例。

验收：超限返回符合预期且正常用户无感；限流键与阈值文档化（§6 备案）。

实施结果（S18-D1，形态决策与键阈值备案见 §6）：

- **形态**：开工前拍板放弃 `@RateLimiter` 注解（`count/time` 编译期字面量读不到 application.yml，违背本切片要求 2），选「`blog.rate-limit.*` 配置化阈值 + `RateLimitChecker` 薄封装直调框架 `RedisUtils.rateLimiter`（Redisson `RRateLimiter`）+ 复用框架 i18n `rate.limiter.message`」；超限行为与注解路径**完全一致**：`ServiceException` → `GlobalExceptionHandler` → `R.fail`，即 **HTTP 200 + envelope code 500 + msg「访问过于频繁，请稍候再试」（不是 429）**；Redis 异常同口径包装 `RuntimeException("服务器限流异常，请稍候再试")`（与 `RateLimiterAspect` 逐行对齐）。
- **接入点**（均在方法体首行/校验后，三个调用点各一行 + 注释）：
  - `AuthController.login`：`ValidatorUtils.validate` 之后、client 校验之前（错误 clientId 的探测请求也计入配额）；username 从加密内层 `PasswordLoginBody` 二次解析取得（小 JSON 成本可忽略），非密码 grant 无用户名字段时退化为同 IP 共享键；
  - `BlogArticleController.comment`：按 `LoginHelper.getUserId()`；与 `@RepeatSubmit` 互补（防重挡同内容短周期重复、限流挡高频不同内容）；
  - `BlogPublicSearchController.search`：按客户端 IP；加在 Controller 层不影响引擎选择（mysql/es 均覆盖），`@NotBlank` 校验先行、空关键字不计配额。
- **新增**（`org.dromara.blog.ratelimit`）：`RateLimitKeys`（纯静态键组装，键前缀复用框架 `global:rate_limit:`）+ `RateLimitChecker`（`@Value` 绑定 `blog.rate-limit.*`，内联默认值与 yml 同源，遵循本模块 S15/S16 约定）；单测 9 条（`RateLimitKeysTest` 4 条键形态 + `RateLimitCheckerTest` 5 条契约：三点位键/阈值/timeout 绑定、超限文案、Redis 异常包装，mock Redisson 范式同 `RedisAspectContractTest`）。
- **零前端改动**：blog-web 对 `R.fail` 已有处理路径（超限提示直接可见）；契约 `blog-api.md` 无变更（不改出入参，仅新增服务端防护行为）。

验收结果（2026-10-15 人工实测通过；前置：重启 8081 加载新码，无 mapstruct ConvertException）：

1. **三点位超限实感**：搜索第 31 发、评论第 11 发、登录第 6 发均返回 HTTP 200 + code 500 + 「访问过于频繁，请稍候再试」；前 N 发均穿过限流器到达业务层——评论打不存在文章 ID（`999999999`）返回「文章不存在或不可评论」零脏数据；登录用不存在用户名 `s18_probe`（规避 5 次错误锁账号），第 1~5 发返回「用户不存在」；
2. **正常用户无感**：窗口过后单次登录/评论/搜索全部恢复；真实账号 `admin` 登录成功且独立成键；换用户名 `s18_probe2` 连打同样第 6 发被限——键的 username 维度实证；
3. **Redis 键核验**（实测键与 §6 S18-D1 备案逐一吻合）：`global:rate_limit:auth/login:127.0.0.1:s18_probe`（IP+username）、`global:rate_limit:blog/article/comments:1761100000000000001`（userId）、`global:rate_limit:blog/public/search:0:0:0:0:0:0:0:1`（IPv6 回环 `::1`——客户端地址族由框架 `getClientIP` 如实提取，非异常）、`global:rate_limit:auth/login:127.0.0.1:admin`（无感验证的真实登录）；另观测到框架既有键 `global:rate_limit:/auth/code:127.0.0.1:`（CaptchaController 的 `@RateLimiter`，requestURI 形态），与 S18 三点位共存于同一前缀——"一个前缀统一观测"的运维口径实证。

遗留人工事项：✅ Apifox 三组超限用例补录（2026-10-16 人工完成，用户确认；断言口径 HTTP 200 + code 500 + 「访问过于频繁，请稍候再试」）。

### S19 plus-ui 登录页 / 后台首页改版 ✅（plus-ui；0.5~1d，2026-10-15 完成）

背景：后台首页与登录页仍是框架默认样式，与博客项目气质不符；与业务逻辑零关联，排最后。细项见 `plus-ui/docs/frontend-plan.md` §4。

范围与步骤：

1. 登录页博客化改造（Impeccable skill，**必须按项目 token 体系重构**，不照抄默认）；**保留登录逻辑、验证码链路、SocialCallback 路由不动**；
2. 后台首页：博客数据概览（文章/评论/标签计数、待治理评论，调 `blog:article:list` 等已有接口）或静态欢迎页（开工前拍板）；
3. 响应式与框架风格保持一致。

验收：浏览器验收（桌面 + 移动视口）；`vue-tsc` 0 错 + 构建通过；治理页/作者页无回归；不动权限与路由结构（三层 API 边界红线）。

开工拍板（2026-10-15 用户确认）：① 后台首页形态 = A 博客数据概览；② 品牌名 = 「Stack Depth · 博客管理后台」（与 blog-web 同源）；③ 两个 `.env` 的 `VITE_APP_TITLE` 同步改品牌名。

实施结果（plus-ui 5 commit，零后端改动）：

1. 登录页（`a4aee4b`）：品牌区/表单标题/特性 chips 博客化；登录逻辑、验证码链路、社交登录、rememberMe、SocialCallback 路由一行未动；960/640 响应式与深色渐变基调保留。
2. 后台首页（`a4aee4b`）：静态欢迎页 → 数据概览。6 统计卡（文章总数/已发布/草稿/隐藏中〔用户隐藏+管理员下线，hint 拆分〕/评论总数/标签总数）全部调已有 `blog:admin/**` 接口，`pageNum=1&pageSize=1` 读 `total` 约 7 发并发（`Promise.allSettled`）；`checkPermi` 预判 `blog:article:list`/`blog:comment:list`/`blog:tag:list`，无权限不发请求并展示降级提示条；单点失败该卡渲染「—」不影响其他卡；另加三层接口边界说明卡（纯文案无深链）。样式全走 `--app-*` token（移除旧页 28px 魔法数字），暗色经 `html.dark` token 覆写。
3. 品牌统一（`c2d2c60`/`d841848`）：`VITE_APP_LOGO_TITLE` 改「Stack Depth」；侧栏 Logo 由框架 `logo.png` 换为与 blog-web `AppBrand.vue` 同源的内联 SVG 品牌标（错位叠框 + 品牌黄前框，后框描边随侧栏深浅主题切换）；favicon 换 Stack Depth SVG（内置 `prefers-color-scheme` 深浅自适应，ico 保留兜底）。
4. 框架痕迹清理（`17d270a`）：删除顶栏 Github/文档外链图标块及 `RuoYiGit`/`RuoYiDoc` 组件；登录/注册页页脚版权改为品牌方；注册页品牌文案对齐登录页；头像旁角色标签「Workspace」→「博客后台」。
5. 遗留人工事项：① ~~侧栏菜单「PLUS官网/测试菜单/AI会话/系统监控」隐藏~~（2026-10-16 用户已在后台菜单管理操作完成，可逆、未落 SQL）；② ~~人工视觉验收~~（2026-10-16 用户 Windows 浏览器确认通过）；③ `navType=TOP` 布局下 Navbar 仍引用框架 `logo.png`（非默认布局，未动，已知小遗留）。

验收结果（2026-10-15 agent 侧全绿）：`vue-tsc` 0 错 + `pnpm build` 通过；playwright 冒烟——浏览器真实登录成功（验证码答案经 Redis 读取复刻）、首页 6 卡拿到真实数据（文章 20055/已发布 20041/草稿 14/隐藏中 1/评论 3/标签 49）、登录页/首页/作者页/三治理页无控制台错误无 5xx、桌面+移动截图正常、清理项（页脚/favicon/顶栏图标/角色标签）逐项断言通过。注：Impeccable 技能本机不可用，视觉按 D39 token 纪律直接落地，未使用技能默认产物。

### S20 Lenis 平滑滚动 ✅（blog-web；0.5d，2026-10-15 验收关闭）

范围（D38 收缩后）：仅 Lenis；hero 视差 / 逐字进场待定；**不做 three.js 背景、不引 GSAP**（内容站违和，SSR 与首屏成本不成比例；GSAP 与 Lenis 收益重叠）。纯表现层：无契约/数据/状态机改动，独立 commit，随时插拔。

验收：与页面过渡、PWA 叠加无冲突；`prefers-reduced-motion` 兜底生效（transitions.css 已有，确认 Lenis 同样尊重）；移动端滚动手感人工确认。

验收结果（2026-10-15）：agent 侧无头 Chrome 冒烟全 PASS——`html.lenis` class、wheel 平滑渐进曲线（lerp ramp 非瞬跳）、路由跳转滚到顶且实例存活、`reducedMotion=reduce` 下无 lenis class 且原生滚动可用、SW 注册态刷新无冲突、三视口零 console error/5xx；人工侧（用户 Windows 浏览器）桌面手感 / 移动端跟手 / 系统减少动效切换 / PWA 安装态叠加全部确认无问题。实现：`app/plugins/lenis.client.ts`（matchMedia 门控 + 偏好切换监听 + `stopInertiaOnNavigate` + lerp 0.1 温和默认，移动端 syncTouch 关闭保持原生跟手）+ transitions.css 官方配套规则（`lenis-smooth` 期间中和 `scroll-behavior: smooth` 防双动画）；坑位：Nuxt 4.6 unimport 会剥离 plugins 下 bare 包静态默认导入致运行时报 `Lenis is not defined` 白屏，改动态 `import('lenis')`（已记入 `blog-web/docs/plan.md` §2.17 与 README）。blog-web `bde6043`，零后端改动，细项见 `blog-web/docs/plan.md` §2.17。

### S21 CI/CD 补全 ⏸（可选；1d）

范围：后端 `mvn compile + test`、blog-web `typecheck + build` 的 GitHub Actions 工作流（plus-ui 已有最小 CI 可扩展）；public 仓库 Actions 免费额度无限。

验收：push / PR 触发全绿；失败可阻断合并（branch protection 可选）。

### S22 注册即登录 ✅（2026-10-16 实施并 E2E 实测关闭；0.5d）

背景（D35）：`/auth/register` 原返回 `R<Void>` 不下发 token，且 `SysRegisterService` 校验时即消费图形验证码（一次性），前端无法"注册成功后就地再调一次登录"——blog-web 注册页因此一直是「注册成功态 + 去登录按钮」，用户须再手动登录一次（与 plus-ui 注册页旧口径一致）。用户 2026-10-16 要求去掉该中间态，跳转目标定为「回来源页」。

范围与实现（跨仓，D32 分两个 commit）：

1. 后端（ruoyi-admin，1 commit）：
   - `AuthController#register` 返回类型 `R<Void>` → `R<LoginVo>`：注册成功后按 `login()` 同口径校验 clientId/grantType/status，调 `SysLoginService#registerAutoLogin` 建会话并返回 token；
   - `SysLoginService#registerAutoLogin`（新增）：按用户名回查新账号 → `buildLoginUser` + `LoginHelper.login` + `IAuthStrategy.buildLoginParameter(client)`，与 S14 `socialAutoRegister` 同一模式；图形验证码已在注册时校验并消费一次，此处不重复校验、不构成绕过；
   - 兼容性：plus-ui 注册页忽略返回 data，行为不变（仍注册后回登录页）；`/auth/register` 无限流为既有问题（S18 只覆盖 login），本切片不扩大范围。
2. blog-web（1 commit，细项见该仓 `docs/plan.md` §2.20）：
   - `server/api/auth/register.post.ts`：响应出现 `access_token` 时按 login.post.ts 同口径种 httpOnly cookie；兼容旧后端 `R<Void>`（无 token 时退回只建档）；
   - `app/pages/register.vue`：删除「去登录」成功态，改为 toast「欢迎加入，已为你完成登录」+ `navigateTo(returnPath)`；来源页经 `?return=` 传入（LoginModal 注册链接带上 `route.fullPath`），页面仅接受站内相对路径（拒 `//` 开头）防开放重定向；
   - `useAuth.register`：成功后 `refresh()` 刷新会话（与 login 一致）。

验收（Agent E2E，2026-10-16）：另起 8082 新后端实例 + blog-web 生产实例指向 8082（用户原 8081 实例未动），六用例全过——① 新用户名 + 正确验证码注册 → `{registered:true, logged:true}` + httpOnly token cookie（JWT 含新 userId）；② 注册后 `/api/auth/session` → `logged:true` + profile；③ 带 cookie 调 `/api/blog/user/profile` → 200 返回资料（token 对博客 API 生效）；④ 无 cookie 同接口 → 401；⑤ 错误验证码 → 后端原文「验证码错误」且不种 token；⑥ 重复用户名 →「保存用户…注册账号已存在」且不种 token。测试用户（`e2e_reg_20261016`）与临时实例均已清理。浏览器视觉验收（toast 呈现、跳回来源页落地、移动视口）2026-10-16 用户确认通过。

### S23 blog-web 全站视觉改版（方向 C · 冷米白 + 衬线）⏳（blog-web；6~8d，2026-10-26 立项）

背景（D41）：首页 1.1 在 Google Stitch 中冻结后，视觉语言与 S9/S12 已交付页面产生冲突——暖纸 `#f4f1ea` + 黄 CTA + 系统字体 + 圆角 2/4 分族，要换成冷米白 `#faf9f6` + 靛蓝 `#1a4d8f` + **Noto Serif SC 衬线标题** + 统一 3px 圆角。必须一次性全站统一，否则新旧两套皮肤并存。

范围（纯表现层）：

1. **零契约/数据/状态机改动**：不动 `server/**`、`app/types/**`，三层 API 边界（`/blog/public|article|user/**` vs `/blog/admin/**` 禁入）零变化。
2. **明示推翻三条已锁规格**（`blog-web/design/mechanisms.md` §0）：① 色板（暖纸→冷米白）；② 字体（首度引入 webfont，推翻"永不加载 webfont"）；③ 圆角（2/4 分族→统一 3px）。动效基线与反馈分层两条继续有效。
3. **plus-ui 不同步**（用户拍板）：Element Plus 体系不套 blog-web token，只借版式思路做登录页/首页小改，独立仓独立 commit（可与 S19 遗留合并处理）。
4. 作者中心仍走 `/blog/article|user/**` 作者自助层，不因改版引入任何治理入口。

阶段与门禁：

| 阶段 | 内容 | 门禁 |
|---|---|---|
| P1 补屏 | Stitch 出：首页列表缩略图混排、深色版首页、文章详情页 | 深浅两版都成立；详情页含 68ch 正文 + 编号小节 + 代码块 |
| P2 立规范 | `blog-web/design/stitch/README.md`（屏台账/落地映射/不吸收清单）+ `.impeccable.md` 方向叙述 + token old→new 对照表 | 规范文件就位，代码零改动 |
| P3 换地基 | `tokens.css` 重建（色/字/圆角/阴影）+ Noto Serif SC 分片引入 + `uno.config.ts` shortcut 重建 + `Sd*` 基础件 | **唯一一次全站同时变样**；`contrast.mjs` 16 项重算并更新期望值；`shot.mjs` 退 0；深浅双主题 |
| P4 逐页 | 壳层 → 首页 → 详情 → 标签 → 搜索 → 用户 → 作者中心 → 浮层/状态页 | 每页 `build` + `shot.mjs` 退 0 + 四视口（桌面 1440/移动 390 × 深浅） |
| P5 plus-ui | 登录页/首页小改（独立仓） | 登录链路 E2E 复测，`vue-tsc` 0 错 |

已知风险与代价（提前登记，避免上线才发现）：

1. **LCP 会掉**：衬线标题是首屏 LCP 元素。对策 = 自托管 + `cn-font-split` 分片 + `unicode-range` + `font-display: swap` + 首屏分片 `preload`，只加载 700 一个字重；验收线 LCP ≤ 2.5s（模拟 Slow 4G）、CLS = 0、首屏字体下载 ≤ 100KB。S17 的 Lighthouse 全 100 基线须在 P3 后重测并记录差异。
2. **改公共组件 = 改多页**：`ArticleCard`（搜索/标签/用户页复用）、`CtaBand`（6 页复用）、`TagChip`、`SdButton`/`SdCard` 任一改动都要 grep 全部复用点。
3. **`contrast.mjs` 期望值绑定旧色板**：换色后必须同步更新期望值，否则门禁恒红（改期望值需人工核对新色对，不得为过门禁调低标准）。
4. 可能变孤儿：`ArchiveGroup`（C 去按月分组）、`HeroVisual`/`LayerStack`（叠框母题被系统状态卡取代）、`ArticleListSkeleton`；废弃前先确认无其他复用点。

任务粒度（T1~T22，含每项门禁）见 `blog-web/docs/plan.md` §2.21；Stitch 屏台账与"不吸收清单"见 `blog-web/design/stitch/README.md`。

### 一次性 SQL 脚本执行状态（docs/）

> 下列迁移 / 清理 / 菜单脚本均为一次性执行，截至 2026-10-05 已全部在 `ry-vue` 库执行完毕，在此登记以免被误当作待办或重复执行。建表基线 `blog_schema_v0.1.sql` 属环境初始化（见 SETUP §3），不列入本表。

| 脚本 | 用途 | 状态 |
|---|---|---|
| `blog_s0_cleanup.sql` | 停用 Controller 菜单清理 + 开放注册开关（转 S5 执行） | ✅ 已执行（2026-10-03） |
| `blog_s2_migration.sql` | `sd_blog_article_tag` 转纯关系表迁移 | ✅ 已执行（S2 时，0 行数据安全） |
| `blog_s3_cleanup.sql` | 清除 S2 冒烟测试文章（DELETE 段） | ✅ 已执行（2026-10-03 人工确认后） |
| `blog_s5_menu.sql` | S5 管理员文章治理按钮 + 残留菜单清理 | ✅ 已执行（2026-10-03） |
| `blog_s6_menu.sql` | S6 标签治理按钮 + Tag 裸 CRUD 菜单清理 | ✅ 已执行（2026-10-03） |
| `blog_s7_menu.sql` | S7 评论治理按钮 + Comment/UserProfile 菜单清理 | ✅ 已执行（2026-10-03） |
| `blog_s10_menu.sql` | S10 治理页 C 型菜单入口（文章/标签/评论，parent=博客目录） | ✅ 已执行（2026-10-05） |
| `blog_s16_demo_data.sql` | S16 演示数据（24 篇中文文 + 2 万压测，标记 ID 段 9e18 量级/s16- 前缀） | ✅ 已执行（2026-10-14 灌入实测，验证段 20024/20024/30 符合预期） |
| `blog_s16_cleanup.sql` | S16 演示数据清理（含 ES 文档清理两法：对账自动 / curl _delete_by_query） | ✅ 已执行（2026-10-16 用户确认清理完成；查库 s16- 前缀 0 条） |
| `blog_s9_web_client.sql` | S9 开工准备：sys_client 新增 `web` 客户端记录（D28，幂等含验证段） | ✅ 已执行（2026-10-06，验证段 1 行：client_id `f38076e6bb30a1170b9ea3fbebac0815`、grant_type 仅 password、status/del_flag 均 0） |

> S14 的 `web` 客户端 grant_types 追加 `social` 经 plus-ui 后台「客户端管理」执行，**不落 SQL 脚本**；若后续改由脚本执行，在此登记。

## 4. 关键路径与并行

```text
S0 → S1 → S2 → S3 → S4 → S5 → S6 → S7 → S8   （后端关键路径，粗估 10~12 人日）
                              ├── S9 前台站（blog-web，S5 后可并行启动）
                              ├── S10 管理后台前端（plus-ui，依赖 S2–S8 接口，S8 后即可启动）
                              ├── S11 移动端套件（uni-app，MVP 后启动，复用 S9 接口面，零后端改动）
                              └── S12 blog-web 作者中心（blog-web，D29；复用 /blog/article/**，零后端改动）
```

- S2 是最大风险点（事务编排 + 权限 + 版本），如超 3 天先拆解再继续。
- 切片间不允许功能倒灌：S5 之前的 Article 保存不得夹带 hide/admin-hide 逻辑。
- **亮点阶段（v0.4 起，S13~S21）**：D0 采购/备案先行（等待期并行 S14/S15）→ S16 ES 主菜 → S17/S18/S19 → S20/S21 待定/可选。节奏 15h/周 ≈ 2 人日/周，核心（S13~S19）约 10.5~12.5 人日，预计 6~7 周（2026-11 底~12 月初完成核心）。S16 是亮点阶段最大风险点（IK 安装 / 同步一致性 / D36 契约变更），第一步先验证 IK 可行性。

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
| 2026-10-03 | D21（端与仓库拓扑）：确认 **PC 博客前台（普通用户看到的站点）= 第三个独立代码仓库**，GitHub `blogs-web`（2026-10-06 建仓前定名，原登记名 ryblogs-web 作废；本地目录名 blog-web 不变），与现有两仓库同级 clone；它只调 `/blog/public/**` 与 `/blog/article·user/**` 两层，不得访问 `/blog/admin/**`；公知事实：公共接口不区分客户端，因此新增前端不需后端改接口。**移动端暂不纳入 MVP**（形态未定，将来讨论从 SETUP §10.4 该行接回，不预先设计）。多根工作区唯一权威副本由**后端仓库托管**（`tooling/blog.code-workspace`），无 meta repo 的决定不变 | docs/SETUP.md §10 |
| 2026-10-03 | D22（文档治理，用户点头）：将「不要在父目录执行 `mvn`」从 `docs/SETUP.md` §10.3 升格为 `AGENTS.md` §8 构建红线；SETUP §10.3 保留原约束并交叉引用 AGENTS.md §8 | 本文件 |
| 2026-10-03 | D23（前端工作显性化）：新增 S10 管理后台前端切片、§2 加「涉及端」列；plus-ui 补 `AGENTS.md` 编码规范/技能小节 + 新建 `plus-ui/docs/frontend-plan.md`（派生实现清单，状态单一来源仍在本 ROADMAP）；现存 blog 前端为失效生成骨架（调用已删除的 `/blog/blogXxx`），待按契约重建 | 本文件 / plus-ui |
| 2026-10-03 | D24（作者端归属，用户确认）：作者「发文/编辑」体验落 **plus-ui**，复用框架后台能力（重申 AGENTS.md §5：所有注册用户天然可发文、共用 PLUS-UI 后台）；blog-web（S9）为纯前台，不承担作者发文/编辑（其登录用户层 API 仅用于评论等读者行为）。S10 范围据此确定。**（2026-10-07 经 D29 修订：blog-web 增设作者中心，登录用户可在前台自助发文/编辑/隐藏/删除自己的文章，走 `/blog/article/**`；平台治理仍仅 plus-ui。plus-ui 作者页保留。见 §6 D29 / S12。）** | 本文件 / plus-ui / SETUP §10.4 / blog-web |
| 2026-10-05 | D26（标签运营属性守卫补全，S10 验收衍生）：① §13.3 `updateOps` 增状态校验，对 MERGED 标签设精选/排序拒绝（对齐 renameTag/adminDelete 的 MERGED 拒绝模式）；② §13.5 合并事务内精选身份转移——source 精选且 target 非精选则 target 继承精选与排序，target 已精选则保持不变，无论 source 是否精选合并后一律 `source.is_featured=0/sort_order=0`，杜绝 MERGED 记录残留精选（数据不变量：不存在 status=1 且 is_featured=1）。契约/后端/前端同步，不新增表/字段/接口；热门标签实时统计不受影响 | 本文件 / blog-api.md §13.3/§13.5 |
| 2026-10-06 | D27（端与切片扩展，S10 收尾后用户确认，接回 D21「移动端暂不纳入，将来讨论接回」预留口子）：① **S9 范围明确为响应式**（PC + 移动浏览器一套代码），移动适配在 S9 内交付，不再另起移动 Web 切片；② **uni-app 立项为 S11 移动端套件**，MVP 后启动，目标平台微信小程序 / H5 / App，范围基线=读者套件（列表/详情/搜索/评论），不做发文与治理（D24 口径延伸）；复用 S9 同一接口面零后端改动，认证走统一 `/auth/login` + `sys_client` 既有 `app` 记录（纯配置）；③ 仓库名、页面级切分、启动排期待 S11 开工前另决。注：编号顺延 D26——D25 已被待决策占用，已确认表无 D25，属既定事实非缺录 | 本文件 / SETUP §10.4 |
| 2026-10-06 | D28（S9 技术栈与开工决策，用户逐项拍板）：① **渲染模式弃 SPA 方案 A、选 SSR，且落地版本从讨论中的 Nuxt 3 升为 Nuxt 4**——starter 仓库 v4 模板（Nuxt 3 已 2026-07-31 EOL，v3/v4-compat 排除；content 模板不适用——内容源是后端 API）；Vite/Vue3/TS/文件路由/自动导入/SSR 全内置，回应"不自建基础"诉求；② 集成清单：`@unocss/nuxt`（与 plus-ui 同语法，uno.config 裁剪复用）+ preset-typography、`@nuxtjs/seo`、`@nuxt/image`、shiki 服务端着色、`@nuxt/eslint`；**无组件库**（博客前台无管理端刚需，薄组件+prose 排版）；③ alias 301 由 Nitro server middleware 原生实现，契约 §7.3 边缘层方案作废；④ 认证：token 走 **cookie + Nitro 代理**（httpOnly，SSR 可读，规避 CORS）；**blog-web 新建 `web` 客户端记录**（与 plus-ui `pc` 会话隔离，grantType 仅 password），SQL 落 docs/blog_s9_web_client.sql；备案：跨端无缝登录不存在（localStorage 按 origin 隔离 + 会话隔离，与 clientid 选择无关，非 SSO 改造不做），plus-ui 仍用 pc；⑤ 响应式策略：**只拍一张桌面骨架**，移动/平板形态由既定断点规则推导（断点=UnoCSS 默认 sm640/md768/lg1024/xl1280），实现期真机微调；⑥ 页面清单 7 项+登录/评论弹层（详 S9 节）；⑦ 治理资产：建仓时 AGENTS.md（§10.4 硬要求）+ 一个 nuxt-frontend-coding skill（symlink 规范），子代理资产按需后加；⑧ SSR-safe 纪律由"迁移预备"升为当下硬性要求（SSR 执行环境） | 本文件 / SETUP §10.4 / docs/blog_s9_web_client.sql |
| 2026-10-07 | D29（作者端归属修订，用户决定反转 D24）：① blog-web 增设**作者中心**——登录用户可在前台发布 / 编辑 / 发布后隐藏 / 重新公开 / 删除**自己的**文章，全部走 `/blog/article/**` 作者自助层（仅登录、服务端按 `author_id` 隔离，**零后端改动**，接口 S2–S5 已就绪）；② 编辑器复用 plus-ui 同款 **WangEditor `@wangeditor-next/*` 6.4.1**，`<ClientOnly>` 包裹 + 按需加载，**不上 Markdown**（与后端 HTML 净化契约、plus-ui 输出一致，无格式分叉）；③ 作者中心复用 blog-web 已登录的 `web` 会话，**无需二次登录**（解决"发文要跳 plus-ui 重登"的割裂）；④ **平台级治理**（他人文章下线/恢复、标签 rename/merge、评论审核）仍**只在 plus-ui `/blog/admin/**`**，blog-web 禁止调用——作者自助 ≠ 平台治理；⑤ 据此新增切片 **S12**，D24 中"blog-web 不承担发文/编辑"的表述作废（plus-ui 作者页保留，两端共用作者自助接口）。本轮仅同步文档，代码实现另起开发对话 | 本文件 / blog-api.md §15 / blog-web/AGENTS.md / blog-web/docs/plan.md / docs/architecture.md / AGENTS.md §5 |
| 2026-10-07 | D30（S9 技术栈范围收编，不改变 D28 已实装部分）：D28 集成清单里的 `@nuxtjs/seo`、`@nuxt/image`、shiki、`@nuxt/eslint` 四项与 three.js 装饰（桶 B）由“随切片交付”调为 **⏸ 按需 / MVP 后再定**，不计入 S9 完成标准。理由：① S9 实需已覆盖——SEO 基线由 unhead 原生 `useSeoMeta` 交付（lang/title/description/og/robots，已入首屏），无需整块模块；② 不为非 MVP 能力提前绑依赖（与“不为面试亮点提前写死”一致）；③ 桶 B 与数据链/版式已解耦（hero 预留 `<ClientOnly>` 槽位，注入不改取数代码，见 D28/plan §2.7）。**S9 实际实装 = `@unocss/nuxt` + `@nuxtjs/color-mode`**；待办清单见 `blog-web/docs/plan.md` §2.6/§2.7，本文件 S9 技术栈行与实施结果已加注 | 本文件 / blog-web/docs/plan.md / blog-web/uno.config.ts |
| 2026-10-08 | D31（图片 URL 存储策略，S12 开发中遇 S10 遗留“正文图片协议”问题的正式收口）：① blog-web 作者中心的**封面与正文图片均存后端返回的真实访问 URL**，不写 `oss://{ossId}` 标记（与 plus-ui 现有做法有意分叉）；② 生产 OSS 约定为**公有读(access_policy=2) + CDN 域名**，并把开发环境当前的 `1 公有读写` 收紧为 `2 公有读`（写入仍只走后端 SDK 凭据）；③ 依据（均为代码实测而非推测）：`SysOssServiceImpl.matchingUrl()` 仅当桶为 PRIVATE(0) 时才换成 `presignGetUrl(...,120s)` 硬编码签名，当前 `sys_oss_config.access_policy` 全为 1 故不过期；`SdBlogArticleServiceImpl.validateCoverUrl()` 只允许 `/`、`http(s)://` 即**封面本就不允许存 oss://**；而解析标记的 `/resource/oss/listByIds` 需登录，**匿名读者无法解析**（即 S10 备案的遗留项根因）；④ **私有桶预案**（仅当真要私有存储时开后端切片）：新增 `OssContentResolver`（正则 `oss://(\d+)` → 已存在的 `OssService.selectByIds` 回填，约 60~80 行，无表结构/契约变更，`sd-blog` 已依赖 `ruoyi-api`）**且必须同时把 120s 有效期配置化调长或改走图片代理**，否则只是把破图率从 100% 降到一两成；⑤ 本决策不改后端代码（零后端改动边界保持），仅约定生产 OSS 配置方向；S10 备案的“正文图片协议缺陷”仍限于 **plus-ui 旧数据里的 oss:// 标记**，不因此产生新数据 | 本文件 / blog-web/docs/plan.md §2.8 / SysOssServiceImpl / SdBlogArticleServiceImpl |
| 2026-10-11 | D32（多 harness 下的 Agent 编排层与跨仓路由枢纽，起因：从 Qoder / DeepSeek harness 切到 WorkBuddy，Agent 会话根落在父目录 `sd-blog/`）：① **工作区文件位置不动**——`tooling/blog.code-workspace` 仍为本仓库托管的唯一权威副本，**不迁到父目录、也不为它单独建仓**。理由：该文件只有版本化才有意义，父目录非 Git 仓库会使其变成换机即失、可被随意改坏的孤儿文件；为一份 555 字节的配置单独建仓，收益远低于其维护成本；且它是所有前端的公共前提而后端不依赖任何前端，托管在本仓库最合理（§10.3 口径）。② **父目录 `sd-blog/` 保持非 Git 仓库**，重申 §10.3：各仓独立提交/分支/上游，无需原子提交，故不做 meta repo、不用 git submodule。③ **新增父目录 `sd-blog/AGENTS.md` 作为跨仓路由枢纽（hub）**，只做三件事——仓库地图（目录↔远端↔角色↔领域 AGENTS.md 入口）、权威源指针（契约/路线图/环境一律指本仓 `docs/`，编码细则指各仓 `.codex/skills` 与 `.claude/agents`）、跨仓环境红线（三层 API 边界、不在父目录执行任何构建、一个 Agent 会话只动一个仓库）；**禁止在 hub 里复制任何子仓规则正文**（沿用 D1「避免双源漂移」），与子仓 `AGENTS.md` 冲突时**一律以子仓为准**并回头修订 hub。④ **新 harness 接技能一律用转发 shim 指向本体**，禁止产出第二份 SKILL.md 实体：父目录 `.workbuddy/skills/` 下三个 shim（`ruoyi-plus-ai-coding` / `frontend-crud-coding` / `nuxt-frontend-coding`）各只含 frontmatter + 必读清单 + 跨工具补充纪律，正文仍唯一存在于各仓 `.codex/skills/`；`.qoder/skills` 沿用既有 symlink 口径**保留不删**（用户决定：它只是历史 harness 的加载入口而非事实源，其本体缺失属资产管理问题，不在此决策范围）。⑤ hub 与 shim 属**本机工具接线层**：因父目录不入库而天然不版本化，这是有意取舍，换取对三个仓库零污染；代价是换机需重建，故其每一条约定必须在本文与 `docs/SETUP.md` §10.5 有对应登记，**登记缺失即视为该约定失效**。⑥ 边界澄清：单独打开任一子仓（如只开 `plus-ui`）时该仓 `AGENTS.md` 已自洽，hub 只服务跨仓场景——两者互补而非替代，**不要把 hub 写成第四个仓库** | 本文件 / SETUP §10.5 / AGENTS.md §3 §11 / 父目录 sd-blog/AGENTS.md |
| 2026-10-13 | D33（亮点阶段规划，MVP 后面试向，用户确认）：新增切片 S13~S21——S13 线上部署（**D0 采购/备案时钟先行**，等待期并行开发）、S14 社交登录、S15 阅读量异步、S16 ES 搜索、S17 PWA+SEO、S18 限流、S19 plus-ui 登录页/后台改版、S20 Lenis（待定）、S21 CI（可选）。节奏 15h/周 ≈ 2 人日/周，核心 S13~S19 约 10.5~12.5 人日，预计 6~7 周。**明确不做**：多级缓存失效链路（以"为什么先不上"作为面试答案）、消息队列（判断力叙事）、多级评论、点赞收藏、可观测性全套、three.js/GSAP（D38）。新建 `docs/cloud-deployment.md` 作为部署方案备忘（个人理解 + 项目记录双用途） | 本文件 / docs/cloud-deployment.md |
| 2026-10-13 | D34（社交登录语义扩展与落地，依据框架源码实测）：① 框架默认仅支持**已绑定账号**登录（`SocialAuthStrategy` 未绑定抛"你还没有绑定第三方账号"，绑定入口 `/auth/social/callback` 又要求先登录），读者场景需扩展为**首次社交登录自动注册**（自动建号+绑定+登录；随机不可用密码/禁账密/默认角色/昵称冲突处理；**安全边界按红线人工复核**）；② 社交登录**只在 blog-web 落地**（读者主场景；GitHub OAuth App callback 单 URL 限制，plus-ui 保持账密）；③ `web` 客户端 `grant_types` 加 `social`（`AuthController` 登录校验客户端 grantType，漏加直接拒；经后台客户端管理执行，不落 SQL 脚本）；④ **微信/QQ/钉钉因企业资质门槛不接入**（微信开放平台仅限企业/机构资质，PC 网站应用需审核+300元/年认证），作为已评估边界写进 README | 本文件 / AuthController / SocialAuthStrategy |
| 2026-10-13 | D35（阅读量异步语义，修订 D8"MVP 不上 Redis/异步"）：① 计数 = PV 口径 + **同人窗口去重**（登录 userId / 匿名 visitorId cookie，窗口 30min 可配，Lua 原子执行 SETNX+HINCRBY）；② 落库 = 10s 定时批量**增量** UPDATE（HGETALL→`view_count = view_count + delta`→HDEL），增量合并不覆盖，**不回退**；③ 读 = DB 值 + 待增量（契约响应结构不变，语义变更登记 Apifox）；④ Redis 不可用降级直连 DB；⑤ redis 开 AOF everysec，崩溃最多丢 1s；异常模式（重复/丢失计数）为计数场景可接受取舍 | 本文件 / S15 |
| 2026-10-13 | D36（ES 高亮实现位置，用户拍板）：**服务端高亮**——ES `highlight` 查询返回 title/summary/content 匹配片段（`<em>` 包裹，ES 默认编码器转义片段内其余 HTML，前端可信任），`/blog/public/search` 响应**新增可选 `highlight` 字段**（向后兼容）；按项目流程**实现前先修订 `blog-api.md` §6**（本决策即授权）；否决客户端高亮（匹配质量弱、前端解析成本高）。S16 因此含一次契约变更（仅新增可选字段） | 本文件 / blog-api.md §6 / S16 |
| 2026-10-13 | D38（动效收缩，承接 D30 桶 B 与批 1/批 2 已交付部分）：零依赖机制（reveal/路由过渡/骨架淡入/阅读进度条/顶栏感知/懒加载+CLS）已交付，动效工程故事已成立；批 3 收缩为**仅 Lenis**（约 30KB），hero 视差 / 逐字进场待定；**不做 three.js 背景、不引 GSAP**（内容站违和、SSR 与首屏成本不成比例、与 Lenis 收益重叠）。S20 纯表现层：无契约/数据/状态机改动，排核心之后，随时可砍 | 本文件 / blog-web plan §2.17 |
| 2026-10-13 | D39（plus-ui 门面改版）：登录页与后台首页由框架默认改为博客项目视觉（Impeccable skill + **按项目 token 体系重构**，不照抄默认）；后台首页优先做"博客数据概览"（真实接口），否则静态欢迎页；**保留 SocialCallback 路由、登录逻辑、验证码链路、权限与路由结构不动**；与业务零关联，排核心之后 | 本文件 / plus-ui frontend-plan §4 |
| 2026-10-14 | S14-D2（社交登录邮箱与账号归并口径，联调期用户提问触发、零代码改动）：① 社交自动注册**不写 `sys_user.email`**（平台邮箱未验证，GitHub 默认 scope 还不返回非公开邮箱）；② **不按邮箱归并账号**——未验证邮箱隐式归并 = 账号接管漏洞（A 在平台资料填 B 邮箱即可登入 B 账号）；同一人授权多平台视为多个独立账号，代价仅是作者中心多一组文章，博客场景可接受；③ 身份模型维持框架既定语义（`authId=source+uuid` 平台维度唯一，框架绑定流程本就是「已登录用户主动绑更多平台到同一账号」）；④ 将来如需归并，做成「登录后 `/auth/social/callback` 主动绑定」显式功能（独立切片，需定契约 + plus-ui 入口），**不做登录时隐式合并**；⑤ 本条为备忘录性质，防止未来把「社交用户无邮箱/同人多账号」误当 bug 去"修"而引入接管风险 | 本文件 / SysLoginService / AuthController |
| 2026-10-14 | S15-D1（阅读量异步计数实现口径，实施期沉淀，含联调期修正）：① 计数与读值合并为**单脚本单 RTT**（`SETNX NX EX`+`HINCRBY` 后 `HGET` 收尾返回当前待增量），不套 MULTI、不第二次 RTT——初版分两次 RTT + RMap 读取，联调期发现 Redisson 全局 `CompositeCodec` 的 JSON 值编码会把 Lua 参数污染成垃圾值（`SET EX` 报 not an integer，全量走降级），合并脚本 + 显式 `getScript(StringCodec.INSTANCE)` 后修复，详见 §3 S15 联调期修复条；② flush 逐条独立提交不套整批事务——单条 DB 失败仅丢该 delta 并续跑，与 D35 取舍一致（套事务会一损俱损）；③ 库内 view_count 由详情路径已读文章行传入，服务层不回查（省一次 SELECT）；④ 无访客标识退化直写（裸调 API 场景，与 S8 一致）；⑤ 列表页不合成待增量（滞后 ≤10s 可接受，避免列表查询打 Redis）；⑥ 宿主 redis AOF 为仓库外手动步骤，生产 AOF 归 S13 部署清单；⑦ **纪律：今后任何 Redisson Lua 调用必须显式指定 StringCodec**（全局 codec 只保证 key 正常、value 必被 JSON 编码，陷阱极隐蔽） | 本文件 / ViewCountService / ViewCountSupport / RedisConfig |
| 2026-10-13 | S14-D1（社交登录自动注册实现口径，依据框架源码与库表实证）：① 锁与事务同方法——lock4j-core 2.2.7 的 `LockAnnotationAdvisor` 为 `Ordered.HIGHEST_PRECEDENCE`，锁包裹事务，锁内双查绑定关系保证并发首登只建一号（与 workflow 模块 `@Lock4j + @Transactional` 既有用法一致）；② "禁账密登录"= 随机 UUID 的 BCrypt 哈希密码（`sys_user` 无专用禁用列，明文不可获知即不可登录，密码永不下发）；③ 不分配任何角色/部门，与框架 `/auth/register` 的 `registerUser` 口径一致（博客红线：所有注册用户天然可发文、不授予管理权限）；④ 账号派生：平台用户名清洗（仅保留 `[a-z0-9_-]` 并小写）→ 冲突 `base_1~base_9` → `source_uuid` 确定性兜底（`user_name` 无唯一约束，必须应用层消歧）；昵称空值回退账号名、截断 30、冲突追加 4 位随机后缀（仅展示用途、无唯一约束）；⑤ 社交头像 URL **不落** `sys_user.avatar`（bigint OSS id，类型不匹配），仅存 `sys_social.avatar`——社交注册用户的博客公共资料头像为空，属已备案边界（S1-D1 头像机制不变，不新建头像链路）；⑥ justauth 只配 dev（`github` 新增 + `gitee` 假凭据清除，均 env 占位），**prod 配置留 S13**（生产域名未定，避免空 redirect-uri 破坏授权链接） | 本文件 / SysLoginService / SocialAccountNamer / AuthController |
| 2026-10-14 | S16-D1（ES 服务端版本选 7.17.28，修订 S16 原文"ES 8"）：项目依赖栈实际锁定 easy-es 3.0.2 → `co.elastic.clients:elasticsearch-java:7.17.28`（本地 ~/.m2 pom 实证），ES 8 服务端与 7.17 客户端存在兼容风险；7.17.28 与客户端精确匹配，IK 7.17.28 同版本配套（直连 GitHub release 404，经 ghproxy.com 镜像安装已实证可达）；`vm.max_map_count` 环境已 1048576 达标 | 本文件 / script/docker/es/Dockerfile |
| 2026-10-14 | S16-D2（ES 客户端走裸 elasticsearch-java，不用 easy-es BaseEsMapper）：IK mapping/boost/alias/零停机 reindex/highlight/search_after 需要精确 DSL 控制，easy-es 启动期自动建索引与 alias 方案冲突；`BlogEsConfig` 手工建 `ElasticsearchClient` bean（约 40 行），`easy-es.enable` 保持 false 与其并存互不激活（依赖经 ruoyi-common-elasticsearch 传递已锁定，零新增依赖） | 本文件 / BlogEsConfig |
| 2026-10-14 | S16-D3（深翻页 = from+size 窗口 + after 游标双轨，兑现 ROADMAP 原文 search_after）：无状态 pageNum 契约无法携带上一页 sort 值，纯 search_after 需状态；定案 = `max_result_window=50000`（覆盖 5 万演示数据）+ 契约新增可选 `after` 请求参数 / `nextAfter` 响应字段（opaque 游标编码 score|publishedAt(epochMilli)|articleId，URL-safe Base64 去 padding）；越窗且无游标返回空页；pageSize 兜底默认 10、上限 100 | 本文件 / blog-api.md §6 / EsArticleSearchService |
| 2026-10-14 | S16-D4（索引同步最终一致 + 降级哲学，与 S15 同脉）：① 事件只作触发信号不携带状态，监听方事务提交后按 DB 现状重推导（重复/乱序消费幂等），索引失败仅记 error 由对账定时任务兜底（不阻断主业务，接受最终一致的秒级窗口）；② 回表查询复用 `selectPublicPage` 三条件底座——堵住"ES 查询后、DB 回表前文章被隐藏"的竞态泄露窗口（红线核验点：三条件在 indexOrRemove / initialFullSync / reconcile 的 MySQL 侧三处同源）；③ ES 运行期异常降级 MySQL LIKE（响应无 highlight/nextAfter），保证搜索可用性；④ 已备案边界：标签 rename/merge 不即时同步索引（tags 字段靠对账任务兜底刷新） | 本文件 / ArticleIndexServiceImpl / EsArticleSearchService |
| 2026-10-14 | S16-D5（演示数据与对比留档登记）：`blog_s16_demo_data.sql`（24 篇中文文 + 2 万压测，recursive CTE；ID 全落 9e18 标记段、tag slug 一律 s16- 前缀防既有标签唯一键冲突；SQL 直插绕过应用事件，ES 侧由对账任务补齐——顺带完成"对账发现并修复不同步"验收）；`blog_s16_cleanup.sql`（按段 DELETE + ES 清理两法）；`blog_s16_benchmark.md`（对比方法/命令留档，结果表人工实测后填写，清理方式随切片登记于此与 §3 SQL 状态表） | 本文件 / docs/blog_s16_*.sql / docs/blog_s16_benchmark.md |
| 2026-10-15 | S18-D1（限流实现形态与键阈值备案，开工前拍板）：① **放弃框架 `@RateLimiter` 注解、选「配置化阈值 + 薄封装直调 `RedisUtils.rateLimiter`」**——注解 `count/time` 为编译期字面量读不到配置，违背 S18 要求 2「不硬编码」；仍复用框架 Redisson `RRateLimiter` 与 i18n `rate.limiter.message`，超限行为与注解路径一致（`ServiceException` → `R.fail`，HTTP 200 + envelope code 500 + msg「访问过于频繁，请稍候再试」，**非 429**），Redis 异常同口径包装 `RuntimeException("服务器限流异常，请稍候再试")`；② 三点位键与阈值：`/auth/login` = `global:rate_limit:auth/login:{ip}:{username}` 5 次/60s（username 取加密内层 `PasswordLoginBody`，置于 client 校验前；非密码 grant 无用户名字段时退化为同 IP 共享键）；发表评论 = `global:rate_limit:blog/article/comments:{userId}` 10 次/60s（全站每用户配额、不区分文章；与 `@RepeatSubmit` 互补）；`/blog/public/search` = `global:rate_limit:blog/public/search:{ip}` 30 次/60s；策略存活 86400s（与框架 timeout 默认一致）；③ 键前缀复用框架 `global:rate_limit:`（`GlobalConstants.RATE_LIMIT_KEY`）便于运维统一观测；实现为 `org.dromara.blog.ratelimit`（`RateLimitKeys` 纯静态键组装 + `RateLimitChecker` `@Value` 绑定，内联默认值与 yml 同源）；单测 9 条；④ 阈值取「正常用户无感」口径：登录 5 次/分（忘记密码重试 2~3 次有余量，与账号密码错误锁定互补）、评论 10 次/分、搜索 30 次/分（防输入联想连打又留 NAT 共享 IP 余量），如实测偏紧改 yml 即可、无需改代码 | 本文件 §3 S18 / RateLimitChecker / RateLimitKeys / application.yml |
| 2026-10-16 | D39（前台质感精修轮验收关闭）：用户发起的三轮质感精修（非排期切片，先例见 plan.md §2.12）共 13 个 polish commit 全部实施完毕，2026-10-16 用户 Windows 浏览器 + 真机人工验收通过，样式改动暂告一段落；力度口径固化——「精修」= 可动间距节奏/字号字距/组件视觉/局部布局微调，**不动信息架构、页面骨架、交互逻辑与数据流**；设计上下文单一来源固化为 `blog-web/.impeccable.md`（后续设计类会话自动生效） | blog-web/docs/plan.md §2.19 |
| 2026-10-16 | D40（社交账号绑定/解绑管理不在 blog-web 提供，保持 S14 登录-only 范围，用户拍板）：入口仅 plus-ui「个人资料 → 第三方账号」（框架原带），普通用户不可达，且社交自动建号账号（随机 UUID 密码）无法登录 plus-ui——解绑对 blog-web 社交用户实际不可用；评估三个场景（平台账号丢失致博客账号锁死 / 无法追加第二平台 / 无法解绑）后 MVP 维持现状（PRD 未要求绑定管理，S14-D2 已列为"将来显式功能"），登记为已知边界；若后续补齐，最小方案 = 作者中心「账号与登录」小节复用 `GET /system/social/list` + `DELETE /auth/unlock/{socialId}`（零后端改动，约 0.5d），必须带「若为唯一登录方式，解绑后将无法登录」警示文案 | 本文件 §3 S14 / S14-D2 / blog-web/docs/plan.md §2.15 |
| 2026-10-16 | D35（注册即登录，S22）：① `/auth/register` 由「只建档」改为注册成功即下发 token（返回类型 `R<Void>` → `R<LoginVo>`），参照 S14 `socialAutoRegister` 建号即登录模式；图形验证码在注册时已校验并消费一次，自动登录不重复校验、不构成绕过；clientId/grantType/status 校验与 `login()` 同口径（前端注册代理早已注入 clientId/grantType）；② blog-web 注册成功后跳转目标为「回来源页」——LoginModal 注册链接带 `?return=route.fullPath`，注册页仅接受站内相对路径（拒 `//` 开头）防开放重定向，无参或非法值回首页；③ plus-ui 注册页忽略返回 data、行为不变（仍注册后回登录页），不构成破坏性变更；④ `/auth/register` 无请求限流为既有缺口（S18 只覆盖 login），本切片不扩大范围，留后续 | 本文件 §3 S22 / blog-web/docs/plan.md §2.20 |
| 2026-10-26 | D41（blog-web 全站视觉改版方向 C，用户拍板）：① **视觉语言全换**——冷米白底 `#faf9f6` + 深靛蓝强调 `#1a4d8f` + **Noto Serif SC 衬线标题**（正文仍无衬线）+ 圆角统一 3px + 近乎取消阴影；首页版式在 Stitch「首页1.1」冻结（头条 LATEST + 数据行列表 + 右栏双卡 + 编号锚点 01/02/03 + 系统状态卡）。**明示推翻已锁规格三条**：色板（暖纸→冷米白）、字体（首度引入 webfont）、圆角（2/4 分族→统一 3px）；动效基线与反馈分层两条继续有效。② 字体策略：自托管 + `cn-font-split` 分片 + `unicode-range` + `font-display: swap` + 首屏分片 `preload`，只加载 700 一个字重；验收 LCP ≤2.5s（模拟 Slow 4G）、CLS=0、首屏字体下载 ≤100KB；S17 的 Lighthouse 全 100 基线须在换地基后重测记录差异。③ 方案来源 = **Google Stitch 辅助**（含 MCP）：仅产出视觉方案，产物必须回译成 `tokens.css` 变量与 `uno.config.ts` shortcut，**禁止搬运内联样式/px 魔法值/编造数据**；每屏须过"数据契约"核对（导航仅首页/标签/搜索，禁止分类/专栏/话题/归档——Category 永久舍弃红线）。④ plus-ui **不同步**，只借版式思路做登录页/首页小改（独立仓独立 commit）。⑤ 立项新切片 S23（P1 补屏 → P2 立规范 → P3 换地基 → P4 逐页 → P5 plus-ui），纯表现层、零契约与数据改动；D39「精修」口径（不动信息架构）**不适用**本轮 | 本文件 §2/§3 S23 / blog-web/docs/plan.md §2.21 / blog-web/design/stitch/README.md |
| 2026-10-26 | D41 附注（编号缺陷登记，未擅改）：`D39` 在 2026-10-13（plus-ui 门面改版）与 2026-10-16（前台质感精修轮验收关闭）被**两次使用**，检索存在歧义；后续新决策自 D41 起编号，D39 歧义待单独修订（修订时须同步 §3 S19 与 blog-web plan §2.19 的引用） | 本文件 §6 |

### 待决策

| # | 决策 | 建议 | 截止 |
|---|------|------|------|
| D25 | 版本只读接口（§4.1/§4.2，`BlogArticleController.versions/version`）的管理员判定用 `LoginHelper.isSuperAdmin()`，比契约 §4.1「作者本人、管理员」更严——非超管但持 `blog:article:query` 的自定义治理角色能看 §11.2 详情（`getInfo` 硬编 `admin=true`）却看不了版本历史。S10 详情抽屉已兜底提示不白屏，超管验收不受影响 | 若后续把治理权授给非超管角色，将版本接口判定从 `isSuperAdmin()` 放宽为「作者本人 或 有 `blog:article:query`」，与 §11.2 详情口径对齐 | 出现非超管治理角色前 |
| D37 | S13 部署资源选型：白嫖（Oracle Always Free 4C/24G，需信用卡、有回收风险）vs 国内轻量付费（约 50~150/年、需备案、最稳）vs 香港节点（免备案、线路波动） | 演示期可白嫖 Oracle + 域名自持；**挂简历前落付费轻量**（面试前被回收的代价高于一年费用）；细节见 `docs/cloud-deployment.md` §2 | S13 采购时（D0） |

> **D7 说明（摘要，全文见 AGENTS.md §8）**：单测只覆盖"不碰数据库、不启 Spring 容器就能算对错的纯逻辑"——
> ① 标签规范化（`"Python 爬虫"` → `python-爬虫` 等契约 §18.10 规则）；
> ② 状态转换矩阵（四状态 × 动作 × 角色 → 允许/拒绝，契约 §14）；
> ③ 内容变化比对（四字段，决定要不要建新 Version）；
> ④ 合并展平与去重的纯计算部分。
> CRUD/HTTP 链路不写 Java 单测（Apifox 五类用例已覆盖），Controller 层不做 mock 测试（ROI 低）。
> 依赖已就绪：根 pom 统一引入 spring-boot-starter-test（JUnit5 + Mockito），common 模块已有单测先例。
