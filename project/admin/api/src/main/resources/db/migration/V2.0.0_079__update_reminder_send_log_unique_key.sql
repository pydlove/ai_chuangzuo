-- 管理端-到期提醒：手动提醒与自动提醒独立去重
-- 原唯一键 (user_id, channel, send_date) 导致手动/自动互斥，
-- 改为 (user_id, channel, send_date, trigger_type)，让手动立即提醒不占用自动任务配额。

ALTER TABLE u_reminder_send_log
    DROP INDEX uk_reminder_user_channel_date,
    ADD UNIQUE KEY uk_reminder_user_channel_date_type (user_id, channel, send_date, trigger_type);
