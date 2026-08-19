SET NAMES utf8mb4;

ALTER TABLE u_self_media_plan_question
    ADD COLUMN created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID' AFTER updated_at,
    ADD COLUMN updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID' AFTER created_by;

ALTER TABLE u_self_media_plan_niche
    ADD COLUMN created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID' AFTER updated_at,
    ADD COLUMN updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID' AFTER created_by;

ALTER TABLE u_self_media_plan_persona
    ADD COLUMN created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID' AFTER updated_at,
    ADD COLUMN updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID' AFTER created_by;
