# AGENTS.md

> 本文件是本仓库的项目级 Agent 说明：项目是什么、代码放在哪里、全局必须遵守什么、Agent 应如何工作。
> 业务需求、字段语义、状态机、API 契约的权威在 `docs/`；编码细则的权威在 `.codex/skills/ruoyi-plus-ai-coding` 与 `.claude/agents/`。本文不重复展开，只保留红线与入口。

## 1. 项目概述

- 基于 **RuoYi-Vue-Plus 6.X** 的后端仓库，构建**多用户 / 多作者技术博客 / 内容平台**。
- 博客业务通过独立 Maven 模块 `sd-blog` 接入，Java 包名 `org.dromara.blog`。
- 前端有两个独立仓库：`plus-ui`（PLUS-UI 6.X-Vue，管理后台）与已确认待建仓的 `blogs-web`（本地 `blog-web`，PC 博客前台，S9）；均与本仓库**同级 clone**（布局与工作区见 `docs/SETUP.md` §10）。本仓库只负责后端（含 generator 前端模板产物），移动端暂不纳入（ROADMAP D21）。
- 策略：先完成可真实运行的博客 MVP；ES、多级缓存、PWA、SEO、CI/CD 等 MVP 后再决定，不为“面试亮点”提前写死非 MVP 技术。

## 2. 工具链

- Java 21（Temurin）+ Maven 3.9.x + Docker
- MySQL 8.4.x、Redis 8.x（本地 Docker 容器），开发环境为 WSL2 Ubuntu
- 不要把个人机器的绝对路径、代理地址、Docker 网络等环境细节写死到项目代码或通用规则中。

## 3. 博客模块与目录

| 项 | 值 |
|---|---|
| 模块目录 | `ruoyi-modules/sd-blog` |
| Maven artifactId | `sd-blog` |
| Java 包 | `org.dromara.blog` |
| Mapper XML | `sd-blog/src/main/resources/mapper/blog/` |
| 数据库表前缀 | `sd_blog_`（以 `docs/blog_schema_v0.1.sql` 与实际库为准） |
| IDE 工作区文件 | `tooling/blog.code-workspace`（多根工作区**唯一权威副本**，由本仓库托管；新增前端仓库时向其 `folders` 追加，不在父目录另存一份） |

Maven 接入点（三处必须同时一致，已配置完成）：根 `pom.xml` 的 `<dependencyManagement>`、`ruoyi-modules/pom.xml` 的 `<modules>`、`ruoyi-admin/pom.xml` 的 `<dependencies>`。

- 不需要额外 ComponentScan / MapperScan：启动类扫描 `org.dromara`，Mapper 扫描 `org.dromara.**.mapper`。
- ⚠️ 新增业务模块后必须在 `ruoyi-admin/src/main/resources/application.yml` 的 `springdoc.group-configs` 追加分组（如 `packages-to-scan: org.dromara.blog`），否则接口不会出现在 API 文档。

## 4. 信息源优先级与编码规范

实现功能时的取样顺序：

1. 当前仓库真实源码（同模块最近似实现优先）
2. `docs/` 设计文档
3. 当前锁定依赖版本的官方资料
4. 通用框架知识 / 旧版 RuoYi 教程（最低，禁止照搬）

编码细则（分层结构、Entity/BO/VO/Mapper/Service/Controller 写法、`QueryBuilder` / `BaseMapperPlus` 查询风格、数据权限注解、前后端联动约定、交付自检）以以下为准，动手前先读对应 reference：

- `.codex/skills/ruoyi-plus-ai-coding/SKILL.md` + `references/backend.md` / `frontend.md` / `examples.md`
- `.claude/agents/backend-*.md`（按任务类型选择：crud / module-enhancement / query-permission / common-infrastructure / javadoc）

冲突时优先相信当前模块真实代码和实际调用点，其次公共基础设施，最后 generator 模板。

RuoYi 已有基础设施（用户 / 认证 / RBAC / 数据权限 / OSS / 字典 / 日志 / SpringDoc / 代码生成器）优先复用，不重复实现，除非需求明确要求。

## 5. 博客领域红线

以下规则涉及数据安全与权限，任何时刻不可违反；完整语义见 PRD §4 / §16 与 database-design 对应章节：

