-- =========================================================
-- S6 菜单脚本：标签治理权限按钮 + 停用 SdBlogTagController 菜单清理
-- 日期：2026-09-30   执行库：ry-vue
-- 依据：docs/blog-api.md §13/§17（blog:tag:动作）、ROADMAP §6 D12（S6 停用 Tag 生成 CRUD）
-- 说明：可安全重复执行（先删后插，按 menu_id / perms 定位）。
--       MVP 无后台标签页面，仅建 F 型按钮权限；超管自动全权，非超管另行授权。
--       公共接口 /blog/public/tags/** 走 security.excludes，无需菜单。
-- =========================================================

-- ---------------------------------------------------------
-- ① 幂等清理本脚本自身管理的 6 个按钮
-- ---------------------------------------------------------
DELETE FROM sys_role_menu WHERE menu_id IN (
  2105090000000000011, 2105090000000000012, 2105090000000000013,
  2105090000000000014, 2105090000000000015, 2105090000000000016);
DELETE FROM sys_menu WHERE menu_id IN (
  2105090000000000011, 2105090000000000012, 2105090000000000013,
  2105090000000000014, 2105090000000000015, 2105090000000000016);

-- ---------------------------------------------------------
-- ② 新增标签治理按钮（parent = 博客目录 2103854919354421249）
-- ---------------------------------------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
VALUES
(2105090000000000011, '标签管理列表', 2103854919354421249, 30, '', NULL, NULL, 1, 0, 'F', '0', '0', 'blog:tag:list',   '#', 103, 1, sysdate(), '契约§13.1'),
(2105090000000000012, '标签管理详情', 2103854919354421249, 31, '', NULL, NULL, 1, 0, 'F', '0', '0', 'blog:tag:query',  '#', 103, 1, sysdate(), '契约§13.2'),
(2105090000000000013, '标签运营更新', 2103854919354421249, 32, '', NULL, NULL, 1, 0, 'F', '0', '0', 'blog:tag:edit',   '#', 103, 1, sysdate(), '契约§13.3 仅精选/排序'),
(2105090000000000014, '标签重命名',   2103854919354421249, 33, '', NULL, NULL, 1, 0, 'F', '0', '0', 'blog:tag:rename', '#', 103, 1, sysdate(), '契约§13.4'),
(2105090000000000015, '标签合并',     2103854919354421249, 34, '', NULL, NULL, 1, 0, 'F', '0', '0', 'blog:tag:merge',  '#', 103, 1, sysdate(), '契约§13.5'),
(2105090000000000016, '标签删除',     2103854919354421249, 35, '', NULL, NULL, 1, 0, 'F', '0', '0', 'blog:tag:remove', '#', 103, 1, sysdate(), '契约§13.6');

-- ---------------------------------------------------------
-- ③ 清理已停用 SdBlogTagController 的菜单（D12：C 菜单 + 5 个 F 按钮）
-- ---------------------------------------------------------
DELETE FROM sys_role_menu WHERE menu_id IN (
  SELECT menu_id FROM (
    SELECT menu_id FROM sys_menu
    WHERE perms LIKE 'blog:blogTag:%'
       OR (component LIKE 'blog/blogTag/%' AND menu_type = 'C')
  ) t
);
DELETE FROM sys_menu WHERE perms LIKE 'blog:blogTag:%';
DELETE FROM sys_menu WHERE component LIKE 'blog/blogTag/%' AND menu_type = 'C';

-- ---------------------------------------------------------
-- ④ 验证
-- ---------------------------------------------------------
-- 4.1 治理按钮应恰好 6 行
SELECT menu_id, perms FROM sys_menu WHERE menu_id BETWEEN 2105090000000000011 AND 2105090000000000016 ORDER BY menu_id;
-- 4.2 blogTag 裸 CRUD 残留应为 0
SELECT COUNT(*) AS should_be_0 FROM sys_menu WHERE perms LIKE 'blog:blogTag:%';
-- 4.3 暂留的 Comment/UserProfile 生成菜单应仍在（S7 处理 Comment）
SELECT menu_id, perms FROM sys_menu WHERE menu_type='C' AND (perms LIKE 'blog:blogComment:%' OR perms LIKE 'blog:blogUserProfile:%');

-- ---------------------------------------------------------
-- ⑤ 执行后操作提醒
-- ---------------------------------------------------------
-- 执行后需重新登录一次以刷新权限集合（菜单/权限存在登录态快照）。
