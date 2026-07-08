package com.aichuangzuo.admin.modules.style.market.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 风格市场 SQL 直出行。Mapper XML 填充，Service 翻译成 {@link com.aichuangzuo.admin.modules.style.market.vo.StyleMarketVO}。
 */
@Data
public class StyleMarketRow {

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
    private Integer enableStatus;
    private LocalDateTime createdAt;
}
