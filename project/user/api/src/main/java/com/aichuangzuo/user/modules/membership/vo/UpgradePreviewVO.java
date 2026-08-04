package com.aichuangzuo.user.modules.membership.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 升级套餐预览结果。
 */
@Data
public class UpgradePreviewVO {

    /** 当前是否拥有有效会员。 */
    private boolean hasMembership;

    /** 当前套餐 key。 */
    private String currentPlanKey;

    /** 当前套餐显示名。 */
    private String currentPlanName;

    /** 当前会员到期日 yyyy-MM-dd。 */
    private String currentExpiresAt;

    /** 剩余天数（≥1）。 */
    private int remainingDays;

    /** 是否升级（目标套餐高于当前套餐）。 */
    private boolean upgrade;

    /** 目标套餐 key。 */
    private String targetPlanKey;

    /** 目标套餐显示名。 */
    private String targetPlanName;

    /** 目标周期。 */
    private String targetCycle;

    /** 目标套餐原价（未抵扣）。 */
    private BigDecimal originalPrice;

    /** 当前订阅剩余价值抵扣金额。 */
    private BigDecimal creditAmount;

    /** 抵扣后应付金额。 */
    private BigDecimal finalPrice;

    /** 目标周期天数。 */
    private int targetDays;

    /** 升级后到期日 yyyy-MM-dd。 */
    private String newExpiresAt;
}
