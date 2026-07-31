package com.aichuangzuo.admin.modules.skill.market.config.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新提示词市场单次使用收益单价请求。
 */
@Data
public class SkillPricePerUseUpdateRequest {

    @NotNull(message = "单次收益单价不能为空")
    @Min(value = 0, message = "单次收益单价不能为负数")
    @Digits(integer = 10, fraction = 0, message = "单次收益单价必须是整数")
    private BigDecimal pricePerUse;
}
