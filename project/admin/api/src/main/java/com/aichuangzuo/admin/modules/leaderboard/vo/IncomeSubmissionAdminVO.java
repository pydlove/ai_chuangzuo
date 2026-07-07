package com.aichuangzuo.admin.modules.leaderboard.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端收入申报列表项 VO。
 */
@Data
public class IncomeSubmissionAdminVO {

    private Long id;
    private String bizNo;
    private Long userId;
    private String periodMonth;
    private BigDecimal amount;
    private String platform;
    private Integer auditStatus;
    private Long auditedBy;
    private LocalDateTime auditedAt;
    private String rejectReason;
    private LocalDateTime createdAt;
}
