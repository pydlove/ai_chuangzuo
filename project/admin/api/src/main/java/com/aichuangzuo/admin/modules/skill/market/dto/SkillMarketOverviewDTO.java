package com.aichuangzuo.admin.modules.skill.market.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 提示词市场统计概览聚合结果。
 */
@Data
public class SkillMarketOverviewDTO {
    private Long totalSkills;
    private Long enabledSkills;
    private Long disabledSkills;
    private Long featuredSkills;
    private Long totalUses;
    private Long weeklyUses;
    private BigDecimal totalEarnings;
    private BigDecimal weeklyEarnings;
    private BigDecimal milestoneBonus;
}
