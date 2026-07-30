package com.aichuangzuo.admin.modules.skill.market.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 风格市场 SQL 直出行。Mapper XML 填充，Service 翻译成 {@link com.aichuangzuo.admin.modules.skill.market.vo.SkillMarketVO}。
 */
@Data
public class SkillMarketRow {

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
    private Integer enableStatus;
    private Integer featured;
    private LocalDateTime createdAt;
}
