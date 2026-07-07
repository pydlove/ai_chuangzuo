package com.aichuangzuo.admin.modules.earnings.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RewardRecordVO {
    private Long id;
    private Integer leaderboardType;
    private String periodMonth;
    private Integer rankNo;
    private BigDecimal amount;
    private LocalDateTime grantedAt;
}
