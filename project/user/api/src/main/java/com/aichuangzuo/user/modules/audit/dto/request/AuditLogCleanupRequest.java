package com.aichuangzuo.user.modules.audit.dto.request;

import lombok.Data;

@Data
public class AuditLogCleanupRequest {

    private Integer retentionDays;
}
