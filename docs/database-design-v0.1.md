# 博客数据库设计 v0.1

## 1. 文档定位

本文件是“多用户技术博客 / 多作者内容平台”的数据库设计基线，基于：

- RuoYi-Vue-Plus 6.X
- PLUS-UI 6.X-Vue
- MySQL 8.4 / InnoDB
- MyBatis-Plus
- RuoYi `BaseEntity` 与 `@TableLogic` 约定

本文先冻结领域模型、字段语义、约束和索引，不直接规定最终 SQL 实现细节；建表 SQL 应依据本文件生成。

---

## 2. 已确定的核心业务规则

### 2.1 用户

- 复用 RuoYi `sys_user`，不新建独立用户表。
- 没有“普通用户 / 作者”的业务区分；所有注册用户默认都可以创建和发布文章。
- 用户可以设置头像、昵称、个人简介；头像和昵称复用 `sys_user`，博客侧额外资料放在 `blog_user_profile`。
- 一个用户最多对应一个 `blog_user_profile`。
- 用户停用沿用 RuoYi 现有用户状态；用户历史文章继续保留。
- MVP 不做删除用户。

### 2.2 文章

- 一篇文章可以没有分类；本项目取消 Category，仅使用 Tag。
- 每篇文章必须有 1～6 个标签。
- 文章标题允许重复。
- `slug` 预留，文章 slug 全局唯一；MVP 不依赖 slug 作为主要访问方式。
- 保存草稿与发布是两个不同动作。
- 新文章首次保存：`DRAFT`。
- 新文章发布：`PUBLISHED`，立即公开。
- 已发布文章修改后点击保存：直接生成新版本并立即生效，不进入待审核状态。
- 管理员对已发布内容采取“发布后治理”：发现违规可将文章设置为 `ADMIN_HIDDEN`。
- 用户可以主动隐藏已发布文章，使用 `USER_HIDDEN`；用户可以重新公开自己隐藏的文章。
- `ADMIN_HIDDEN` 与 `USER_HIDDEN` 必须是两个独立业务状态。
- 被管理员下线的文章：作者仍拥有文章，可以在后台看到；MVP 不允许继续编辑或自行恢复。若想再次发布，通常复制内容创建新文章；申诉恢复属于后续功能。
- 草稿、已发布、用户隐藏、管理员下线的文章都允许作者删除；删除采用逻辑删除。
- 文章软删除后，文章历史版本、文章标签关系、评论数据均保留，不额外清理。
- 当前版本通过 `current_version_id` 指向 `blog_article_version`。
- 每个版本保存文章完整内容快照：标题、摘要、封面、正文。
- MVP 不把标签历史放入版本表。
- 每次实际内容发生变化并点击保存时创建新版本；无实际变化不创建新版本。
- 版本一旦创建不可修改、不可删除，版本表以追加写入为主。
- `published_at` 表示首次发布成功时间；后续修改不更新该字段。
- 阅读量 MVP 采用文章详情打开后 `view_count + 1`，不做复杂去重。

### 2.3 标签

