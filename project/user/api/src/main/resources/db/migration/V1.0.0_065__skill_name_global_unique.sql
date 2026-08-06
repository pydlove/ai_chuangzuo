SET NAMES utf8mb4;

-- =============================================================
-- 2026-08-05: 用户端「我的提示词」名称改为全局唯一。
-- 1) 删除旧的 (user_id, skill_name) 联合唯一索引；
-- 2) 新增 (skill_name, is_deleted) 联合唯一索引，允许已删除记录与未删除记录同名，
--    避免逻辑删除数据阻塞新用户创建同名提示词；
-- 3) 更新字段注释。
-- =============================================================

ALTER TABLE u_user_skill
    DROP INDEX uk_u_user_skill_user_id_name,
    ADD UNIQUE INDEX uk_u_user_skill_skill_name_deleted (skill_name, is_deleted),
    MODIFY COLUMN skill_name VARCHAR(64) NOT NULL COMMENT '风格名称；全局唯一（按是否删除分区）';
