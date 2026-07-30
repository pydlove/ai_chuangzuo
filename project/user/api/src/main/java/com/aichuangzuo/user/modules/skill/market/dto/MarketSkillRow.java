package com.aichuangzuo.user.modules.skill.market.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户端 - 风格市场 SQL 直出行。
 */
@Data
public class MarketSkillRow {

    private String bizNo;
    private String skillName;
    private String description;
    private String promptSummary;
    private String prompt;
    private String scope;
    private Long publisherUserId;
    private String publisherName;
    private BigDecimal price;
    private Integer totalUses;
    private Integer weeklyUses;
    private BigDecimal weeklyEarnings;
    private BigDecimal milestoneBonus;
    private Integer monthlyUses;
    private BigDecimal monthlyEarnings;
    private BigDecimal leaderboardReward;
    private Integer featured;
    private LocalDateTime lastSettlementAt;
    private LocalDateTime createdAt;
}
