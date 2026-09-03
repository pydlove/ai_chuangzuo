package com.aichuangzuo.user.modules.membership.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户订单视图。
 */
@Data
public class OrderVO {

    /** 订单ID。 */
    private Long id;

    /** 订单编号。 */
    private String orderNo;

    /** 套餐key。 */
    private String planKey;

    /** 套餐显示名。 */
    private String planName;

    /** 周期code。 */
    private String cycle;

    /** 周期显示名。 */
    private String cycleName;

    /** 现金实付金额（元）。 */
    private BigDecimal amount;

    /** 创作币抵扣数量。 */
    private Long coinAmount;

    /** 创作币抵扣金额（元）。 */
    private BigDecimal coinDiscount;

    /** 优惠券抵扣金额（元）。 */
    private BigDecimal couponDiscount;

    /** 订单名义总金额。 */
    private BigDecimal totalAmount;

    /** 状态：0-待支付，1-已支付。 */
    private Integer status;

    /** 状态显示名。 */
    private String statusName;

    /** 支付时间。 */
    private LocalDateTime paidAt;

    /** 第三方交易流水号。 */
    private String thirdPartyTradeId;

    /** 创建时间。 */
    private LocalDateTime createdAt;
}