- 不区分普通用户与作者，不新增 `AUTHOR` 角色；所有注册用户天然可发文，共用 PLUS-UI 后台。
- 永久舍弃 Category：不得建分类表，不得重新引入分类体系，内容组织只用全局标签。
- `author_id`（文章归属）与 `create_by`（记录操作者）语义不同；归属判断与操作权限必须由后端按当前登录用户强制校验，不得信任前端传入的 `authorId`。
- 公共文章必须同时满足 `status='1' AND visibility='0' AND del_flag='0'`，公共 API 不得绕过。
- `status`：`0=DRAFT`、`1=PUBLISHED`；`visibility`：`0=PUBLIC`、`1=USER_HIDDEN`、`2=ADMIN_HIDDEN`；作者隐藏与管理员下线是两个独立状态。
- 文章版本 append-only：`blog_article_version` 只允许 INSERT，禁止 UPDATE / DELETE；内容实际变化才创建新版本；`current_version_id` 指向当前生效版本。
- 逻辑删除遵循 RuoYi 风格：`del_flag` 为 `CHAR(1)`，Entity 用 `String` + `@TableLogic`（不要改成 `Long` 等类型）；除明确需求外不做物理删除。
- 不建立 MySQL FOREIGN KEY，关系由 Service / Transaction 维护。

## 6. 分层与事务

- Controller：接参、校验、权限入口、调用 Service、返回 `R<T>`；不堆复杂业务。
- Service：业务规则、状态转换、业务权限判断、事务边界、跨表协调。
- 公共 API、登录用户/作者 API、管理员 API 应在 Controller 层形成清晰边界；多个 Controller 可复用同一领域 Service。
- 多表原子操作显式 `@Transactional(rollbackFor = Exception.class)`，典型场景：保存文章 + 创建 Version + 回写 `current_version_id` + 同步 Tag；Tag Merge + 关系迁移 + 去重 + alias。
- 不要机械地给所有 Service 方法加事务。

## 7. 代码生成器原则与已验证坑

Generator 定位是生成 RuoYi 风格骨架，不是完成博客业务。Entity/BO/VO/Mapper/Service/Controller/基础 CRUD 可生成；ArticleVersion（append-only 约束）、TagAlias、ArticleTag、发布/隐藏/恢复、Tag Rename/Merge、作者权限校验、公共可见性过滤等必须生成后按业务规则改造，不能直接当普通 CRUD 用；不要让 Generator 默认 CRUD 反过来决定业务设计。

已验证的坑（务必遵守）：

- 生成器字段配置里，主键列必须勾选“查询”：BO 模板只为勾选了“查询/新增/修改”的列生成字段，而 ServiceImpl 模板无条件引用 `bo.setXxxId(...)` 主键回填，漏勾会直接编译错误。
- 导入表时 `packageName` 填 `org.dromara.blog`、`moduleName` 填 `blog`：模块目录名 `sd-blog` 含连字符，不能用作 Java 包名。
- 生成只支持下载 zip，需人工解压放入模块与前端工程；菜单 SQL 需手动执行。
- 修改表结构后先用“同步”功能刷新字段配置再重新生成。

## 8. 构建与排障

任何代码修改后至少验证：Maven 构建通过、`ruoyi-admin` 可启动、涉及 API 可访问、权限边界正确、关键数据库状态正确。

本项目特有的构建陷阱：

- mapstruct-plus 的转换器类是**编译期生成**的。运行时报 `ConvertException: cannot find converter` 大概率不是代码问题，而是 IDEA 增量编译与 Maven 产物混用导致生成类缺失——先 `mvn clean` 全量重编 + IDEA Rebuild，再排查源码。
- 复制模块目录时不要带上 `target/` 与 `.flattened-pom.xml` 残留；模块改名后必须 clean。
- IDEA Maven 同步报错（未解析插件等）时，先对齐 IDEA 与命令行的 Maven home / settings.xml，再判断是不是代码问题。
- ⚠️ **不要在父目录（多仓库同级的上层目录，如 `sd-blog/`）执行 `mvn`**：会在不受任何 `.gitignore` 保护的父目录落下游离 `target/`（mapstruct-plus 编译期产物）长期残留成噪音；构建一律在 `RuoYi-Vue-Plus/` 目录内执行（`./mvnw` 或 `mvn`）。详见 `docs/SETUP.md` §10.3。

测试策略（切片级约定）：

