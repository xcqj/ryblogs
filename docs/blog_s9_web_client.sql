-- ----------------------------------------------------------------------------
-- blog_s9_web_client.sql —— S9 开工准备：sys_client 新增 blog-web 前台客户端记录（D28）
-- 背景：blog-web 登录（评论）需独立 clientid，与 plus-ui 的 pc 记录会话隔离；
--       零代码，纯配置。id 列序对齐 script/sql/ry_vue.sql sys_client 定义。
-- 特性：幂等可重跑（NOT EXISTS 守卫）；执行后必须跑文末验证段。
-- 执行方式：docker exec -i blog-mysql mysql ... ry-vue 库（照 SETUP §3 惯例）
-- 注意：client_secret 'web123' 为开发配置（与 pc123/app123 同级强度，仅限本地），
--       生产环境必须更换。
-- ----------------------------------------------------------------------------

-- 1. 幂等插入 web 客户端
insert into sys_client
select 1762000000000000003,
       'f38076e6bb30a1170b9ea3fbebac0815',
       'web',
       'web123',
       'password,social',
       'pc',
       null,
       null,
       1800,
       604800,
       '0',
       '0',
       1761000000000000103,
       1761100000000000001,
       sysdate(),
       1761100000000000001,
       sysdate()
where not exists(select 1
                 from sys_client
                 where client_key = 'web'
                    or id = 1762000000000000003);

-- 1b. 兼容已有库：S14 曾通过后台 UI 手工把 web 客户端改成 password,social（未入版本库），
--     这里补一条幂等 UPDATE，避免"开发环境能社交登录、新部署不能"的环境漂移
update sys_client
set grant_type = 'password,social'
where client_key = 'web'
  and grant_type not like '%social%';

-- 2. 验证段（人工确认输出：应恰好 1 行，del_flag='0'，status='0'）
select id, client_id, client_key, grant_type, device_type, status, del_flag
from sys_client
where client_key = 'web';

-- 3. blog-web 开发环境 .env 对应值（记入仓库 README/SETUP，不写入代码硬编码之外位置）
-- NUXT_APP_CLIENT_ID = 'f38076e6bb30a1170b9ea3fbebac0815'