- 取消 Category，建立全局扁平标签池。
- 标签名称对用户可见；系统对输入做规范化并生成 slug。
- 规范化规则至少包括：trim、去首尾空白、去多余空白、去重、小写化、空格转连字符、去除特殊字符。
- 例如 `Python 爬虫` → `python-爬虫`。
- MVP 不做复杂语义判断；`AI` 与 `人工智能` 可以同时存在。
- 标签 slug 作为规范化后的全局身份标识。
- 作者可创建新标签，创建后自动入库，不需要管理员预审核。
- 已存在的规范化标签直接复用；不能重复创建。
- 每篇文章 1～6 个标签，数量限制由业务层校验。
- 热门标签：按当前公开文章关联数量动态排序，不持久化 `is_hot`。
- 精选标签：管理员手动设置 `is_featured`，并使用 `sort_order` 控制展示顺序。
- 管理员可以重命名标签、合并同义标签、删除垃圾标签。
- 普通标签删除：解除文章关联；不自动迁移到其他标签。
- 标签删除采用逻辑删除；删除后如果再次输入相同 slug，优先恢复原标签，而不是创建新的同名标签。
- 合并标签不删除源标签；源标签保持 `status = MERGED`，通过 `merged_into` 指向目标标签。
- 合并目标必须是当前有效的 `ACTIVE` 标签。
- 合并关系一次性展平，禁止形成 A → B → C 的长期链；当 B → C 时，原本指向 B 的 merged 标签和 alias 直接更新为指向 C。
- 合并文章标签关系时“先迁移，再去重”：目标关系不存在则迁移；目标关系已经存在则保留目标关系并逻辑删除重复关系。
- 合并与相关标签关系迁移、alias 更新必须在同一个事务中完成。
- 标签重命名和合并会保存旧 slug / 旧名字为 alias；旧 URL 由 alias 解析到当前有效标签，并由路由层实现 301。
- 普通垃圾标签删除不产生 alias；不存在对应当前标签时旧 URL 直接返回 404。
- Alias 始终直接指向最终有效的 canonical tag。

### 2.4 评论

- 仅登录用户可以评论。
- 仅 `PUBLISHED + PUBLIC` 的文章可以发表评论。
- MVP 只有一级评论，`parent_id` 预留但当前必须为 `NULL`。
- 评论不能修改。
- 评论采用后置治理；管理员可删除违规评论。
- 评论删除采用逻辑删除。
- 用户不能删除自己的评论（MVP）。

---

## 3. 表总览

| 表 | 类型 | 说明 |
|---|---|---|
| `blog_user_profile` | 业务表 | 博客侧用户资料扩展 |
| `blog_article` | 核心业务表 | 文章身份、状态、可见性、当前版本、统计、治理信息 |
| `blog_article_version` | 历史表 | 不可变的完整文章内容快照 |
| `blog_tag` | 核心业务表 | 全局标签池、精选属性、合并关系 |
| `blog_tag_alias` | 历史/映射表 | 标签历史 slug/name 到当前标签的映射 |
| `blog_article_tag` | 关系表 | 文章与标签的多对多关系 |
| `blog_comment` | 业务表 | 用户评论 |

RuoYi 现有表：`sys_user`、`sys_role`、`sys_user_role` 等直接复用，不在本文件重复定义。

---

## 4. 通用表结构规范

除 `blog_article_version` 的“不可变历史记录”业务约束外，所有博客表统一遵循 RuoYi 风格：

```text
主键：BIGINT NOT NULL，应用侧生成，不 AUTO_INCREMENT
审计字段：
  create_dept BIGINT NULL
  create_by   BIGINT NULL
  create_time DATETIME NULL
  update_by   BIGINT NULL
  update_time DATETIME NULL
逻辑删除：
  del_flag CHAR(1) NOT NULL DEFAULT '0'
```

Java Entity 统一继承 RuoYi `BaseEntity`，业务实体中的 `delFlag` 使用 `@TableLogic`。

当前 RuoYi `BaseEntity` 提供 `createDept/createBy/createTime/updateBy/updateTime`，博客业务表遵循这一套命名和自动填充约定。

不建立 MySQL `FOREIGN KEY`，由应用层维护实体关系与级联逻辑。

数据库字符集建议使用 `utf8mb4`，存储引擎使用 InnoDB。

---

## 5. `blog_user_profile`

### 5.1 职责

保存博客侧用户扩展资料。头像、昵称直接复用 `sys_user.avatar`、`sys_user.nick_name`。

### 5.2 字段

| 字段 | 类型 | NULL | 默认值 | 说明 |
|---|---|---:|---:|---|
| `user_id` | BIGINT | 否 | — | RuoYi 用户 ID；同时作为主键 |
| `bio` | VARCHAR(500) | 是 | NULL | 个人简介 |
| `create_dept` | BIGINT | 是 | NULL | 创建部门 |
| `create_by` | BIGINT | 是 | NULL | 创建者 |
| `create_time` | DATETIME | 是 | NULL | 创建时间 |
| `update_by` | BIGINT | 是 | NULL | 更新者 |
| `update_time` | DATETIME | 是 | NULL | 更新时间 |
| `del_flag` | CHAR(1) | 否 | `'0'` | 逻辑删除 |

