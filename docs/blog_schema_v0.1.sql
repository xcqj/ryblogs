-- =========================================================
-- 多用户博客平台数据库结构 v0.1
-- 基于 RuoYi-Vue-Plus 6.X / MyBatis-Plus / MySQL 8.4
-- 说明：
-- 1. 主键均由应用侧生成，不使用 AUTO_INCREMENT。
-- 2. 不建立 MySQL FOREIGN KEY，实体关系由应用层维护。
-- 3. 所有博客表统一保留 RuoYi BaseEntity 对应的基础审计字段与 del_flag。
-- 4. article_version 业务上为不可变历史快照，只允许 INSERT。
-- =========================================================

-- ----------------------------
-- 1、博客用户资料表
-- ----------------------------
create table sd_blog_user_profile
(
    user_id        bigint(20)     not null                   comment '用户ID，RuoYi sys_user.user_id，同时作为主键',
    bio            varchar(500)   default null               comment '个人简介',
    create_dept    bigint(20)     default null               comment '创建部门',
    create_by      bigint(20)     default null               comment '创建者',
    create_time    datetime                                  comment '创建时间',
    update_by      bigint(20)     default null               comment '更新者',
    update_time    datetime                                  comment '更新时间',
    del_flag       char(1)        not null default '0'       comment '删除标志（0代表存在 1代表删除）',
    primary key (user_id)
) engine=innodb comment = '博客用户资料表';

-- ----------------------------
-- 2、博客文章主表
-- ----------------------------
create table sd_blog_article
(
    article_id              bigint(20)      not null                   comment '文章ID',
    author_id               bigint(20)      not null                   comment '作者用户ID',
    slug                    varchar(200)    default null               comment '文章URL标识，MVP预留，全局唯一',
    status                  char(1)         not null default '0'        comment '文章状态（0草稿 1已发布）',
    visibility              char(1)         not null default '0'        comment '可见性（0公开 1用户隐藏 2管理员下线）',
    current_version_id      bigint(20)      default null               comment '当前生效版本ID',
    view_count              bigint(20)      not null default 0          comment '阅读量',
    published_at            datetime        default null               comment '首次发布时间',
    admin_hidden_reason     varchar(500)    default null               comment '管理员下线原因',
    admin_hidden_by         bigint(20)      default null               comment '执行管理员下线的用户ID',
    admin_hidden_time       datetime        default null               comment '管理员下线时间',
    create_dept             bigint(20)      default null               comment '创建部门',
    create_by               bigint(20)      default null               comment '创建者',
    create_time             datetime                                   comment '创建时间',
    update_by               bigint(20)      default null               comment '更新者',
    update_time             datetime                                   comment '更新时间',
    del_flag                char(1)         not null default '0'        comment '删除标志（0代表存在 1代表删除）',
    primary key (article_id),
    unique key uq_blog_article_slug (slug),
    key idx_blog_article_author_status (author_id, status, visibility, del_flag, update_time),
    key idx_blog_article_public (status, visibility, del_flag, published_at)
) engine=innodb comment = '博客文章表';

-- ----------------------------
-- 3、博客文章版本历史表
-- ----------------------------
create table sd_blog_article_version
(
    version_id          bigint(20)      not null                   comment '文章版本ID',
    article_id          bigint(20)      not null                   comment '所属文章ID',
    version_no          int(4)          not null                   comment '文章版本号，从1开始递增',
    title               varchar(200)    not null                   comment '文章标题快照',
    summary             varchar(500)    default null               comment '文章摘要快照',
    cover_url           varchar(500)    default null               comment '文章封面URL快照',
    content             longtext        not null                   comment '文章正文HTML快照（WangEditor内容，经安全处理后保存）',
    create_dept         bigint(20)      default null               comment '创建部门',
    create_by           bigint(20)      default null               comment '该版本创建者',
    create_time         datetime                                   comment '版本创建时间',
    update_by           bigint(20)      default null               comment '统一审计字段，业务层禁止更新',
    update_time         datetime                                   comment '统一审计字段，业务层禁止更新',
    del_flag            char(1)         not null default '0'        comment '删除标志（0代表存在 1代表删除），业务层禁止删除版本',
    primary key (version_id),
    unique key uq_blog_article_version_no (article_id, version_no),
    key idx_blog_article_version_article (article_id, version_no)
) engine=innodb comment = '博客文章版本历史表';

