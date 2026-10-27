-- =============================================================================
-- 清理历史测试数据（**手动执行，不可撤销**）
--
-- 用途：把库里 `S3测试` / `32131aaa` / `????111aaa` 这类开发期测试数据清掉，
--       只保留 seed 装载的演示数据（22xx 段）。
--
-- 边界：只删除 **22xx 段之外** 的博客数据。种子数据（2210~2260 段）一条不动；
--       RuoYi 系统表（菜单、字典、角色等）也一条不动。
--
-- ⚠️ 执行前务必备份：这会删掉你现有的全部文章 / 版本 / 评论 / 标签！
--    docker exec blog-mysql mysqldump -uroot -proot ry-vue > backup-$(date +%F).sql
--
-- 分两步，先看再删：
--   1) 执行「步骤 1」预览将被删除的数量与标题，确认无误
--   2) 取消「步骤 2」的注释后整段执行
-- =============================================================================

-- ---------- 步骤 1：预览（只读，可安全执行）----------
SELECT '文章' AS kind, COUNT(*) AS rows_to_delete FROM sd_blog_article WHERE article_id < 2210000000000000000
UNION ALL SELECT '版本', COUNT(*) FROM sd_blog_article_version WHERE version_id < 2210000000000000000
UNION ALL SELECT '评论', COUNT(*) FROM sd_blog_comment WHERE comment_id < 2210000000000000000
UNION ALL SELECT '标签', COUNT(*) FROM sd_blog_tag WHERE tag_id < 2210000000000000000
UNION ALL SELECT '用户', COUNT(*) FROM sys_user WHERE user_id < 2210000000000000000 AND user_id > 2000000000000000000;

-- 将被删除的标题样本（最多 20 条）
SELECT a.article_id, v.title
FROM sd_blog_article a
LEFT JOIN sd_blog_article_version v ON v.version_id = a.current_version_id
WHERE a.article_id < 2210000000000000000
ORDER BY a.article_id DESC
LIMIT 20;

-- ---------- 步骤 2：确认后执行（取消下面所有注释）----------
-- 注意顺序：先子表后父表，避免中间态出现悬挂引用。
--
-- ⚠️ 每张表都按**自身主键**判断，而不是按 article_id 之类的外键列：
--    实测遇到过 article_id 不在 21xx 段、但 version_id 在 21xx 段的孤儿版本
--    （文章早已删除，版本残留），按 article_id 过滤会漏掉它。
--
-- START TRANSACTION;
--
-- DELETE FROM sd_blog_comment
--  WHERE comment_id < 2210000000000000000;
--
-- DELETE FROM sd_blog_article_tag
--  WHERE article_tag_id < 2210000000000000000;
--
-- DELETE FROM sd_blog_article_version
--  WHERE version_id < 2210000000000000000;
--
-- DELETE FROM sd_blog_article
--  WHERE article_id < 2210000000000000000;
--
-- -- 标签：只删没有被种子文章引用的，避免删掉种子关系
-- DELETE FROM sd_blog_tag_alias
--  WHERE tag_alias_id < 2210000000000000000;
--
-- DELETE FROM sd_blog_tag
--  WHERE tag_id < 2210000000000000000
--    AND tag_id NOT IN (SELECT DISTINCT tag_id FROM sd_blog_article_tag);
--
-- -- 用户：只删「没有文章、没有评论」的博客用户（保留 admin 等系统账号）
-- DELETE FROM sd_blog_user_profile
--  WHERE user_id < 2210000000000000000
--    AND user_id > 2000000000000000000
--    AND user_id NOT IN (SELECT DISTINCT author_id FROM sd_blog_article)
--    AND user_id NOT IN (SELECT DISTINCT user_id FROM sd_blog_comment);
--
-- DELETE FROM sys_user_role
--  WHERE user_id < 2210000000000000000
--    AND user_id > 2000000000000000000
--    AND user_id NOT IN (SELECT DISTINCT author_id FROM sd_blog_article)
--    AND user_id NOT IN (SELECT DISTINCT user_id FROM sd_blog_comment);
--
-- DELETE FROM sys_user
--  WHERE user_id < 2210000000000000000
--    AND user_id > 2000000000000000000
--    AND user_id NOT IN (SELECT DISTINCT author_id FROM sd_blog_article)
--    AND user_id NOT IN (SELECT DISTINCT user_id FROM sd_blog_comment);
--
-- COMMIT;
--
-- -- 执行后核对：下面的数字应该等于 seed 的规模
-- SELECT '文章' AS kind, COUNT(*) FROM sd_blog_article
-- UNION ALL SELECT '评论', COUNT(*) FROM sd_blog_comment
-- UNION ALL SELECT '标签', COUNT(*) FROM sd_blog_tag;
