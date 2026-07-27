-- 首页 Banner 丰富展示字段
SET NAMES utf8mb4;

ALTER TABLE a_home_banner
    ADD COLUMN title VARCHAR(128) NOT NULL DEFAULT '' COMMENT '卡片标题',
    ADD COLUMN subtitle VARCHAR(256) NOT NULL DEFAULT '' COMMENT '卡片副标题',
    ADD COLUMN cta_text VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CTA 按钮文字',
    ADD COLUMN theme VARCHAR(16) NOT NULL DEFAULT 'light' COMMENT '主题: light/dark/auto',
    ADD COLUMN status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态: 0 禁用 / 1 启用',
    ADD COLUMN start_time DATETIME(3) NULL COMMENT '上线时间, NULL=立即上线',
    ADD COLUMN end_time DATETIME(3) NULL COMMENT '下线时间, NULL=永久',
    ADD INDEX idx_a_home_banner_status_sort (status, sort, id);
