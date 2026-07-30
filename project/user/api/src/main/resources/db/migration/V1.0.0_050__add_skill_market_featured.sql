SET NAMES utf8mb4;

ALTER TABLE u_skill_market
    ADD COLUMN featured TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否官方精选：0-否，1-是' AFTER enable_status;
