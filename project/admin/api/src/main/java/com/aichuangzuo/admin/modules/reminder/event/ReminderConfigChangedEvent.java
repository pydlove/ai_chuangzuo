package com.aichuangzuo.admin.modules.reminder.event;

/**
 * 提醒配置变更事件。
 * 由 ReminderConfigServiceImpl 在 saveConfig 后发布，
 * ExpireReminderJob 监听并 reschedule。
 */
public record ReminderConfigChangedEvent(Long adminId) {
}