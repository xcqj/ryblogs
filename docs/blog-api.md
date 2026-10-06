# 博客 MVP API 契约

> 文档状态：v0.2（2026-09-28 按评审决议修订）
> 用途：作为 `sd-blog` 模块后端 API 的业务契约，供开发、Review、测试和 AI Agent 实现时使用。
>
> 本文定义接口契约与业务语义，不规定具体 Java 类名、RuoYi 返回包装类型、MyBatis-Plus 调用方式、异常类等实现细节。实现时以当前 RuoYi-Vue-Plus 6.X 源码中的真实模式为准。

## 1. API 总体原则

### 1.1 API 分层

```text
/blog/public/...      公共前台 API
/blog/article/...     登录用户文章与评论 API（作者自助，服务端按 author_id 隔离）
/blog/user/...        当前用户博客资料 API
/blog/admin/...       管理员治理 API
```

公共 API 与后台 API 不混用。

多个 Controller 可以复用同一个领域 Service；API 边界通过 Controller 划分，业务能力通过 Service 复用。

### 1.2 身份要求

除明确标记为“公开”的接口外，均要求登录。

作者权限判断必须以当前登录用户身份为依据：

```text
当前登录用户 ID
    ↓
article.author_id
    ↓
后端权限校验
```

不得信任前端传入的 `authorId` 来决定文章归属或操作权限。

管理员接口必须进行管理员权限校验。

### 1.3 公共文章可见条件

所有公共文章查询必须统一满足：

```sql
status = '1'
AND visibility = '0'
AND del_flag = '0'
```

其中：

```text
status:
0 = DRAFT
1 = PUBLISHED

visibility:
0 = PUBLIC
1 = USER_HIDDEN
2 = ADMIN_HIDDEN
```

该规则适用于公共：

- 首页 / 文章列表
- 文章详情
- 标签文章列表
- 搜索结果
- 作者公开文章
- 热门标签统计

### 1.4 ID 与 URL

API 业务标识统一使用数据库主键 ID。

文章 `slug` 当前仅作为预留字段，不作为 MVP 的主要文章路由。

标签使用 `slug` 作为公共 URL 标识。

## 2. 统一请求/响应约定

本文件只约定业务字段和语义。

以下内容暂不在本文件中写死：

- RuoYi 具体响应包装类型
- 分页对象的 Java 类型
- 具体 HTTP 异常类
- 具体业务错误码
- DTO / VO / BO 的 Java 类名
- Sa-Token 的具体调用方式

实现时以当前项目源码的既有约定为准。

### 2.1 分页参数

涉及分页的接口统一支持：

```text
pageNum
pageSize
```

具体默认值、最大值以当前项目公共分页规范为准。

### 2.2 分页返回

分页接口返回：

```text
rows
total
```

字段名与当前 RuoYi-Vue-Plus 项目统一分页对象（`PageResult`）保持一致。

### 2.3 成功/失败

具体响应 envelope 遵循当前 RuoYi-Vue-Plus 项目惯例。

业务失败至少需要能够表达：

```text
参数非法
资源不存在
无权限
状态不允许执行当前操作
业务规则校验失败
```

### 2.4 文件与图片上传

博客不提供独立上传 API。封面、正文图片等上传直接复用 RuoYi 现有 OSS 上传接口（`/resource/oss/upload`）。

**身份：** 登录用户。

上传返回的 ossId / url 由前端回填到业务字段（如 `coverUrl`）；文件类型与大小限制沿用 RuoYi OSS 现有配置。

## 3. Article API

### 3.1 创建文章

```http
POST /blog/article
```

**身份：** 登录用户。

**请求：**

```json
{
  "title": "Java 中的 xxx",
  "summary": "文章摘要",
  "coverUrl": "https://...",
  "content": "<p>正文 HTML...</p>",
  "tagNames": [
    "Java",
    "Spring"
  ]
}
```

**字段：**

| 字段 | 必填 | 说明 |
|---|---|---|
| title | 是 | 当前版本标题 |
| summary | 否 | 作者填写的摘要 |
| coverUrl | 否 | 封面地址 |
| content | 是 | 富文本 HTML |
| tagNames | 是 | 标签名称集合，1～6 个 |

**业务行为：**