### 5.3 约束

- `PRIMARY KEY (user_id)`。
- 不建立物理外键到 `sys_user`。
- 用户第一次修改博客资料时再创建 profile 记录即可。
- MVP 不提供删除 profile 的业务接口。

---

## 6. `blog_article`

### 6.1 职责

文章主表，只保存文章的身份、归属、状态、可见性、当前版本指针、统计和治理信息；正文内容存放在 `blog_article_version`。

### 6.2 字段

| 字段 | 类型 | NULL | 默认值 | 说明 |
|---|---|---:|---:|---|
| `article_id` | BIGINT | 否 | — | 文章 ID |
| `author_id` | BIGINT | 否 | — | 作者用户 ID，对应 `sys_user.user_id` |
| `slug` | VARCHAR(200) | 是 | NULL | 文章 URL 标识，预留；全局唯一 |
| `status` | CHAR(1) | 否 | `'0'` | `0=草稿`，`1=已发布` |
| `visibility` | CHAR(1) | 否 | `'0'` | `0=PUBLIC`，`1=USER_HIDDEN`，`2=ADMIN_HIDDEN` |
| `current_version_id` | BIGINT | 是 | NULL | 当前生效文章版本 ID |
| `view_count` | BIGINT | 否 | `0` | 阅读量 |
| `published_at` | DATETIME | 是 | NULL | 首次发布成功时间 |
| `admin_hidden_reason` | VARCHAR(500) | 是 | NULL | 管理员下线原因 |
| `admin_hidden_by` | BIGINT | 是 | NULL | 执行下线的管理员用户 ID |
| `admin_hidden_time` | DATETIME | 是 | NULL | 管理员下线时间 |
| `create_dept` | BIGINT | 是 | NULL | 创建部门 |
| `create_by` | BIGINT | 是 | NULL | 创建者 |
| `create_time` | DATETIME | 是 | NULL | 创建时间 |
| `update_by` | BIGINT | 是 | NULL | 更新者 |
| `update_time` | DATETIME | 是 | NULL | 更新时间 |
| `del_flag` | CHAR(1) | 否 | `'0'` | 逻辑删除 |

### 6.3 关键语义

`author_id` 表示文章归属作者；`create_by/update_by` 表示实际操作该数据库记录的用户，两者语义不同。

公共文章必须同时满足：

```text
status = '1'
AND visibility = '0'
AND del_flag = '0'
```

管理员下线后：

```text
status = '1'
visibility = '2'
del_flag = '0'
```

用户主动隐藏后：

```text
status = '1'
visibility = '1'
del_flag = '0'
```

文章删除只修改 `del_flag`，不修改历史版本和标签历史。

### 6.4 索引/约束

```text
PRIMARY KEY (article_id)
UNIQUE KEY uq_blog_article_slug (slug)
KEY idx_blog_article_author_status (author_id, status, visibility, del_flag, update_time)
KEY idx_blog_article_public (status, visibility, del_flag, published_at)
```

`slug` 当前允许 NULL；MVP 不要求每篇文章立即生成 slug。

---

## 7. `blog_article_version`

### 7.1 职责

文章版本历史。每条记录是一份完整文章内容快照，创建后不可更新、不可删除。

### 7.2 字段

| 字段 | 类型 | NULL | 默认值 | 说明 |
|---|---|---:|---:|---|
| `version_id` | BIGINT | 否 | — | 版本 ID |
| `article_id` | BIGINT | 否 | — | 所属文章 ID |
| `version_no` | INT | 否 | — | 当前文章内的版本号，从 1 开始 |
| `title` | VARCHAR(200) | 否 | — | 文章标题快照 |
| `summary` | VARCHAR(500) | 是 | NULL | 文章摘要快照 |
| `cover_url` | VARCHAR(500) | 是 | NULL | 封面 URL 快照 |
| `content` | LONGTEXT | 否 | — | WangEditor 输出的文章 HTML 快照 |
| `create_dept` | BIGINT | 是 | NULL | 创建部门 |
| `create_by` | BIGINT | 是 | NULL | 该版本创建者 |
| `create_time` | DATETIME | 是 | NULL | 版本创建时间 |
| `update_by` | BIGINT | 是 | NULL | 统一表结构字段；业务层禁止更新 |
| `update_time` | DATETIME | 是 | NULL | 统一表结构字段；业务层禁止更新 |
| `del_flag` | CHAR(1) | 否 | `'0'` | 统一软删除字段；业务层禁止删除 |

