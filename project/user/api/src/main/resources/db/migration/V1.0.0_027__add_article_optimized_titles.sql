-- 作品表加 AI 优化标题缓存（首次调用大模型生成后持久保存，之后直接返回）
SET NAMES utf8mb4;

ALTER TABLE u_article
    ADD COLUMN optimized_titles_json JSON NULL COMMENT 'AI 优化标题缓存，形如 {"wechat":["标题1","标题2"],...}' AFTER tags_json;
