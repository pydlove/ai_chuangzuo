package com.aichuangzuo.admin.modules.audit.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditConfigVO {

    private Long id;
    private Integer retentionDays;
    private String cleanupCron;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}