### 7.3 关键语义

版本表完整保存：

```text
title + summary + cover_url + content
```

不保存：

```text
author_id
status
visibility
view_count
published_at
tags
```

因为这些属于 Article 层元数据，不属于内容快照。

### 7.4 业务约束

- Version 只允许 INSERT。
- 禁止修改历史版本。
- 禁止删除历史版本。
- 每次实际内容变化并点击保存才创建新版本。
- 无实际变化则不创建新版本。
- `version_no` 在同一 `article_id` 内唯一。

### 7.5 索引/约束

```text
PRIMARY KEY (version_id)
UNIQUE KEY uq_blog_article_version_no (article_id, version_no)
KEY idx_blog_article_version_article (article_id, version_no)
```

文章创建、生成版本、回写 `current_version_id` 应在同一个事务中完成。

---

## 8. `blog_tag`

### 8.1 职责

全局标签池。标签是独立内容分类维度，取消 Category。

### 8.2 字段

| 字段 | 类型 | NULL | 默认值 | 说明 |
|---|---|---:|---:|---|
| `tag_id` | BIGINT | 否 | — | 标签 ID |
| `name` | VARCHAR(50) | 否 | — | 当前展示名称 |
| `slug` | VARCHAR(100) | 否 | — | 当前规范化 slug，全局唯一 |
| `status` | CHAR(1) | 否 | `'0'` | `0=ACTIVE`，`1=MERGED` |
| `merged_into` | BIGINT | 是 | NULL | 合并目标标签 ID；ACTIVE 时必须为 NULL |
| `is_featured` | CHAR(1) | 否 | `'0'` | `0=普通`，`1=精选` |
| `sort_order` | INT | 否 | `0` | 精选标签展示顺序 |
| `create_dept` | BIGINT | 是 | NULL | 创建部门 |
| `create_by` | BIGINT | 是 | NULL | 创建者 |
| `create_time` | DATETIME | 是 | NULL | 创建时间 |
| `update_by` | BIGINT | 是 | NULL | 更新者 |
| `update_time` | DATETIME | 是 | NULL | 更新时间 |
| `del_flag` | CHAR(1) | 否 | `'0'` | 逻辑删除 |

### 8.3 关键约束

```text
PRIMARY KEY (tag_id)
UNIQUE KEY uq_blog_tag_slug (slug)
KEY idx_blog_tag_status (status, del_flag)
KEY idx_blog_tag_featured (is_featured, sort_order, del_flag)
KEY idx_blog_tag_merged_into (merged_into)
```

规则：

- `ACTIVE`：`merged_into IS NULL`。
- `MERGED`：`merged_into IS NOT NULL`，且指向最终有效标签。
- `MERGED` 标签不参与作者新建标签候选、不参与精选/热门标签列表。
- 合并后的源标签保持 `del_flag = '0'`，因为 `status=MERGED + merged_into` 本身就是治理历史。
- 标签普通删除：`del_flag = '1'`；再次创建同 slug 时，在未被 alias 占用的前提下恢复原记录。
- 标签重命名不改变 `tag_id`，只更新当前 `name/slug`，并创建旧标识的 alias。

---

## 9. `blog_tag_alias`

### 9.1 职责

保存标签历史名称和旧 slug，用于旧 URL / 旧标识重定向到当前 canonical tag。

### 9.2 字段

