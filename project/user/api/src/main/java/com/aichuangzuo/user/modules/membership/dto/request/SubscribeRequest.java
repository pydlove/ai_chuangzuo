package com.aichuangzuo.user.modules.membership.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    /** 测试支付码（仅在测试模式下必填）。 */
    private String payCode;

    /** 订单金额（取自 Pricing 页当前价格，抵扣后的现金金额）。 */
    @NotNull(message = "订单金额不能为空")
    @DecimalMin(value = "0.00", inclusive = true, message = "订单金额不能为负数")
    private BigDecimal amount;

    /** 创作币抵扣数量（10 创作币 = 1 元）。 */
    @DecimalMin(value = "0", inclusive = true, message = "创作币抵扣数量不能为负数")
    private BigDecimal coinAmount;

    /** 优惠券码（可选）。 */
    private String couponCode;
}
