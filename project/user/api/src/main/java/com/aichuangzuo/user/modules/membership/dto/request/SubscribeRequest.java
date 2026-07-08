package com.aichuangzuo.user.modules.membership.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 立即订阅请求。
 */
@Data
public class SubscribeRequest {

    /** 套餐：basic / pro / flagship。 */
    @NotBlank(message = "套餐不能为空")
    private String planKey;

    /** 周期：month / quarter / year。 */
    @NotBlank(message = "订阅周期不能为空")
    private String cycle;

    /** 测试支付码。 */
    @NotBlank(message = "支付码不能为空")
    private String payCode;

    /** 订单金额（取自 Pricing 页当前价格）。 */
    @NotNull(message = "订单金额不能为空")
    @Positive(message = "订单金额必须大于 0")
    private BigDecimal amount;
}