| 字段 | 类型 | NULL | 默认值 | 说明 |
|---|---|---:|---:|---|
| `tag_alias_id` | BIGINT | 否 | — | Alias ID |
| `tag_id` | BIGINT | 否 | — | 指向当前有效 canonical tag |
| `alias_slug` | VARCHAR(100) | 否 | — | 旧 slug |
| `alias_name` | VARCHAR(50) | 否 | — | 旧标签名称 |
| `create_dept` | BIGINT | 是 | NULL | 创建部门 |
| `create_by` | BIGINT | 是 | NULL | 创建者 |
| `create_time` | DATETIME | 是 | NULL | 创建时间 |
| `update_by` | BIGINT | 是 | NULL | 更新者 |
| `update_time` | DATETIME | 是 | NULL | 更新时间 |
| `del_flag` | CHAR(1) | 否 | `'0'` | 逻辑删除 |

### 9.3 索引/约束

```text
PRIMARY KEY (tag_alias_id)
UNIQUE KEY uq_blog_tag_alias_slug (alias_slug)
KEY idx_blog_tag_alias_tag_id (tag_id, del_flag)
```

### 9.4 关键语义

例如：

```text
tag_id = 10
canonical slug = javascript

alias:
10 | js         | JS
10 | ecmascript | ECMAScript
```

访问 `/tags/js`：

```text
1. 查询未删除 Tag 的 `slug`。
2. 如果命中 `ACTIVE`：正常返回。
3. 如果命中 `MERGED`：沿 `merged_into` 取得最终有效 Tag，并返回 301。
4. 如果 Tag 查询不到，再查询 `tag_alias.alias_slug`。
5. Alias 命中后取得最终有效 Tag，并返回 301。
6. 两者都没有：404。
```

Alias 始终直接指向最终有效标签，不形成 alias → alias 链。

### 9.5 跨表 slug 唯一性

数据库无法通过普通 UNIQUE 约束保证：

```text
blog_tag.slug
```
与
```text
blog_tag_alias.alias_slug
```
跨表互不冲突。

因此系统必须在 Service/事务层维护“标签 URL 标识命名空间”。MVP 规则为：

> **一个 Alias slug 不能与任何已有 Tag 记录的 slug 重复，包括逻辑删除和 `MERGED` 状态的 Tag。**

这样可以避免一个 slug 同时存在“Tag 记录”和“Alias 记录”的歧义，也保留了删除后恢复原 Tag 的能力。

对于合并：源 Tag 自身保留原有 `slug`，状态改为 `MERGED`，访问该 slug 时通过 `merged_into` 直接跳转；只有更早的历史 slug/name 才进入 `blog_tag_alias`。

标签创建、重命名、合并、alias 创建、逻辑删除与恢复时必须进行统一冲突检查；并发场景需要事务/锁机制保障。

---

## 10. `blog_article_tag`

### 10.1 职责

文章与标签的多对多关系。

### 10.2 字段

| 字段 | 类型 | NULL | 默认值 | 说明 |
|---|---|---:|---:|---|
| `article_tag_id` | BIGINT | 否 | — | 关系 ID |
| `article_id` | BIGINT | 否 | — | 文章 ID |
| `tag_id` | BIGINT | 否 | — | 标签 ID |
| `create_dept` | BIGINT | 是 | NULL | 创建部门 |
| `create_by` | BIGINT | 是 | NULL | 创建者 |
| `create_time` | DATETIME | 是 | NULL | 创建时间 |
| `update_by` | BIGINT | 是 | NULL | 更新者 |
| `update_time` | DATETIME | 是 | NULL | 更新时间 |
| `del_flag` | CHAR(1) | 否 | `'0'` | 逻辑删除 |

### 10.3 索引/约束

```text
PRIMARY KEY (article_tag_id)
UNIQUE KEY uq_blog_article_tag (article_id, tag_id, del_flag)
KEY idx_blog_article_tag_tag_article (tag_id, article_id, del_flag)
KEY idx_blog_article_tag_article (article_id, tag_id, del_flag)
```

### 10.4 业务约束

