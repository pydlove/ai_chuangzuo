package com.aichuangzuo.admin.modules.skill.market.config.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
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
    @Min(0)
    @Digits(integer = 10, fraction = 0, message = "奖励金额必须是整数")
    private BigDecimal firstAmount;

    @NotNull
    @Min(0)
    @Digits(integer = 10, fraction = 0, message = "奖励金额必须是整数")
    private BigDecimal secondAmount;

    @NotNull
    @Min(0)
    @Digits(integer = 10, fraction = 0, message = "奖励金额必须是整数")
    private BigDecimal thirdAmount;

    @NotNull
    @Min(0)
    @Digits(integer = 10, fraction = 0, message = "奖励金额必须是整数")
    private BigDecimal fourthAmount;

    @NotNull
    @Min(0)
    @Digits(integer = 10, fraction = 0, message = "奖励金额必须是整数")
    private BigDecimal fifthAmount;

    @NotBlank
    @Size(max = 64)
    private String settlementCron;

    @NotNull
    private Integer enabled;
}
