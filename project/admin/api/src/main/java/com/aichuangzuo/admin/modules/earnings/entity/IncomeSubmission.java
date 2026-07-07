package com.aichuangzuo.admin.modules.earnings.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("u_leaderboard_income_submission")
public class IncomeSubmission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String bizNo;
    private Long userId;
    private String periodMonth;
    private BigDecimal amount;
    private String platform;
    private String screenshotPaths;
    private Integer auditStatus;
    private Long auditedBy;
    private LocalDateTime auditedAt;
    private String rejectReason;

    @TableLogic
    private Integer isDeleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