- 一篇文章关联标签数必须为 1～6，由 Service 层控制。
- 不能创建相同 `(article_id, tag_id)` 的有效重复关系。
- 删除关系：逻辑删除。
- 再次添加原标签：优先恢复历史关系记录，而不是新建重复记录。
- 文章软删除时，不处理、不删除、不修改既有标签关系。

### 10.5 标签合并

A → B 时：

1. 查找所有有效 `A` 关系。
2. 将其迁移到 B。
3. 若 `(article_id, B)` 已存在，则保留 B 的有效关系并将 A 的重复关系逻辑删除。
4. 最终保证每篇文章不会产生重复标签。
5. 与 Tag 状态更新、Alias 更新放在同一事务中。

---

## 11. `blog_comment`

### 11.1 职责

用户对已发布公开文章发表评论。

### 11.2 字段

| 字段 | 类型 | NULL | 默认值 | 说明 |
|---|---|---:|---:|---|
| `comment_id` | BIGINT | 否 | — | 评论 ID |
| `article_id` | BIGINT | 否 | — | 所属文章 ID |
| `user_id` | BIGINT | 否 | — | 评论用户 ID |
| `parent_id` | BIGINT | 是 | NULL | 预留回复父评论；MVP 必须为 NULL |
| `content` | VARCHAR(1000) | 否 | — | 评论内容 |
| `create_dept` | BIGINT | 是 | NULL | 创建部门 |
| `create_by` | BIGINT | 是 | NULL | 创建者 |
| `create_time` | DATETIME | 是 | NULL | 创建时间 |
| `update_by` | BIGINT | 是 | NULL | 统一表结构字段；业务层禁止修改评论 |
| `update_time` | DATETIME | 是 | NULL | 统一表结构字段；业务层禁止修改评论 |
| `del_flag` | CHAR(1) | 否 | `'0'` | 逻辑删除 |

### 11.3 索引

```text
PRIMARY KEY (comment_id)
KEY idx_blog_comment_article (article_id, del_flag, create_time)
KEY idx_blog_comment_user (user_id, del_flag, create_time)
KEY idx_blog_comment_parent (parent_id, del_flag)
```

### 11.4 业务约束

- 发表评论时必须校验文章：`PUBLISHED + PUBLIC + NOT DELETED`。
- MVP `parent_id` 必须 NULL。
- 评论不能修改。
- 管理员删除评论使用逻辑删除。
- 用户不能删除自己的评论。
- 删除文章不级联删除评论；评论数据保留。

---

## 12. 文章版本生命周期

### 12.1 新文章

```text
创建 Article
    ↓
保存草稿
    ↓
Version 1
    ↓
article.current_version_id = 1
status = DRAFT
```

继续修改并保存：

```text
Version 2
Version 3
...
```

### 12.2 首次发布

```text
DRAFT
 ↓ 发布
PUBLISHED + PUBLIC
```

同时设置首次 `published_at`。

### 12.3 已发布文章修改

```text
PUBLISHED v3
    ↓
作者编辑
    ↓
保存
    ↓
创建 v4
    ↓
current_version_id = v4
    ↓
立即公开 v4
```

旧版本 v1/v2/v3 永久保留。

### 12.4 用户隐藏

```text
PUBLISHED + PUBLIC
    ↓
USER_HIDDEN
```

作者可以：

```text
USER_HIDDEN
    ↓
PUBLIC
```

### 12.5 管理员下线

```text
PUBLISHED + PUBLIC
    ↓ 管理员治理
PUBLISHED + ADMIN_HIDDEN
```

MVP：作者不能继续编辑或自行恢复；历史版本继续保留。

### 12.6 用户删除

```text
任意文章业务状态
    ↓
del_flag = '1'
```

不物理删除任何历史版本、标签关系和评论。

---

## 13. 标签生命周期

### 13.1 创建

```text
用户输入 tag name
      ↓
规范化
      ↓
生成 canonical slug
      ↓
查询 active tag / alias
      ↓
已有 → 直接使用
不存在 → 创建新的 ACTIVE tag
```

### 13.2 重命名

例如：

```text
旧：JavaScript / javascript
新：JavaScript / javascript-lang
```

