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
    private Integer featured;
    private LocalDateTime lastSettlementAt;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;

    /** 来源类型：1-自定义 / 2-学习；用于收藏列表等需要区分来源的场景。 */
    private Integer sourceType;

    /** 启用状态：0-禁用 / 1-启用。 */
    private Integer enableStatus;

    /** 审核状态：0-待审核 / 1-已通过 / 2-已打回。 */
    private Integer auditStatus;

    /** 逻辑删除标记：0-未删除 / 1-已删除。 */
    private Integer isDeleted;
}