```text
创建 blog_article
    ↓
status = DRAFT
visibility = PUBLIC
author_id = 当前登录用户
    ↓
创建 Version 1
    ↓
current_version_id = Version 1
    ↓
同步文章标签
```

**约束：**

- 标签数量必须为 1～6。
- 标签按既定规范化规则处理。
- 新标签允许自动入库。
- 文章正文存储在 Version 表，而不是 Article 表。
- 创建 Article、创建 Version、创建标签关系属于同一业务操作。

### 3.2 查询我的文章

```http
GET /blog/article/my
```

**身份：** 登录用户。

**查询范围：**

```text
author_id = 当前登录用户
AND del_flag = 0
```

**请求参数：**

```text
pageNum
pageSize
status
visibility
keyword
```

### 3.3 查询我的文章详情

```http
GET /blog/article/{articleId}
```

**身份：** 作者本人、管理员。

**业务规则：**

- 文章必须存在。
- 文章不能是逻辑删除数据。
- 普通登录用户只能查看自己拥有的文章。
- 管理员可以查看平台的全部文章。

**返回至少包含：**

```text
articleId
authorId
status
visibility
currentVersion
tags
publishedAt
viewCount
管理员治理信息（管理员场景）
```

`currentVersion` 至少包含：

```text
versionId
versionNo
title
summary
coverUrl
content
```

### 3.4 修改文章 / 保存文章

```http
PUT /blog/article/{articleId}
```

**身份：** 作者本人。管理员不可编辑任何文章内容（MVP 不提供管理员编辑接口）。

**请求：**

```json
{
  "title": "Java 中的 xxx",
  "summary": "文章摘要",
  "coverUrl": "https://...",
  "content": "<p>正文 HTML...</p>",
  "tagNames": [
    "Java",
    "Spring"
  ]
}
```

**作者编辑限制：**

作者只能修改自己的文章。

`ADMIN_HIDDEN` 文章作者不能继续编辑。

**Version 规则：**

先比较当前版本：

```text
title
summary
coverUrl
content
```

如果内容没有变化：

```text
不创建新 Version
```

如果内容发生变化：

```text
创建新 Version
version_no = 当前文章下一个版本号
current_version_id = 新 Version
```

历史 Version 不被修改。

**已发布文章：**

```text
保存
 ↓
创建新 Version（有实际变化时）
 ↓
新 Version 立即成为当前生效内容
```

`published_at` 不因为编辑而更新。

**Tag：**

保存文章时同步文章标签，数量必须保持 1～6。

### 3.5 发布文章

```http
POST /blog/article/{articleId}/publish
```

**身份：** 作者本人。管理员不可代作者发布（MVP）。

**状态：**

```text
DRAFT → PUBLISHED
```

**前置条件：**

- Article 存在
- 未逻辑删除
- 当前操作用户有权操作
- 存在有效 Version
- 至少有 1 个标签
- 当前状态为 DRAFT：`USER_HIDDEN` 只能由作者通过 3.7 `show` 恢复；`ADMIN_HIDDEN` 只能由管理员通过 11.4 `restore` 恢复，均不能借由 publish 重新公开

**首次发布：**

若 `published_at` 为空：

```text
published_at = 当前时间
```

之后重新公开不修改首次发布时间。

**注意：**

发布不是创建 Version 的动作。

## 3.6 用户隐藏文章

```http
POST /blog/article/{articleId}/hide
```

**身份：** 作者本人。

**状态：**

```text
PUBLIC → USER_HIDDEN
```

**结果：**

- 文章不进入公共页面。
- 作者仍拥有文章。
- 作者之后可以重新公开。
- 不改变 `status`。

### 3.7 用户重新公开文章

```http
POST /blog/article/{articleId}/show
```

**身份：** 作者本人。

**状态：**

```text
USER_HIDDEN → PUBLIC
```

该接口不能用于恢复 `ADMIN_HIDDEN`。

### 3.8 删除文章

```http
DELETE /blog/article/{articleId}
```

**身份：** 作者本人。MVP 不提供管理员删除文章接口，管理员治理手段为下线（见 11.3）。

作者可以删除：

```text
DRAFT
PUBLISHED
USER_HIDDEN
ADMIN_HIDDEN
```

删除方式：

```text
del_flag = 1
```

不得物理删除 Article。

删除后保留：

- Article Version
- Article Tag 关系
- Comment 数据

