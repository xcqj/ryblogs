-- =========================================================
-- S16 演示数据清理脚本（配对 docs/blog_s16_demo_data.sql，人工确认后执行，同 S3-D3 模式）
--
-- 清理范围（与 demo 脚本登记的 ID 段一一对应）：
--   sd_blog_article_tag   : article_id ∈ [9000000000000000001, 9000000000000020024]
--   sd_blog_article_version: version_id ∈ [9100000000000000001, 9100000000000020024]
--   sd_blog_article       : article_id ∈ [9000000000000000001, 9000000000000020024]
--   sd_blog_tag           : tag_id    ∈ [9200000000000000001, 9200000000000000030]
--
-- ES 文档清理（二选一，推荐 ①）：
--   ① 什么都不做——MySQL 删完后，对账定时任务（blog.index.reconcile-cron，默认 30min）
--      会发现「ES 有、MySQL 无」的差异并自动删文档（本切片设计的兜底路径，顺带完成验收演示）；
--   ② 手动立即清（等不及对账周期时）：
--      curl -s -X POST 'localhost:9200/blog_article/_delete_by_query?refresh=true' \
--        -H 'Content-Type: application/json' \
--        -d '{"query":{"range":{"articleId":{"gte":9000000000000000001,"lte":9000000000000020024}}}}'
--
-- 执行：docker exec -i blog-mysql mysql -uroot -proot ry-vue < docs/blog_s16_cleanup.sql
-- =========================================================

SET NAMES utf8mb4;

-- ---------- 0. 执行前核验（人工比对预期数量后确认继续） ----------
SELECT COUNT(*) AS articles_to_delete FROM sd_blog_article
WHERE article_id BETWEEN 9000000000000000001 AND 9000000000000020024;
SELECT COUNT(*) AS versions_to_delete FROM sd_blog_article_version
WHERE version_id BETWEEN 9100000000000000001 AND 9100000000000020024;
SELECT COUNT(*) AS tags_to_delete FROM sd_blog_tag
WHERE tag_id BETWEEN 9200000000000000001 AND 9200000000000000030;
SELECT COUNT(*) AS relations_to_delete FROM sd_blog_article_tag
WHERE article_id BETWEEN 9000000000000000001 AND 9000000000000020024;

-- ---------- 1. 删除（顺序：关系 → 版本 → 主表 → 标签） ----------
DELETE FROM sd_blog_article_tag
WHERE article_id BETWEEN 9000000000000000001 AND 9000000000000020024;

DELETE FROM sd_blog_article_version
WHERE version_id BETWEEN 9100000000000000001 AND 9100000000000020024;

DELETE FROM sd_blog_article
WHERE article_id BETWEEN 9000000000000000001 AND 9000000000000020024;

DELETE FROM sd_blog_tag
WHERE tag_id BETWEEN 9200000000000000001 AND 9200000000000000030;

-- ---------- 2. 执行后核验（期望全部为 0） ----------
SELECT COUNT(*) AS residual_articles FROM sd_blog_article
WHERE article_id BETWEEN 9000000000000000001 AND 9000000000000020024
   OR slug LIKE 's16-%';
SELECT COUNT(*) AS residual_tags FROM sd_blog_tag
WHERE tag_id BETWEEN 9200000000000000001 AND 9200000000000000030
   OR slug LIKE 's16-%';
SELECT COUNT(*) AS residual_versions FROM sd_blog_article_version
WHERE version_id BETWEEN 9100000000000000001 AND 9100000000000020024;
SELECT COUNT(*) AS residual_relations FROM sd_blog_article_tag
WHERE article_id BETWEEN 9000000000000000001 AND 9000000000000020024;
