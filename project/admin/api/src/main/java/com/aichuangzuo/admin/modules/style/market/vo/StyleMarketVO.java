package com.aichuangzuo.admin.modules.style.market.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 风格市场视图对象（前端契约）。
 *
 * <p>{@code id} = {@code bizNo}（前端 row-key），{@code status} 取值 {@code "enabled"} / {@code "disabled"}。
 */
@Data
public class StyleMarketVO {

    private String id;
    private String name;
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
    private String status;
    private LocalDateTime createdAt;
}