MVP 不提供文章回收站/恢复接口。

## 4. Article Version API

Version 是历史快照，不是普通 CRUD 资源。

### 4.1 查询文章版本列表

```http
GET /blog/article/{articleId}/versions
```

**身份：** 作者本人、管理员。

**返回至少：**

```text
versionId
articleId
versionNo
title
createBy
createTime
```

仅查询，不提供创建/修改/删除接口。

### 4.2 查询历史版本详情

```http
GET /blog/article/{articleId}/versions/{versionId}
```

**身份：** 作者本人、管理员。

**返回完整快照：**

```text
versionId
articleId
versionNo
title
summary
coverUrl
content
createBy
createTime
```

不得提供 Version 的 UPDATE / DELETE 接口。

MVP 不提供从历史版本恢复当前版本。

## 5. Public Article API

### 5.1 公共文章列表

```http
GET /blog/public/articles
```

**身份：** 公开。

**请求参数：**

```text
pageNum
pageSize
tagSlug    可选
authorId   可选
```

**查询条件：**

必须统一满足公共文章可见条件：

```sql
status = '1'
AND visibility = '0'
AND del_flag = '0'
```

**返回至少：**

```text
articleId
title
summary
coverUrl
author
publishedAt
viewCount
tags
```

默认排序：

```text
published_at DESC
```

### 5.2 公共文章详情

```http
GET /blog/public/articles/{articleId}
```

**身份：** 公开。

**查询条件：**

必须满足公共文章可见条件。

**返回至少：**

```text
article
currentVersion
author
tags
publishedAt
viewCount
```

**阅读量：**

成功打开公共文章详情时：

```text
view_count + 1
```

MVP 不做：

- IP 去重
- 用户去重
- 设备指纹
- Redis 热点计数
- 异步刷盘

## 6. Public Search API

### 6.1 搜索公开文章

```http
GET /blog/public/search
```

**身份：** 公开。

**请求参数：**

```text
keyword
pageNum
pageSize
```

搜索公开文章。

具体搜索字段和底层实现属于实现层问题。

以后引入 Elasticsearch 时，不应改变本 API 的业务契约。

## 7. Public Tag API

### 7.1 精选标签

```http
GET /blog/public/tags/featured
```

**身份：** 公开。

返回：

```text
is_featured = 1
```

并按 `sort_order` 排序。

### 7.2 热门标签

```http
GET /blog/public/tags/hot
```

**身份：** 公开。

按照当前公开文章的标签关联数量排序。

只统计公共文章。

不保存 `is_hot` 字段。

### 7.3 公共标签详情

```http
GET /blog/public/tags/{slug}
```

**身份：** 公开。

**slug 解析：**

```text
当前 canonical tag slug
    ↓ 找不到
tag_alias.alias_slug
    ↓
canonical tag
```

命中 alias 后，数据库负责完成 alias → canonical 的映射；最终 HTTP 301 由前台部署/边缘层承担。

### 7.4 标签下公开文章

```http
GET /blog/public/tags/{slug}/articles
```

**身份：** 公开。

**参数：**

```text
pageNum
pageSize
```

先解析当前 slug / alias slug，最终定位 canonical tag；只返回公共可见文章。

### 7.5 公共标签列表（含候选联想）

```http
GET /blog/public/tags
```

**身份：** 公开。

**参数：**

```text
pageNum
pageSize
keyword    可选，按名称 / slug 模糊匹配
```

只返回 `status = ACTIVE` 且未逻辑删除的标签。

**返回至少：**

```text
tagId
name
slug
```

该接口同时服务于：

- 前台“全部标签”页（PRD §6 `/tags`）。
- 作者编辑文章时的标签候选联想（TAG-02：输入即检索，已存在则复用，不存在则随文章自动入库）。

## 8. User Profile API

### 8.1 查询自己的博客资料

```http
GET /blog/user/profile
```

**身份：** 登录用户。

**返回至少：**

```text
userId
avatar
nickName
bio
```

### 8.2 修改自己的博客资料

```http
PUT /blog/user/profile
```

**身份：** 当前登录用户。

**请求：**

```json
{
  "bio": "个人简介"
}
```

**规则：**

