SET NAMES utf8mb4;

-- 发布计划缓存维度从 (user, platform) 变为 (user, platform, plan_content_hash)。
-- 该表为缓存表，直接清空旧数据，让业务请求时按新维度重建。
DELETE FROM u_self_media_plan_publish_guide;

ALTER TABLE u_self_media_plan_publish_guide
    ADD COLUMN plan_content_hash VARCHAR(64) NOT NULL DEFAULT '' COMMENT '运营方案内容 SHA-256 哈希' AFTER plan_updated_at;

ALTER TABLE u_self_media_plan_publish_guide
    DROP INDEX uk_user_platform,
    ADD UNIQUE KEY uk_user_platform_hash (user_id, main_platform, plan_content_hash);
