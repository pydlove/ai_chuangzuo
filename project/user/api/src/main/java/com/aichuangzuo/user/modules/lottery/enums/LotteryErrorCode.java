package com.aichuangzuo.user.modules.lottery.enums;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LotteryErrorCode implements ErrorCode {

    CAMPAIGN_NOT_FOUND(70001, "活动不存在"),
    CAMPAIGN_NOT_ONGOING(70002, "活动未开始或已结束"),
    NO_DRAW_CHANCE(70003, "没有可用抽奖次数"),
    PRIZE_TIER_NOT_FOUND(70004, "奖项不存在"),
    REDEMPTION_CODE_NOT_FOUND(70005, "兑换码不存在"),
    REDEMPTION_CODE_USED(70006, "兑换码已使用"),
    REDEMPTION_CODE_EXPIRED(70007, "兑换码已过期"),
    INVALID_REWARD_TYPE(70008, "奖励类型无效"),
    COUPON_NOT_APPLICABLE(70009, "优惠券不适用当前套餐或周期"),
    INVALID_COUPON(70010, "优惠券无效或已过期");

    private final int code;
    private final String message;
}
