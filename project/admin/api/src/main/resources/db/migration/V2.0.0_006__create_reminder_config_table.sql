-- 管理端-到期提醒：单行配置表（id=1）
-- 提前天数 N、每天提醒时间点 0-23、通知形式 message/email/message_email、开关。

CREATE TABLE IF NOT EXISTS reminder_config (
    id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    advance_days INT NOT NULL DEFAULT 7 COMMENT '提前提醒天数 N',
    notify_hour TINYINT UNSIGNED NOT NULL DEFAULT 9 COMMENT '每天提醒时间点 0-23',
    notify_channel VARCHAR(16) NOT NULL DEFAULT 'message' COMMENT 'message/email/message_email',
    enabled TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '定时提醒开关',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='到期提醒配置';

INSERT IGNORE INTO reminder_config (id, advance_days, notify_hour, notify_channel, enabled)
VALUES (1, 7, 9, 'message', 1);