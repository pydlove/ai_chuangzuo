package com.aichuangzuo.user.modules.benefit.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 新人首冲优惠：非邀请新用户首次购买旗舰版年包，在正常年付价基础上再打 8 折。
 */
@Getter
@Setter
public class NewcomerOfferVO {

    /** 是否可享受新人优惠。 */
    private boolean eligible;

    /** 套餐 key，固定 flagship。 */
    private String planKey;

    /** 套餐显示名。 */
    private String planName;

    /** 周期，固定 year。 */
    private String cycle;

    /** 年付原价（划线价）。 */
    private BigDecimal originalPrice;

    /** 正常年付折扣价。 */
    private BigDecimal regularPrice;

    /** 新人首冲再 8 折后的最终价。 */
    private BigDecimal finalPrice;

    /** 相比原价共节省金额。 */
    private BigDecimal savings;

    /** 旗舰版包含的核心权益列表。 */
    private List<String> benefits;
}
