package com.aichuangzuo.admin.modules.skill.market.config.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 提示词市场月度排行榜奖励配置更新请求。
 */
@Data
public class SkillMonthlyRewardConfigUpdateRequest {

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal firstAmount;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal secondAmount;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal thirdAmount;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal fourthAmount;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal fifthAmount;

    @NotBlank
    @Size(max = 64)
    private String settlementCron;

    @NotNull
    private Integer enabled;
}
