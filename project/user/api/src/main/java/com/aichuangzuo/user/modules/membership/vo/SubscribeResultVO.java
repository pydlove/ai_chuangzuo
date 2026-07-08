package com.aichuangzuo.user.modules.membership.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订阅成功响应。
 */
@Data
public class SubscribeResultVO {

    /** 订单编号。 */
    private String orderNo;

    /** 开通等级。 */
    private String level;

    /** 增加天数。 */
    private int days;

    /** 到期日期 yyyy-MM-dd。 */
    private String expiresAt;

    /** 是否已给邀请人发放奖励。 */
    private boolean inviterRewarded;

    /** 奖励金额（创作币）。 */
    private BigDecimal rewardAmount;
}
