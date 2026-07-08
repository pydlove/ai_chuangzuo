package com.aichuangzuo.user.modules.style.market.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户端 - 风格市场视图对象。
 *
 * <p>字段命名兼容 {@code useStyleMarket.js} / {@code StyleMarketIndex.vue} 的契约。
 */
@Data
public class MarketStyleVO {

    private String id;
    private String name;
    private String sourceType;
    private Long creatorId;
    private String creatorName;
    private String prompt;
    private String scope;
    private String excerpt1;
    private String excerpt2;
    private String status;
    private BigDecimal price;
    private Integer weeklyUses;
    private Integer totalUses;
    private BigDecimal weeklyEarnings;
    private BigDecimal milestoneBonus;
    private LocalDateTime lastSettlementAt;
    private LocalDateTime createdAt;
}
