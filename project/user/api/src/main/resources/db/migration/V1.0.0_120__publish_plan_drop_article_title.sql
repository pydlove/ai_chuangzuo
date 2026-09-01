SET NAMES utf8mb4;

-- 发布计划改为只依赖自媒体方案，不再按文章标题缓存。
-- 1. 删除历史缓存（主键维度变更后旧数据无法复用）。
DELETE FROM u_self_media_plan_publish_guide;

-- 2. 删除 article_title 字段。
ALTER TABLE u_self_media_plan_publish_guide DROP COLUMN article_title;

-- 3. 重建唯一索引：同一用户 + 同一主发平台只保留一份缓存。
ALTER TABLE u_self_media_plan_publish_guide
    DROP INDEX uk_user_title_platform,
    ADD UNIQUE KEY uk_user_platform (user_id, main_platform);