处理：

```text
保留 tag_id
更新 name/slug
创建 alias：
javascript → 当前 tag
```

旧 URL → 301 → 新 URL。

### 13.3 合并

```text
A → B
```

事务内：

```text
1. A/B 必须都是 ACTIVE 且 A != B
2. 迁移 A 的 article_tag 到 B
3. 对重复关系做逻辑删除
4. A 的 alias 直接指向 B
5. A 的当前 `slug/name` 由源 Tag 记录本身继续保留；更早的历史 slug/name 创建 alias → B。
6. A.status = MERGED
7. A.merged_into = B
8. COMMIT
```

### 13.4 链式合并展平

如果：

```text
A → B
```

后来：

```text
B → C
```

第二次事务必须同时把：

```text
A.merged_into = C
B.merged_into = C
```

并把原本指向 B 的历史 alias 全部更新为直接指向 C。

禁止长期保存：

```text
A → B → C
```

### 13.5 普通删除

```text
tag.del_flag = '1'
```

不创建 alias，不自动迁移文章标签关系。

若用户以后输入相同 slug：优先恢复原 Tag。

---

## 14. 公共查询统一约束

所有面向游客/公共页面的文章查询，都必须使用：

```sql
status = '1'
AND visibility = '0'
AND del_flag = '0'
```

涉及公共文章的数据场景包括：

- 首页文章流
- 文章详情
- 标签文章列表
- 作者公开文章列表
- MySQL 搜索
- 热门标签统计
- 文章相关推荐（未来）

热门标签统计必须排除：

```text
DRAFT
USER_HIDDEN
ADMIN_HIDDEN
已逻辑删除文章
```

---

## 15. 数据关系与查询方向

```text
sys_user
   │
   ├──── 1 : 0..1 ─── blog_user_profile
   │
   ├──── 1 : N ────── blog_article
   │                        │
   │                        ├──── 1 : N ─── blog_article_version
   │                        │
   │                        ├──── N : M ─── blog_tag
   │                        │                 │
   │                        │                 └── blog_tag_alias
   │                        │
   │                        └──── 1 : N ─── blog_comment
   │
   └──── 1 : N ────── blog_comment

blog_article N : M blog_tag
      via blog_article_tag
```

不使用 MySQL `FOREIGN KEY`；关系正确性由 Service / Transaction 维护。

---

## 16. 暂不进入数据库的后续扩展

以下功能暂不属于 MVP 表结构：

- 文章点赞
- 收藏
- 关注作者
- 站内通知
- 多级评论正式实现
- 评论自删
- 用户申请作者
- 文章申诉恢复
- 文章版本恢复
- 文章归档
- 定时发布
- Elasticsearch 索引表/同步机制
- 多级缓存
- PWA / SEO 专用数据

其中未来可能新增但不应破坏现有模型的表包括：

```text
blog_article_revision（若未来需要更复杂的版本操作）
blog_notification
blog_user_follow
blog_article_like
blog_article_favorite
```

---

## 17. 实体层映射原则

博客实体应尽量沿用 RuoYi-Vue-Plus 6.X 的既有结构：

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blog_article")
public class BlogArticle extends BaseEntity {

    @TableId(value = "article_id")
    private Long articleId;

    @TableLogic
    private String delFlag;

    // business fields...
}
```

`blog_article_version` 虽然同样继承 `BaseEntity`，但业务层严格禁止其 update/delete；这属于统一表结构与不可变历史数据之间的有意平衡。

---

## 18. 数据库设计的下一阶段

数据库模型确认后，下一步不是立即开始写业务代码，而是：

1. 将本设计转换为 MySQL 建表 SQL。
2. 对照当前 RuoYi-Vue-Plus 6.X 的代码生成器与项目命名规范检查字段、注释、Entity、Mapper、BO/VO。
3. 生成博客模块的 Entity / BO / VO / Mapper / Service / Controller 骨架。
4. 使用 Apifox 验证 API 契约。
5. 再进入 PLUS-UI 博客后台页面实现与前后端联调。