- `userId` 从当前登录身份获取，不由前端指定。
- 第一次填写简介时可以创建 `blog_user_profile`。
- 没有博客简介时，允许没有 profile 记录。
- 不复制头像和昵称到博客 Profile 表。

## 9. Public User API

### 9.1 公共用户资料

```http
GET /blog/public/users/{userId}
```

**身份：** 公开。

**返回至少：**

```text
userId
avatar
nickName
bio
```

### 9.2 用户公开文章

```http
GET /blog/public/users/{userId}/articles
```

**身份：** 公开。

只返回：

```text
PUBLISHED
PUBLIC
del_flag = 0
```

的文章。

## 10. Comment API

### 10.1 公共评论列表

```http
GET /blog/public/articles/{articleId}/comments
```

**身份：** 公开。

只允许查询公共文章评论。

只返回未逻辑删除的评论。

MVP 为一级评论。

### 10.2 发表评论

```http
POST /blog/article/{articleId}/comments
```

**身份：** 登录用户。

**请求：**

```json
{
  "content": "评论内容"
}
```

**规则：**

评论对象必须是：

```text
PUBLISHED + PUBLIC + del_flag = 0
```

创建：

```text
user_id = 当前登录用户
article_id = 路径中的文章 ID
parent_id = NULL
```

MVP：

- 不允许修改评论。
- 不提供用户删除自己评论。
- 不做评论预审核。
- 管理员可以删除违规评论。

## 11. Admin Article API

### 11.1 管理员文章列表

```http
GET /blog/admin/articles
```

**身份：** 管理员。

可以看到非公共状态，例如：

```text
DRAFT
PUBLISHED
USER_HIDDEN
ADMIN_HIDDEN
```

### 11.2 管理员文章详情

```http
GET /blog/admin/articles/{articleId}
```

**身份：** 管理员。

管理员可以查看平台任意文章的完整当前状态以及治理信息。

### 11.3 管理员下线文章

```http
POST /blog/admin/articles/{articleId}/hide
```

**身份：** 管理员。

**请求：**

```json
{
  "reason": "内容违规说明"
}
```

**状态：**

```text
PUBLISHED / PUBLIC
        ↓
ADMIN_HIDDEN
```

**写入治理字段：**

```text
admin_hidden_reason
admin_hidden_by
admin_hidden_time
```

下线后：

- 文章不进入公共页面。
- 文章继续保留。
- 作者仍拥有文章。
- 作者不能继续编辑。

### 11.4 管理员恢复文章

```http
POST /blog/admin/articles/{articleId}/restore
```

**身份：** 管理员。

**状态：**

```text
ADMIN_HIDDEN → PUBLIC
```

这是管理员治理操作，不属于作者申诉流程。

MVP 不实现申诉系统。

## 12. Admin Comment API

### 12.1 管理员评论列表

```http
GET /blog/admin/comments
```

**身份：** 管理员。

可选筛选：

```text
articleId
userId
keyword
```

### 12.2 管理员删除评论

```http
DELETE /blog/admin/comments/{commentId}
```

**身份：** 管理员。

逻辑删除：

```text
del_flag = 1
```

不物理删除评论。

## 13. Admin Tag API

### 13.1 标签管理列表

```http
GET /blog/admin/tags
```

**身份：** 管理员。

可查看：

```text
ACTIVE
MERGED
```

以及治理信息。

### 13.2 标签详情

```http
GET /blog/admin/tags/{tagId}
```

**身份：** 管理员。

### 13.3 更新标签运营属性

```http
PUT /blog/admin/tags/{tagId}
```

**身份：** 管理员。

用途：

```text
isFeatured
sortOrder
```

不要通过普通更新接口直接修改：

```text
name
slug
status
mergedInto
```

标签重命名、合并必须使用专用动作接口。

**约束：** 目标标签 `status` 必须为 `ACTIVE`（0 正常）。对 `MERGED`（1 已合并）标签设置运营属性返回失败——已合并标签是历史节点，不参与精选与候选。

### 13.4 重命名标签

```http
PUT /blog/admin/tags/{tagId}/rename
```

**身份：** 管理员。

**请求：**

```json
{
  "name": "Spring Boot"
}
```

**规则：**

旧 name / slug 必须保留为 alias：

```text
旧 name / slug
    ↓
创建 TagAlias
    ↓
更新当前 Tag
```

当前 `tag_id` 不变。

### 13.5 合并标签

