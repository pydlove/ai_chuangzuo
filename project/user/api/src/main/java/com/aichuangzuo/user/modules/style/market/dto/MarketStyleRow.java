package com.aichuangzuo.user.modules.style.market.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户端 - 风格市场 SQL 直出行。
 */
@Data
public class MarketStyleRow {

    private String bizNo;
    private String styleName;
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
    private LocalDateTime lastSettlementAt;
    private LocalDateTime createdAt;
}
