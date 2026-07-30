package com.aichuangzuo.user.modules.skill.market.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户端 - 风格市场收益潜力榜创作者。
 */
@Data
public class TopCreatorVO {

    private Long creatorId;
    private String creatorName;
    private BigDecimal weeklyEarnings;
    private Integer weeklyUses;
    private BigDecimal monthlyEarnings;
    private Integer monthlyUses;
    private MarketSkillVO bestSkill;
    private BigDecimal totalEarnings;
}