```http
POST /blog/admin/tags/{sourceTagId}/merge
```

**身份：** 管理员。

**请求：**

```json
{
  "targetTagId": 20
}
```

**前置条件：**

- source != target
- source 为当前有效可合并标签
- target 必须是当前 ACTIVE 标签
- 不允许形成循环

**事务行为：**

必须在同一事务内：

```text
1. 迁移 ArticleTag 关系
2. 对重复 ArticleTag 去重
3. 处理 alias
4. source.status = MERGED
5. source.merged_into = 最终目标
6. 历史 alias 展平到最终 canonical tag
```

例如：

```text
A → B
B → C
```

最终：

```text
A → C
B → C
```

不保留长期合并链。

**精选身份转移：** 合并事务内，`source` 的精选属性转移到 `target`（精选是标签级属性，不随版本快照）：

```text
source.is_featured = 1 且 target.is_featured ≠ 1
    → target.is_featured = 1，target.sort_order = source.sort_order
source.is_featured = 1 且 target.is_featured = 1
    → target 保持其现有精选状态与排序不变
无论 source 是否精选
    → 合并后一律确保 source.is_featured = 0、source.sort_order = 0
```

该转移保证 `MERGED` 记录上不残留精选标记（数据不变量：不存在 status=1 且 is_featured=1 的标签）。

### 13.6 删除垃圾标签

```http
DELETE /blog/admin/tags/{tagId}
```

**身份：** 管理员。

普通删除：

```text
解除 ArticleTag 关系
Tag 逻辑删除
```

普通删除不创建 alias。

旧 URL 可以返回 404。

## 14. API 与业务状态的边界

不要提供通用状态修改接口，例如：

```http
PUT /blog/article/{id}
{
  "status": "PUBLISHED",
  "visibility": "ADMIN_HIDDEN"
}
```

状态变化应通过语义明确的动作 API：

```text
publish
hide
show
admin hide
admin restore
```

这样可以限制非法状态转换、明确权限并降低测试复杂度。

以下表不应作为普通 CRUD 资源直接暴露：

```text
blog_article_version
blog_tag_alias
blog_article_tag
```

它们由领域业务操作维护。

> 实现说明：当前 `sd-blog` 模块中代码生成器产出的 `SdBlogArticleVersionController`、`SdBlogTagAliasController`、`SdBlogArticleTagController` 已按本条停用（不注册路由），仅保留其 Service / Mapper 供领域业务操作复用。

## 15. API 与公共前台的边界

公共（匿名）页面不得直接调用作者/管理员接口获取内部数据。

> 边界澄清（D29）：本节约束的是**匿名公共页面**，不是整个 blog-web 站点。blog-web 的**登录作者区（作者中心，S12）**可调用 `/blog/article/**` 作者自助层管理本人文章；`/blog/admin/**` 平台治理仍仅管理端 plus-ui 调用。三层边界不因前端载体而变。

例如：

```text
公共文章详情
→ /blog/public/articles/{id}

作者查看自己的文章
→ /blog/article/{id}

管理员查看平台文章
→ /blog/admin/articles/{id}
```

三个接口可以共享底层 Service 能力，但查询语义和权限边界必须保持独立。

## 16. 暂不属于 MVP 的 API

目前不提供：

- 文章版本恢复
- 文章回收站 / 恢复
- 定时发布
- 第三方登录
- 点赞
- 收藏
- 关注
- 私信
- 站内通知
- 多级评论
- 评论预审核
- 申诉恢复管理员下线文章
- AI 功能

Elasticsearch、复杂 Redis 缓存、PWA、复杂 SEO 等工程能力不改变当前 MVP API 契约；是否加入以后单独决定。

## 17. MVP API 一览

权限列约定：

- `匿名`：公开接口，需加入 `application.yml` 的 `security.excludes` 白名单。
- `登录`：作者自助接口，仅要求登录（`@SaCheckLogin`），数据归属由服务端按 `author_id` / 当前用户强制过滤，不设按钮级权限。
- `blog:*`：管理员治理接口，使用 `@SaCheckPermission`，权限标识遵循 `blog:资源:动作`。

