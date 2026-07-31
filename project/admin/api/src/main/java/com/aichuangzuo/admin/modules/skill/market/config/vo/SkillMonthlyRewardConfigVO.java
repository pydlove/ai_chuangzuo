package com.aichuangzuo.admin.modules.skill.market.config.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提示词市场月度排行榜奖励配置视图对象。
 */
@Data
public class SkillMonthlyRewardConfigVO {

    private Long id;
    private BigDecimal firstAmount;
    private BigDecimal secondAmount;
    private BigDecimal thirdAmount;
    private BigDecimal fourthAmount;
    private BigDecimal fifthAmount;
    private String settlementCron;
    private Integer enabled;
    private BigDecimal pricePerUse;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}
