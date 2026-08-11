package com.aichuangzuo.user.modules.membership.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订阅价格预览结果。
 */
@Data
public class SubscribePreviewVO {

    /** 套餐 key。 */
    private String planKey;

    /** 周期。 */
    private String cycle;

    /** 套餐原价。 */
    private BigDecimal originalPrice;

    /** 升级抵扣金额（非升级时为 0）。 */
    private BigDecimal creditAmount;

    /** 优惠券抵扣金额（元）。 */
    private BigDecimal couponDiscount;

    /** 抵扣后应付现金（创作币抵扣前）。 */
    private BigDecimal finalPrice;

    /** 用户创作币余额。 */
    private BigDecimal coinBalance;

    /** 本次最多可抵扣创作币数量。 */
    private Long maxCoinAmount;

    /** 创作币兑人民币比例。 */
    private Integer coinToYuanRatio;
}
