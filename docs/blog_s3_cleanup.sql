-- =========================================================
-- S3 收尾脚本：清除 S2 期间 Apifox 冒烟造的测试文章
-- 日期：2026-09-30   执行库：ry-vue   依据：ROADMAP §3 S3（Q4 决议）
-- 背景：S2 期间正文未净化原样入库（裸 HTML 测试数据），S3 上线
--       HtmlSanitizer 后新数据已净化，存量测试数据按决议直接删除。
-- ⚠️ 开发/测试库专用；执行前必须先跑核验段确认目标数据，人工确认后再放开 DELETE。
-- 定位方式：按已知 article_id + 标题前缀 'S2 ' 双重限定，不误伤正式数据。
-- =========================================================

-- ---------------------------------------------------------
-- ① 执行前核验（应恰好命中 3 篇 S2 冒烟数据）
-- ---------------------------------------------------------
SELECT a.article_id, a.author_id, a.status, v.title
  FROM sd_blog_article a
  LEFT JOIN sd_blog_article_version v ON v.version_id = a.current_version_id
 WHERE a.article_id IN (2104651812388716546, 2104658141010444289, 2104658439179321346)
    OR v.title LIKE 'S2 %';

-- ---------------------------------------------------------
-- ② 删除（确认①结果后人工放开执行；append-only 版本表在此为测试数据清理，
--    属人工运维操作，不违反业务侧"禁止 UPDATE/DELETE"约束）
-- ---------------------------------------------------------
DELETE FROM sd_blog_comment
 WHERE article_id IN (2104651812388716546, 2104658141010444289, 2104658439179321346);

DELETE FROM sd_blog_article_tag
 WHERE article_id IN (2104651812388716546, 2104658141010444289, 2104658439179321346);

DELETE FROM sd_blog_article_version
 WHERE article_id IN (2104651812388716546, 2104658141010444289, 2104658439179321346);

DELETE FROM sd_blog_article
 WHERE article_id IN (2104651812388716546, 2104658141010444289, 2104658439179321346);

-- ---------------------------------------------------------
-- ③ 执行后核验（四张表按上述 ID 计数应全部为 0）
-- ---------------------------------------------------------
SELECT
  (SELECT COUNT(*) FROM sd_blog_article        WHERE article_id IN (2104651812388716546, 2104658141010444289, 2104658439179321346)) AS article_left,
  (SELECT COUNT(*) FROM sd_blog_article_version WHERE article_id IN (2104651812388716546, 2104658141010444289, 2104658439179321346)) AS version_left,
  (SELECT COUNT(*) FROM sd_blog_article_tag     WHERE article_id IN (2104651812388716546, 2104658141010444289, 2104658439179321346)) AS tag_rel_left,
  (SELECT COUNT(*) FROM sd_blog_comment         WHERE article_id IN (2104651812388716546, 2104658141010444289, 2104658439179321346)) AS comment_left;

-- 附注：S2 测试自动创建的 sd_blog_tag 记录保留不删——S6 有"同 slug 恢复原记录"
-- 语义，删了反而丢历史；正式治理入口在 S6 标签管理。
