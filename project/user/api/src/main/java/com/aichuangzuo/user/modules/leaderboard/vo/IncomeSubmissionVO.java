package com.aichuangzuo.user.modules.leaderboard.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 收入申报记录 VO。
 */
@Data
public class IncomeSubmissionVO {

    private String bizNo;
    private String periodMonth;
    private BigDecimal amount;
    private String platform;
    private Integer auditStatus;
    private String rejectReason;
    private List<String> screenshotPaths;
    private LocalDateTime createdAt;
}
