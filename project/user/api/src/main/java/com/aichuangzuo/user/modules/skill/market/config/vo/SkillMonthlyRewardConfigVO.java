package com.aichuangzuo.user.modules.skill.market.config.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 提示词市场月度排行榜奖励配置（用户端展示）。
 */
@Data
public class SkillMonthlyRewardConfigVO {

    private BigDecimal firstAmount;
    private BigDecimal secondAmount;
    private BigDecimal thirdAmount;
    private BigDecimal fourthAmount;
    private BigDecimal fifthAmount;
    private Integer enabled;
    private BigDecimal pricePerUse;
}