- 单元测试只覆盖纯逻辑（不碰数据库、不启 Spring 容器）：标签规范化、文章状态转换校验、内容变化比对（title/summary/coverUrl/content）、标签合并展平与去重的纯计算部分。这些逻辑必须实现为独立可测类（如 `TagNormalizer`、`ArticleStateMachine`），不得揉进 ServiceImpl 大方法。
- CRUD / HTTP 链路不写 Java 单测，由 Apifox 五类用例覆盖（正向 / 鉴权失败 / 参数校验 / 资源不存在 / 越权）；Controller 层不做 mock 测试。
- 单测与被测代码同模块 `src/test/java`，依赖根 pom 统一的 spring-boot-starter-test（JUnit5 + Mockito）。

## 9. 安全红线

- 不信任前端传入的 `authorId`、权限字段、关键状态字段；作者数据权限必须后端强制校验，隔离靠服务端过滤而非前端隐藏按钮。
- 公共接口不得泄露草稿、用户隐藏、管理员下线、逻辑删除数据。
- 富文本 HTML 保存前必须安全处理（危险标签、事件属性、危险 URL 协议），输出不得直接信任未经处理的用户 HTML；内容安全与业务权限是两条独立链路。
- 图片/文件上传限制类型与大小，复用 RuoYi OSS 能力，不重复实现对象存储。
- 不提交或硬编码真实生产 Token、密码、密钥、数据库凭据；本地测试配置不得改变生产安全策略。

## 10. Agent 工作流

中大型 Feature 遵循：

```text
阅读相关文档 → 阅读当前真实源码 → 寻找同类实现
→ 只读分析 + 实施计划 → 修改代码 → 编译 → 测试 → Review → Git commit
```

- 一次只实现一个明确 Feature，一个 feature 一个小步 commit；修改代码前建立 Git checkpoint；不要让两个 Agent 同时修改同一工作区；关键权限/删除逻辑人工复核。
- 开发切片顺序、功能需求编号、MVP 验收标准以 PRD §5 / §12.1 / §14 为准；每个切片闭环：数据库/后端 → Apifox → 前端 → 联调 → 浏览器验收。
- MVP 范围与“明确不做”清单以 PRD §2.3 / §15 为准（ES、多级缓存、PWA、第三方登录、点赞/收藏/关注、多级评论、评论前置审核、版本恢复、回收站、定时发布、AI 功能等，除需求文档明确加入外不主动实现）。
- 发现设计与现有代码冲突时，先明确指出冲突，不要默默改变设计；文档冲突时先识别冲突并报告，由最新明确决定裁决。

## 11. 参考文档

| 文档 | 内容 | 备注 |
|---|---|---|
| `docs/ROADMAP.md` | 开发切片排期 + 决策记录（已确认/待决策） | 开工前先查当前切片状态与待决事项；决策变更写入其 §6 |
| `docs/SETUP.md` | 开发环境搭建：容器、SQL 最小集、启动与冒烟清单 + 多仓库同级布局与工作区 | 新环境照此 30 分钟可跑通；clone 布局见其 §10 |
| `docs/architecture.md` | 系统架构：模块图、请求链路、部署拓扑、扩展边界 | 只讲结构不讲业务 |
| `docs/PRD博客MVP需求文档_v0.2.docx` | 产品需求基线：范围、业务规则、功能需求、验收标准、已确认决策 | Word 二进制，可用 python/unzip 解出 `word/document.xml` 阅读，或转 Markdown |
| `docs/blog-api.md` | 博客 MVP API 契约 v0.2：三层 API 边界、接口清单、权限标识、状态动作规则 | 接口实现与文档冲突时，先报告冲突再修订，不得默默偏离契约 |
| `docs/database-design-v0.1.md` | 数据库设计：字段语义、约束、索引、生命周期、实体映射 | 文中表名为 `blog_*` 逻辑命名 |
| `docs/blog_schema_v0.1.sql` | 实际建表 SQL | **表前缀 `sd_blog_`，以此与实际库为准** |
| `.codex/skills/ruoyi-plus-ai-coding/` | 编码规范 skill（SKILL.md + references）；`.qoder/skills/` 下同名条目是指向它的符号链接（Qoder 自动加载用，勿复制出第二份） | 后端/前端任务动手前先读对应 reference |
| `.claude/agents/backend-*.md` | 后端任务型子代理规则 | 按任务类型选用 |
