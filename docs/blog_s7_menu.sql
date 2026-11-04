-- =========================================================
-- S7 菜单脚本：评论治理权限按钮 + 停用 Comment/UserProfile 生成菜单清理
-- 日期：2026-10-01   执行库：ry-vue
-- 依据：docs/blog-api.md §12/§17（blog:comment:动作）、ROADMAP §6 D12（S7 停用 Comment）、
--       D16（S7 停用 UserProfile）
-- 说明：可安全重复执行（先删后插，按 menu_id / perms 定位）。
--       公共评论接口 /blog/public/articles/{id}/comments 与发表接口 /blog/article/**
--       分别走白名单与登录即可，无需菜单。
-- =========================================================

-- ---------------------------------------------------------
-- ① 幂等清理本脚本自身管理的 2 个按钮
-- ---------------------------------------------------------
DELETE FROM sys_role_menu WHERE menu_id IN (2105090000000000021, 2105090000000000022);
DELETE FROM sys_menu WHERE menu_id IN (2105090000000000021, 2105090000000000022);

-- ---------------------------------------------------------
-- ② 新增评论治理按钮（parent = 博客目录 2103854919354421249）
-- ---------------------------------------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
VALUES
(2105090000000000021, '评论管理列表', 2103854919354421249, 40, '', NULL, NULL, 'N', 0, 'F', '0', '0', 'blog:comment:list',   '#', 103, 1, sysdate(), '契约§12.1'),
(2105090000000000022, '评论删除',     2103854919354421249, 41, '', NULL, NULL, 'N', 0, 'F', '0', '0', 'blog:comment:remove', '#', 103, 1, sysdate(), '契约§12.2');

-- ---------------------------------------------------------
-- ③ 清理已停用生成 Controller 的菜单（D12：blogComment；D16：blogUserProfile）
-- ---------------------------------------------------------
DELETE FROM sys_role_menu WHERE menu_id IN (
  SELECT menu_id FROM (
    SELECT menu_id FROM sys_menu
    WHERE perms LIKE 'blog:blogComment:%'
       OR perms LIKE 'blog:blogUserProfile:%'
       OR (component LIKE 'blog/blogComment/%' AND menu_type = 'C')
       OR (component LIKE 'blog/blogUserProfile/%' AND menu_type = 'C')
  ) t
);
DELETE FROM sys_menu WHERE perms LIKE 'blog:blogComment:%' OR perms LIKE 'blog:blogUserProfile:%';
DELETE FROM sys_menu WHERE (component LIKE 'blog/blogComment/%' OR component LIKE 'blog/blogUserProfile/%') AND menu_type = 'C';

-- ---------------------------------------------------------
-- ④ 验证
-- ---------------------------------------------------------
-- 4.1 治理按钮应恰好 2 行
SELECT menu_id, perms FROM sys_menu WHERE menu_id IN (2105090000000000021, 2105090000000000022);
-- 4.2 全部生成裸 CRUD 菜单残留应为 0（S5~S7 清理后博客模块不应再有任何 blog:*:* 生成权限）
SELECT COUNT(*) AS should_be_0 FROM sys_menu
 WHERE perms LIKE 'blog:blog%';
-- 4.3 契约治理按钮合计应为 12（文章4 + 标签6 + 评论2）
SELECT COUNT(*) AS should_be_12 FROM sys_menu WHERE perms LIKE 'blog:%' AND menu_type = 'F';

-- ---------------------------------------------------------
-- ⑤ 执行后操作提醒
-- ---------------------------------------------------------
-- 执行后需重新登录一次以刷新权限集合。
