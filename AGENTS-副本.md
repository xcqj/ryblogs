# AGENTS.md

> 本文件是本仓库的项目级 Agent 说明。
> 作用：说明项目是什么、使用什么技术、代码放在哪里、全局遵守什么规则，以及 Agent 应如何工作。
> 具体产品需求、数据库字段、API 契约和业务流程应读取 `docs/` 下的对应文档，不在这里重复展开。

## 1. 项目概述

本仓库是基于 **RuoYi-Vue-Plus 6.X** 的后端项目，用于构建一个**多用户技术博客 / 多作者内容平台**。

博客业务通过独立模块 `ruoyi-blog` 接入现有 RuoYi-Vue-Plus 后端。

本仓库主要负责后端；PLUS-UI 6.X-Vue 是独立前端仓库，应使用其自己的项目规则。

项目目标：

- 先完成可真实运行的博客 MVP。
- MVP 完成后，再决定是否加入 Elasticsearch、Redis 多级缓存、PWA、SEO、CI/CD 等工程演进。
- 不为了“面试亮点”提前把非 MVP 技术写死。

## 2. 当前开发环境

- Windows 10
- WSL2 Ubuntu
- Java 21（当前开发环境：21.0.12+1.1-tem）
- Maven
- Docker
- MySQL 8.4.x（当前本地开发容器：8.4.9）
- Redis 8.x（当前本地开发容器：8.6.3）

Java、Maven、Docker 和后端项目主要运行在 WSL2 中。

不要把个人机器的绝对路径、代理地址、Docker 网络等环境细节写死到项目代码或通用规则中。

## 3. 核心技术栈

### Backend

- Java 21
- Spring Boot / Spring 生态
- RuoYi-Vue-Plus 6.X
- MyBatis-Plus
- Sa-Token
- MySQL
- Redis
- Maven

### Existing RuoYi capabilities

优先复用当前代码库已有能力，包括：

- 用户管理
- 登录认证
- 角色 / 权限 / RBAC
- 数据权限基础能力
- 文件管理
- OSS / MinIO
- Redis 基础能力
- 系统日志 / 操作日志 / 登录日志
- 参数 / 字典
- OpenAPI / SpringDoc
- 代码生成器

不要重新实现已有基础设施，除非当前需求明确要求。

## 4. 后端目录

博客模块：

```text
ruoyi-modules/
└── ruoyi-blog/
```

Java 包名：

```text
org.dromara.blog
```

Mapper XML：

```text
ruoyi-blog/src/main/resources/mapper/blog/
```

博客模块应遵循当前仓库已有的 Maven 模块接入方式。当前已知涉及：

- `ruoyi-modules/pom.xml`
- 根 `pom.xml`
- `ruoyi-admin/pom.xml`

除非当前代码库实际要求，不要额外修改 ComponentScan / MapperScan 配置。

## 5. 当前源码优先

实现任何功能时，优先级如下：

1. 当前仓库真实源码
2. 当前项目设计文档
3. 当前依赖版本的官方资料
4. 通用框架知识 / 其他项目示例

实现前应优先搜索当前项目中的同类代码，遵循当前 RuoYi-Vue-Plus 6.X 的实际模式。

不要照搬旧版 RuoYi 教程，也不要因为熟悉其他 Spring / MyBatis 项目而自行引入另一套模式。

遇到具体框架 API 不确定时，先查当前代码库和实际依赖版本的真实用法，不要猜测方法名。

## 6. 设计与实现的职责边界

业务需求、数据模型、API 契约、业务规则和 MVP 边界由项目设计确定。

Agent 主要负责：

- 阅读设计
- 调查当前代码库
- 找到可复用的实现
- 制定实施计划
- 修改代码
- 编译
- 测试
- 修复实现问题

不要自行重新设计已经确定的业务规则。

发现设计与现有代码实现冲突时，先明确指出冲突，不要默默改变设计。

## 7. 博客领域的全局约束

### 7.1 用户与作者

- 不区分普通用户和作者。
- 每个注册用户天然可以创建、编辑、发布自己的文章。
- 不新增 `AUTHOR` 角色。
- 作者和管理员共同使用 PLUS-UI 管理后台。

### 7.2 分类

项目明确舍弃 Category。

不得新增：

```text
blog_category
```

不得自行重新引入分类体系。

### 7.3 文章作者与权限

文章作者字段为：

```text
blog_article.author_id
```

`author_id` 与 RuoYi 的 `create_by` 不是同一语义。

作者权限判断必须基于后端当前登录用户身份；不得信任前端传入的 `authorId` 来决定文章归属或操作权限。

### 7.4 Article 状态

`status`：

```text
0 = DRAFT
1 = PUBLISHED
```

`visibility`：

```text
0 = PUBLIC
1 = USER_HIDDEN
2 = ADMIN_HIDDEN
```

公共文章必须满足：

```sql
status = '1'
AND visibility = '0'
AND del_flag = '0'
```

公共 API 不得绕过这条规则。

### 7.5 Article Version

文章正文通过 `current_version_id` 指向 `blog_article_version`。

Version 是完整快照，业务上为 append-only：

- 允许 INSERT
- 禁止 UPDATE
- 禁止 DELETE

保存文章时：

- 实际内容发生变化：创建新 Version
- 内容没有变化：不创建新 Version

已发布文章修改后，新 Version 成为当前生效版本；`published_at` 不因普通编辑而更新。

