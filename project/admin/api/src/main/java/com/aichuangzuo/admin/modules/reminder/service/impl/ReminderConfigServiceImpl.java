package com.aichuangzuo.admin.modules.reminder.service.impl;

import com.aichuangzuo.admin.modules.reminder.dto.request.ReminderConfigRequest;
import com.aichuangzuo.admin.modules.reminder.entity.ReminderConfig;
import com.aichuangzuo.admin.modules.reminder.enums.AdminReminderErrorCode;
import com.aichuangzuo.admin.modules.reminder.event.ReminderConfigChangedEvent;
import com.aichuangzuo.admin.modules.reminder.mapper.ReminderConfigMapper;
import com.aichuangzuo.admin.modules.reminder.properties.ReminderProperties;
import com.aichuangzuo.admin.modules.reminder.service.ReminderConfigService;
import com.aichuangzuo.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderConfigServiceImpl implements ReminderConfigService {

    private static final long CONFIG_ID = 1L;

    private final ReminderConfigMapper configMapper;
    private final ReminderProperties properties;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public ReminderConfig getConfig() {
        ReminderConfig cfg = configMapper.selectById(CONFIG_ID);
        if (cfg == null) {
            throw new BusinessException(AdminReminderErrorCode.CONFIG_NOT_FOUND);
        }
        return cfg;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReminderConfig saveConfig(ReminderConfigRequest req, Long updatedBy) {
        validate(req);

        ReminderConfig existing = configMapper.selectById(CONFIG_ID);
        ReminderConfig entity = existing == null ? new ReminderConfig() : existing;
        entity.setId(CONFIG_ID);
        entity.setAdvanceDays(req.getAdvanceDays());
        entity.setNotifyHour(req.getNotifyHour());
        entity.setNotifyChannel(req.getNotifyChannel());
        entity.setEnabled(req.getEnabled());
        entity.setUpdatedBy(updatedBy == null ? 0L : updatedBy);

        if (existing == null) {
            entity.setCreatedAt(LocalDateTime.now());
            configMapper.insert(entity);
        } else {
            configMapper.updateById(entity);
        }

        eventPublisher.publishEvent(new ReminderConfigChangedEvent(updatedBy));
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncFromProperties() {
        ReminderConfig existing = configMapper.selectById(CONFIG_ID);
        if (existing != null) {
            return;
        }
        ReminderConfig entity = new ReminderConfig();
        entity.setId(CONFIG_ID);
        entity.setAdvanceDays(properties.getAdvanceDays());
        entity.setNotifyHour(properties.getNotifyHour());
        entity.setNotifyChannel(properties.getNotifyChannel());
        entity.setEnabled(properties.isEnabled() ? 1 : 0);
        entity.setUpdatedBy(0L);
        configMapper.insert(entity);
        log.info("到期提醒配置默认值已写入 DB");
    }

    private void validate(ReminderConfigRequest req) {
        if (req.getAdvanceDays() == null || req.getAdvanceDays() < 1 || req.getAdvanceDays() > 90) {
            throw new BusinessException(AdminReminderErrorCode.INVALID_ADVANCE_DAYS);
        }
        if (req.getNotifyHour() == null || req.getNotifyHour() < 0 || req.getNotifyHour() > 23) {
            throw new BusinessException(AdminReminderErrorCode.INVALID_NOTIFY_HOUR);
        }
        // notifyChannel 已用 @Pattern 校验
    }
}