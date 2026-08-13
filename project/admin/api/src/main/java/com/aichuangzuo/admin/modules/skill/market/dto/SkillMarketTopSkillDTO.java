package com.aichuangzuo.admin.modules.skill.market.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 热门提示词排行行。
 */
@Data
public class SkillMarketTopSkillDTO {
    private String skillName;
    private String publisherName;
    private Long publisherUserId;
    private Integer totalUses;
    private Integer weeklyUses;
    private BigDecimal weeklyEarnings;
    private BigDecimal milestoneBonus;
}
