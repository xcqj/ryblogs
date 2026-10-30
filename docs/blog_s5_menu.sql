-- =========================================================
-- S5 菜单脚本：管理员文章治理权限按钮 + 停用 Controller 菜单清理
-- 日期：2026-09-30   执行库：ry-vue
-- 依据：docs/blog-api.md §11/§17（blog:资源:动作）、ROADMAP §6 D12（SdBlogArticleController 停用）
--       与 S0 延后事项（blog_s0_cleanup.sql 转 S5 执行）
-- 说明：可安全重复执行（先删后插，按 menu_id / perms 定位，不依赖随机 ID）。
--       MVP 无后台管理页面，仅建 F 型按钮权限；S9/前端接入时再补 C 型菜单页面。
--       超管角色自动拥有全部权限，无需 sys_role_menu；如给非超管角色授权再单独关联。
-- =========================================================

-- ---------------------------------------------------------
-- ⓪ 前置：确保「博客管理」父目录存在（S2 起所有博客菜单的 parent = 2103854919354421249）
--    说明：旧环境里这条目录是早先手工/后台建出来的，从未进脚本；新库缺它会让
--          所有 C/F 菜单变成孤儿节点 → 后台侧边栏不显示「博客管理」，治理页进不去。
-- ---------------------------------------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
SELECT 2103854919354421249, '博客管理', 0, 3, 'blog', NULL, NULL, 1, 0, 'M', '0', '0', '', 'documentation', 103, 1, sysdate(), '博客菜单父目录（治理页入口的容器）'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2103854919354421249);

-- ---------------------------------------------------------
-- ① 幂等清理本脚本自身管理的 4 个按钮（重跑脚本时先删再插）
-- ---------------------------------------------------------
DELETE FROM sys_role_menu WHERE menu_id IN (2105090000000000001, 2105090000000000002, 2105090000000000003, 2105090000000000004);
DELETE FROM sys_menu WHERE menu_id IN (2105090000000000001, 2105090000000000002, 2105090000000000003, 2105090000000000004);

-- ---------------------------------------------------------
-- ② 新增管理员文章治理按钮（parent = 博客目录 2103854919354421249）
-- ---------------------------------------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
VALUES
(2105090000000000001, '文章治理查询', 2103854919354421249, 20, '', NULL, NULL, 1, 0, 'F', '0', '0', 'blog:article:query',    '#', 103, 1, sysdate(), '契约§11.1/11.2 管理员列表与详情'),
(2105090000000000002, '文章下线',     2103854919354421249, 21, '', NULL, NULL, 1, 0, 'F', '0', '0', 'blog:article:adminHide',  '#', 103, 1, sysdate(), '契约§11.3 管理员下线'),
(2105090000000000003, '文章恢复',     2103854919354421249, 22, '', NULL, NULL, 1, 0, 'F', '0', '0', 'blog:article:adminRestore','#', 103, 1, sysdate(), '契约§11.4 管理员恢复'),
(2105090000000000004, '文章治理列表', 2103854919354421249, 23, '', NULL, NULL, 1, 0, 'F', '0', '0', 'blog:article:list',       '#', 103, 1, sysdate(), '契约§11.1 管理员文章列表');

-- ---------------------------------------------------------
-- ③ 清理已停用 SdBlogArticleController 的菜单（D12：C 菜单 + 5 个 F 按钮）
--    ⚠️ 其前端页面 blog/blogArticle/index 是生成器裸 CRUD，路由一并移除
-- ---------------------------------------------------------
DELETE FROM sys_role_menu WHERE menu_id IN (
  SELECT menu_id FROM (
    SELECT menu_id FROM sys_menu
    WHERE perms LIKE 'blog:blogArticle:%'
       OR (component LIKE 'blog/blogArticle/%' AND menu_type = 'C')
  ) t
);
DELETE FROM sys_menu WHERE perms LIKE 'blog:blogArticle:%';
DELETE FROM sys_menu WHERE component LIKE 'blog/blogArticle/%' AND menu_type = 'C';

-- ---------------------------------------------------------
-- ④ S0 延后事项：清理其余 3 组停用 Controller 残留菜单
--    （等价于执行 docs/blog_s0_cleanup.sql 的 ①②③ 段；若已单独执行过则本段空跑无害）
-- ---------------------------------------------------------
DELETE FROM sys_role_menu WHERE menu_id IN (
  SELECT menu_id FROM (
    SELECT menu_id FROM sys_menu
    WHERE perms LIKE 'blog:blogArticleVersion:%'
       OR perms LIKE 'blog:blogTagAlias:%'
       OR perms LIKE 'blog:blogArticleTag:%'
       OR component LIKE 'blog/blogArticleVersion/%'
       OR component LIKE 'blog/blogTagAlias/%'
       OR component LIKE 'blog/blogArticleTag/%'
  ) t
);
DELETE FROM sys_menu WHERE perms LIKE 'blog:blogArticleVersion:%' OR perms LIKE 'blog:blogTagAlias:%' OR perms LIKE 'blog:blogArticleTag:%';
DELETE FROM sys_menu WHERE component LIKE 'blog/blogArticleVersion/%' OR component LIKE 'blog/blogTagAlias/%' OR component LIKE 'blog/blogArticleTag/%';

-- ---------------------------------------------------------
-- ⑤ 验证
-- ---------------------------------------------------------
-- 5.1 治理按钮应恰好 4 行
SELECT menu_id, perms FROM sys_menu WHERE menu_id BETWEEN 2105090000000000001 AND 2105090000000000004 ORDER BY menu_id;
-- 5.2 blogArticle 及 3 组停用菜单残留应为 0
SELECT COUNT(*) AS should_be_0 FROM sys_menu
 WHERE perms LIKE 'blog:blogArticle:%'
    OR perms LIKE 'blog:blogArticleVersion:%'
    OR perms LIKE 'blog:blogTagAlias:%'
    OR perms LIKE 'blog:blogArticleTag:%';
-- 5.3 暂留的 Tag/Comment/UserProfile 生成菜单应仍在（S6/S7 处理）
SELECT menu_id, perms FROM sys_menu WHERE menu_type='C' AND (perms LIKE 'blog:blogTag:%' OR perms LIKE 'blog:blogComment:%' OR perms LIKE 'blog:blogUserProfile:%');

-- ---------------------------------------------------------
-- ⑥ 执行后操作提醒
-- ---------------------------------------------------------
-- 菜单与权限存在登录态快照：执行后至少重新登录一次以刷新权限集合，
-- 否则新按钮权限不生效（给非超管角色授权后同理）。
