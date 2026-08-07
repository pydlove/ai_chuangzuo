package com.aichuangzuo.admin.modules.audit.service;

import com.aichuangzuo.admin.modules.audit.dto.request.AuditConfigRequest;
import com.aichuangzuo.admin.modules.audit.entity.AuditConfig;

public interface AuditConfigService {

    AuditConfig getConfig();

    AuditConfig saveConfig(AuditConfigRequest request, Long updatedBy);
}
