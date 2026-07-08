SET NAMES utf8mb4;

ALTER TABLE u_user
    ADD COLUMN user_type TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '用户类型：0-机器人，1-真实用户' AFTER user_status,
    ADD KEY idx_u_user_type (user_type);
