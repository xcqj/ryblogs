-- =========================================================
-- S2 结构迁移：sd_blog_article_tag 转纯关系表（物理删除）
-- 日期：2026-09-28   执行库：ry-vue   依据：ROADMAP §6 S2 决议
-- 背景：关系表软删除会因唯一键 (article_id, tag_id, del_flag) 导致
--       同一关系二次删除撞键，且"恢复式重加"复杂无益；
--       对齐框架 sys_user_role 惯例改为物理删除。
-- 注意：本表当前应为空表（S2 前无正式数据）；若非空请先人工确认再执行。
-- =========================================================

-- 执行前核验（应为 0）
SELECT COUNT(*) AS should_be_0 FROM sd_blog_article_tag;

ALTER TABLE sd_blog_article_tag
    DROP COLUMN del_flag,
    DROP INDEX uq_blog_article_tag,
    ADD UNIQUE KEY uq_blog_article_tag (article_id, tag_id),
    DROP INDEX idx_blog_article_tag_tag_article,
    DROP INDEX idx_blog_article_tag_article,
    ADD KEY idx_blog_article_tag_tag (tag_id),
    COMMENT = '博客文章标签关系表（纯关系表，物理删除，无del_flag）';

-- 执行后核验：应无 del_flag 列，唯一键为 (article_id, tag_id)
SHOW CREATE TABLE sd_blog_article_tag;
