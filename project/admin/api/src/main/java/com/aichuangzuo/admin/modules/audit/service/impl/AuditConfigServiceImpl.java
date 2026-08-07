package com.aichuangzuo.admin.modules.audit.service.impl;

import com.aichuangzuo.admin.modules.audit.dto.request.AuditConfigRequest;
import com.aichuangzuo.admin.modules.audit.entity.AuditConfig;
import com.aichuangzuo.admin.modules.audit.event.AuditConfigChangedEvent;
import com.aichuangzuo.admin.modules.audit.mapper.AuditConfigMapper;
import com.aichuangzuo.admin.modules.audit.service.AuditConfigService;
import com.aichuangzuo.shared.enums.error.AdminAuditErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditConfigServiceImpl implements AuditConfigService {

    private static final long CONFIG_ID = 1L;

    private final AuditConfigMapper auditConfigMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public AuditConfig getConfig() {
        AuditConfig config = auditConfigMapper.selectById(CONFIG_ID);
        if (config == null) {
            throw new BusinessException(AdminAuditErrorCode.CONFIG_NOT_FOUND);
        }
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuditConfig saveConfig(AuditConfigRequest request, Long updatedBy) {
        AuditConfig existing = auditConfigMapper.selectById(CONFIG_ID);
        AuditConfig entity = existing == null ? new AuditConfig() : existing;
        entity.setId(CONFIG_ID);
        entity.setRetentionDays(request.getRetentionDays());
        entity.setCleanupCron(request.getCleanupCron());
        entity.setUpdatedBy(updatedBy == null ? 0L : updatedBy);

        if (existing == null) {
            entity.setCreatedAt(LocalDateTime.now());
            auditConfigMapper.insert(entity);
        } else {
            auditConfigMapper.updateById(entity);
        }

        eventPublisher.publishEvent(new AuditConfigChangedEvent(updatedBy));
        return entity;
    }
}