-- ----------------------------
-- 4、博客标签表
-- ----------------------------
create table sd_blog_tag
(
    tag_id              bigint(20)      not null                   comment '标签ID',
    name                varchar(50)     not null                   comment '当前标签名称',
    slug                varchar(100)    not null                   comment '当前规范化标签slug，全局唯一',
    status              char(1)         not null default '0'        comment '标签状态（0正常 1已合并）',
    merged_into         bigint(20)      default null               comment '合并目标标签ID，正常状态时为空',
    is_featured         char(1)         not null default '0'        comment '是否精选（0否 1是）',
    sort_order          int(4)          not null default 0          comment '精选标签展示顺序',
    create_dept         bigint(20)      default null               comment '创建部门',
    create_by           bigint(20)      default null               comment '创建者',
    create_time         datetime                                   comment '创建时间',
    update_by           bigint(20)      default null               comment '更新者',
    update_time          datetime                                   comment '更新时间',
    del_flag             char(1)         not null default '0'       comment '删除标志（0代表存在 1代表删除）',
    primary key (tag_id),
    unique key uq_blog_tag_slug (slug),
    key idx_blog_tag_status (status, del_flag),
    key idx_blog_tag_featured (is_featured, sort_order, del_flag),
    key idx_blog_tag_merged_into (merged_into)
) engine=innodb comment = '博客标签表';

-- ----------------------------
-- 5、博客标签别名表
-- ----------------------------
create table sd_blog_tag_alias
(
    tag_alias_id        bigint(20)      not null                   comment '标签别名ID',
    tag_id              bigint(20)      not null                   comment '当前有效canonical标签ID',
    alias_slug          varchar(100)    not null                   comment '历史标签slug，全局唯一',
    alias_name          varchar(50)     not null                   comment '历史标签名称',
    create_dept         bigint(20)      default null               comment '创建部门',
    create_by           bigint(20)      default null               comment '创建者',
    create_time         datetime                                   comment '创建时间',
    update_by           bigint(20)      default null               comment '更新者',
    update_time          datetime                                   comment '更新时间',
    del_flag             char(1)         not null default '0'       comment '删除标志（0代表存在 1代表删除）',
    primary key (tag_alias_id),
    unique key uq_blog_tag_alias_slug (alias_slug),
    key idx_blog_tag_alias_tag_id (tag_id, del_flag)
) engine=innodb comment = '博客标签历史别名表';

-- ----------------------------
-- 6、博客文章标签关系表
-- ----------------------------
create table sd_blog_article_tag
(
    article_tag_id     bigint(20)      not null                   comment '文章标签关系ID',
    article_id         bigint(20)      not null                   comment '文章ID',
    tag_id             bigint(20)      not null                   comment '标签ID',
    create_dept        bigint(20)      default null               comment '创建部门',
    create_by          bigint(20)      default null               comment '创建者',
    create_time        datetime                                   comment '创建时间',
    update_by          bigint(20)      default null               comment '更新者',
    update_time         datetime                                   comment '更新时间',
    del_flag             char(1)         not null default '0'      comment '删除标志（0代表存在 1代表删除）',
    primary key (article_tag_id),
    unique key uq_blog_article_tag (article_id, tag_id, del_flag),
    key idx_blog_article_tag_tag_article (tag_id, article_id, del_flag),
    key idx_blog_article_tag_article (article_id, tag_id, del_flag)
) engine=innodb comment = '博客文章标签关系表';

-- ----------------------------
-- 7、博客评论表
-- ----------------------------
create table sd_blog_comment
(
    comment_id          bigint(20)      not null                   comment '评论ID',
    article_id          bigint(20)      not null                   comment '文章ID',
    user_id             bigint(20)      not null                   comment '评论用户ID',
    parent_id           bigint(20)      default null               comment '父评论ID，MVP必须为空，预留未来回复功能',
    content             varchar(1000)   not null                   comment '评论内容',
    create_dept         bigint(20)      default null               comment '创建部门',
    create_by           bigint(20)      default null               comment '创建者',
    create_time         datetime                                   comment '创建时间',
    update_by           bigint(20)      default null               comment '统一审计字段，业务层禁止修改评论',
    update_time          datetime                                   comment '统一审计字段，业务层禁止修改评论',
    del_flag             char(1)         not null default '0'       comment '删除标志（0代表存在 1代表删除）',
    primary key (comment_id),
    key idx_blog_comment_article (article_id, del_flag, create_time),
    key idx_blog_comment_user (user_id, del_flag, create_time),
    key idx_blog_comment_parent (parent_id, del_flag)
) engine=innodb comment = '博客评论表';
