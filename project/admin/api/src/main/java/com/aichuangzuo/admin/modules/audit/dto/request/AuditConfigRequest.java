package com.aichuangzuo.admin.modules.audit.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AuditConfigRequest {

    @NotNull
    @Min(1)
    @Max(365)
    private Integer retentionDays;

    @NotNull
    @Pattern(regexp = "^[0-9*?,/\\-\\s]+$", message = "cron 表达式格式不正确")
    private String cleanupCron;
}
