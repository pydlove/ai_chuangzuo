package com.aichuangzuo.admin.modules.reminder.service;

import com.aichuangzuo.admin.modules.reminder.dto.request.ReminderConfigRequest;
import com.aichuangzuo.admin.modules.reminder.entity.ReminderConfig;

public interface ReminderConfigService {
    ReminderConfig getConfig();
    ReminderConfig saveConfig(ReminderConfigRequest request, Long updatedBy);
    void syncFromProperties();
}