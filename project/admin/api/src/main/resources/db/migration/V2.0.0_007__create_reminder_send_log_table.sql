-- 管理端-到期提醒：发送记录 + 去重
-- 唯一键 (user_id, channel, send_date) 保证同日同渠道只成功发一次。

CREATE TABLE IF NOT EXISTS u_reminder_send_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL COMMENT '被提醒用户',
    channel VARCHAR(16) NOT NULL COMMENT 'message/email',
    send_date DATE NOT NULL COMMENT '发送日期（本地日）',
    remaining_days INT NOT NULL COMMENT '发送时剩余天数',
    trigger_type VARCHAR(8) NOT NULL COMMENT 'auto/manual',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1-成功 0-失败',
    fail_reason VARCHAR(256) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_reminder_user_channel_date (user_id, channel, send_date),
    KEY idx_reminder_send_date (send_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='到期提醒发送记录';