| 场景 | Method | Endpoint | 权限 |
|---|---|---|---|
| 创建文章 | POST | `/blog/article` | 登录 |
| 我的文章 | GET | `/blog/article/my` | 登录 |
| 我的文章详情 | GET | `/blog/article/{id}` | 登录 |
| 保存文章 | PUT | `/blog/article/{id}` | 登录 |
| 发布 | POST | `/blog/article/{id}/publish` | 登录 |
| 用户隐藏 | POST | `/blog/article/{id}/hide` | 登录 |
| 用户重新公开 | POST | `/blog/article/{id}/show` | 登录 |
| 删除文章 | DELETE | `/blog/article/{id}` | 登录 |
| 版本列表 | GET | `/blog/article/{id}/versions` | 登录 |
| 版本详情 | GET | `/blog/article/{id}/versions/{versionId}` | 登录 |
| 公共文章列表 | GET | `/blog/public/articles` | 匿名 |
| 公共文章详情 | GET | `/blog/public/articles/{id}` | 匿名 |
| 公共搜索 | GET | `/blog/public/search` | 匿名 |
| 精选标签 | GET | `/blog/public/tags/featured` | 匿名 |
| 热门标签 | GET | `/blog/public/tags/hot` | 匿名 |
| 公共标签列表/联想 | GET | `/blog/public/tags` | 匿名 |
| 公共标签详情 | GET | `/blog/public/tags/{slug}` | 匿名 |
| 标签文章 | GET | `/blog/public/tags/{slug}/articles` | 匿名 |
| 公共用户资料 | GET | `/blog/public/users/{userId}` | 匿名 |
| 用户公开文章 | GET | `/blog/public/users/{userId}/articles` | 匿名 |
| 我的博客资料 | GET | `/blog/user/profile` | 登录 |
| 更新博客资料 | PUT | `/blog/user/profile` | 登录 |
| 公共评论 | GET | `/blog/public/articles/{id}/comments` | 匿名 |
| 发表评论 | POST | `/blog/article/{id}/comments` | 登录 |
| 管理文章列表 | GET | `/blog/admin/articles` | `blog:article:list` |
| 管理文章详情 | GET | `/blog/admin/articles/{id}` | `blog:article:query` |
| 管理员下线 | POST | `/blog/admin/articles/{id}/hide` | `blog:article:adminHide` |
| 管理员恢复 | POST | `/blog/admin/articles/{id}/restore` | `blog:article:adminRestore` |
| 管理评论列表 | GET | `/blog/admin/comments` | `blog:comment:list` |
| 管理员删评论 | DELETE | `/blog/admin/comments/{id}` | `blog:comment:remove` |
| 标签管理列表 | GET | `/blog/admin/tags` | `blog:tag:list` |
| 标签详情 | GET | `/blog/admin/tags/{id}` | `blog:tag:query` |
| 更新标签运营属性 | PUT | `/blog/admin/tags/{id}` | `blog:tag:edit` |
| 标签重命名 | PUT | `/blog/admin/tags/{id}/rename` | `blog:tag:rename` |
| 标签合并 | POST | `/blog/admin/tags/{id}/merge` | `blog:tag:merge` |
| 删除垃圾标签 | DELETE | `/blog/admin/tags/{id}` | `blog:tag:remove` |

## 18. 待在实现层确定的事项

以下内容本契约暂不硬编码，应由 Qoder 阅读当前 RuoYi-Vue-Plus 6.X 源码后确定：

1. 具体 Controller / Service / Mapper 类名。
2. Request DTO / BO / VO 的具体字段拆分。
3. 当前项目统一响应包装。
4. 当前项目统一分页对象。
5. 当前项目业务异常及错误码体系。
6. Sa-Token 当前项目的登录用户获取方式。
7. RuoYi 当前数据权限注解和实现方式。
8. MyBatis-Plus / Mapper 的具体查询与更新写法。
9. 富文本 HTML 的实际安全过滤实现方式。
10. 标签 slug 的精确规范化实现代码。
11. 公共 / 我的文章列表所需的 title、summary、coverUrl 物理存储在 Version 表（Article 主表无这些字段），列表查询需联查 `current_version_id` 对应版本（联表或两段式查询，注意避免 N+1）。
12. `version_no` 并发生成策略：数据库唯一键 `(article_id, version_no)` 兜底，冲突时重试或提示“请刷新后重试”由实现层决定。

这些属于实现细节，不得反过来改变本文已经确定的业务契约。
