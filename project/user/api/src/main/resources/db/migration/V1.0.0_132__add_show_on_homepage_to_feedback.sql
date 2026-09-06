SET NAMES utf8mb4;

ALTER TABLE u_feedback
    ADD COLUMN is_show_on_homepage TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否展示到首页：0-否，1-是' AFTER status,
    ADD KEY idx_u_feedback_show_homepage (is_show_on_homepage);
