package com.aichuangzuo.admin.modules.earnings.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RewardRecordAdminVO {
    private Long id;
    private String bizNo;
    private Integer leaderboardType;
    private String periodMonth;
    private Integer rankNo;
    private Long userId;
    private String nickname;
    private BigDecimal amount;
    private LocalDateTime grantedAt;
}