### 7.6 Tag

标签使用：

```text
blog_tag
blog_tag_alias
blog_article_tag
```

不得重新引入 Category。

标签规则包括：

- 全局唯一
- 用户可以创建
- 名称 / slug 必须按既定规范化规则处理
- 每篇文章 1～6 个标签
- slug 全局唯一

标签 Rename 必须保留 alias；Tag Merge 必须在事务中迁移关系、去重并处理 alias，不能形成长期的多级合并链。

### 7.7 逻辑删除

博客业务使用 RuoYi 风格逻辑删除。

当前数据库设计的 `del_flag` 为 `CHAR(1)`；Entity 应遵循当前项目真实用法（例如 `String` + `@TableLogic`），不要自行改成 `Long` 等其他类型。

除明确需求外，不提供物理删除。

## 8. API 与分层

公共 API、登录用户/作者 API、管理员 API 应在 Controller 层形成清晰边界。

多个 Controller 可以复用同一个领域 Service；Controller 与 Service 不需要一一对应。

### Controller

负责：

- 请求接收
- 参数校验
- API 层权限入口
- 调用 Service
- 返回结果

不要把复杂业务流程堆在 Controller。

### Service

负责：

- 业务规则
- 状态转换
- 业务权限判断
- 事务边界
- 跨表业务协调

### Mapper

负责数据访问，遵循当前 RuoYi-Vue-Plus / MyBatis-Plus 的实际项目模式。

## 9. 事务原则

涉及多个表且必须保持原子一致性的操作，应明确事务边界。

典型场景：

- 保存文章 + 创建 Version + 更新 `current_version_id` + 同步 Tag
- Tag Merge + ArticleTag 迁移 + 去重 + Alias 更新

不要机械地给所有 Service 方法添加事务。

## 10. Generator 原则

Generator 的定位是生成 RuoYi 风格基础代码骨架，而不是完成博客业务。

可用于生成：

- Entity
- BO
- VO
- Mapper
- Service
- ServiceImpl
- Controller
- 基础 CRUD

以下内容生成后必须按业务规则改造，不能直接当普通 CRUD：

- ArticleVersion
- TagAlias
- ArticleTag
- Article 发布 / 隐藏 / 恢复
- Article Version 管理
- Tag Rename / Merge
- 作者权限校验
- 公共文章统一可见条件

不要让 Generator 默认 CRUD 反过来决定业务设计。

## 11. 数据库原则

当前博客 SQL 使用 `sd_` 前缀，正式表包括：

```text
sd_blog_user_profile
sd_blog_article
sd_blog_article_version
sd_blog_tag
sd_blog_tag_alias
sd_blog_article_tag
sd_blog_comment
```

数据库细节以正式 SQL 和最新数据库设计文档为准。

统一原则：

- BIGINT 主键
- 应用侧生成主键，不使用 AUTO_INCREMENT
- Entity 继承当前项目 `BaseEntity`
- MySQL InnoDB
- utf8mb4
- DATETIME ↔ `LocalDateTime`
- 不建立 MySQL FOREIGN KEY，由应用层维护关系

## 12. 安全注意事项

- 不信任前端传入的 `authorId`、权限字段或关键状态字段。
- 作者数据权限必须由后端强制校验。
- 公共接口不得泄露草稿、用户隐藏、管理员下线或逻辑删除数据。
- 富文本正文为 HTML 链路时，必须考虑 XSS / HTML 安全处理。
- 不提交或硬编码真实生产 Token、密码、密钥、数据库凭据等敏感信息。
- 本地测试配置不得直接改变生产安全策略。

## 13. Agent 工作流

中大型 Feature 遵循：

```text
阅读相关文档
    ↓
阅读当前真实源码
    ↓
寻找同类实现
    ↓
只读分析 + 实施计划
    ↓
修改代码
    ↓
编译
    ↓
测试
    ↓
Review
```

一次尽量只实现一个明确 Feature。

AI 修改代码前优先建立 Git checkpoint；完成一个 Feature 后执行编译、测试、Review 和 Git commit。

不要一次性让 Agent “完成整个博客”。

## 14. 构建与测试

后端使用 Maven 构建。

具体 Maven 命令必须以当前仓库根 `pom.xml`、模块 POM 和实际脚本 / CI 为准；不要根据其他版本 RuoYi 猜测命令。

任何代码修改后至少应验证：

1. Maven 构建通过。
2. `ruoyi-admin` 可以正常启动。
3. 涉及的 API 可以实际访问。
4. 关键权限边界正确。
5. 关键数据库状态变化正确。

## 15. MVP 范围边界

除最新需求文档明确加入外，暂不主动实现：

- Elasticsearch
- Redis 多级缓存
- PWA
- 复杂 SEO / SSR / SSG
- 第三方登录
- 点赞 / 收藏 / 关注
- 私信 / 站内通知
- 多级评论
- 评论前置审核
- 文章版本恢复
- 回收站
- 定时发布
- AI 功能

## 16. 参考文档

实现博客功能前，应根据当前任务读取相关文档，例如：

```text
博客 MVP 需求文档
 database-design-v0.1.md
 blog_schema.sql
 blog-api.md
 blog-business-rules.md
 blog-implementation-spec.md
```

实际文件名以仓库当前版本为准。

文档发生冲突时，先识别冲突，再以最新明确的设计决定为准，不要默默选择。
