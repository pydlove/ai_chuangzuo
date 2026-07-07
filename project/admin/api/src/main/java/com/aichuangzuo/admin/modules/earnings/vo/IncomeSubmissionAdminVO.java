package com.aichuangzuo.admin.modules.earnings.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class IncomeSubmissionAdminVO {
    private Long id;
    private String bizNo;
    private Long userId;
    private String nickname;
    private String periodMonth;
    private BigDecimal amount;
    private String platform;
    private String screenshotPaths;
    private Integer auditStatus;
    private Long auditedBy;
    private LocalDateTime auditedAt;
    private String rejectReason;
    private LocalDateTime createdAt;
}
