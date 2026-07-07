package com.aichuangzuo.admin.modules.leaderboard.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端奖励发放记录 VO。
 */
@Data
public class RewardRecordAdminVO {

    private Long id;
    private String bizNo;
    private Integer leaderboardType;
    private String periodMonth;
    private Integer rankNo;
    private Long userId;
    private BigDecimal amount;
    private String coinRecordBizNo;
    private Long grantedBy;
    private LocalDateTime grantedAt;
    private LocalDateTime createdAt;
}
