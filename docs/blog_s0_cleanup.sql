-- =========================================================
-- S0 收尾脚本：停用 Controller 菜单清理 + 开放注册开关
-- 日期：2026-09-28   执行库：ry-vue
-- 依据：docs/blog-api.md §14（Version/TagAlias/ArticleTag 不作 CRUD 暴露）
--       docs/ROADMAP.md §6 D9（注册开关）
-- 说明：按 perms / component 定位，不依赖随机 menu_id，可安全重复执行。
-- =========================================================

-- ---------------------------------------------------------
-- ① 清理角色-菜单关联（防止孤儿引用）
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

-- ---------------------------------------------------------
-- ② 删除按钮（F 型菜单）
-- ---------------------------------------------------------
DELETE FROM sys_menu WHERE perms LIKE 'blog:blogArticleVersion:%';
DELETE FROM sys_menu WHERE perms LIKE 'blog:blogTagAlias:%';
DELETE FROM sys_menu WHERE perms LIKE 'blog:blogArticleTag:%';

-- ---------------------------------------------------------
-- ③ 删除菜单本体（C 型，component 兜底防漏）
-- ---------------------------------------------------------
DELETE FROM sys_menu WHERE component LIKE 'blog/blogArticleVersion/%';
DELETE FROM sys_menu WHERE component LIKE 'blog/blogTagAlias/%';
DELETE FROM sys_menu WHERE component LIKE 'blog/blogArticleTag/%';

-- ---------------------------------------------------------
-- ④ 开放用户注册（S1 需要；框架自带开关，AuthController /register 读取）
--    ⚠️ sys_config 有 Redis 缓存：走本 SQL 更新后，必须到后台
--       「系统管理 → 参数设置」点【刷新缓存】，或重启应用，否则不生效！
--    （推荐直接在参数设置界面改值，界面保存会自动刷缓存，见 SETUP.md §5）
-- ---------------------------------------------------------
UPDATE sys_config SET config_value = 'true'
 WHERE config_key = 'sys.account.registerUser';

-- ---------------------------------------------------------
-- ⑤ 验证（两条都应返回 0 / true）
-- ---------------------------------------------------------
SELECT COUNT(*) AS should_be_0 FROM sys_menu
 WHERE perms LIKE 'blog:blogArticleVersion:%'
    OR perms LIKE 'blog:blogTagAlias:%'
    OR perms LIKE 'blog:blogArticleTag:%';

SELECT config_value AS should_be_true FROM sys_config
 WHERE config_key = 'sys.account.registerUser';
