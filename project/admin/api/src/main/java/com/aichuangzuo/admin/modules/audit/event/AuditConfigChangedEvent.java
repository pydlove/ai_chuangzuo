package com.aichuangzuo.admin.modules.audit.event;

import lombok.Getter;

@Getter
public class AuditConfigChangedEvent {

    private final Long updatedBy;

    public AuditConfigChangedEvent(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
}
