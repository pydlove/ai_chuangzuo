package com.aichuangzuo.shared.enums.error;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AdminLotteryErrorCode implements ErrorCode {

    CAMPAIGN_NOT_FOUND(72001, "活动不存在"),
    CAMPAIGN_CANNOT_UPDATE(72002, "活动已结束，不可编辑"),
    TIER_KEY_EXISTS(72003, "奖项标识已存在"),
    PROBABILITY_SUM_EXCEEDS(72004, "奖项概率之和不能超过 1"),
    TIER_NOT_FOUND(72005, "奖项不存在"),
    INVALID_CAMPAIGN_STATUS(72006, "活动状态无效"),
    CAMPAIGN_ALREADY_OPEN(72007, "已有其他活动处于开启状态，不能同时开启多个活动");

    private final int code;
    private final String message;
}
