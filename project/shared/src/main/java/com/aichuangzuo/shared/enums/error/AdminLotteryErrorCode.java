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
    CAMPAIGN_ALREADY_OPEN(72007, "已有其他活动处于开启状态，不能同时开启多个活动"),
    TIER_STOCK_EMPTY(72008, "该奖项剩余奖品不足"),
    TIER_NOT_GRANTABLE(72009, "该奖项不支持人工发奖（谢谢回顾不可发放）"),
    RECORD_NOT_FOUND(72010, "抽奖记录不存在"),
    RECORD_NOT_MANUAL(72011, "仅人工发奖记录支持删除或修改"),
    RECORD_CODE_USED(72012, "兑换码已使用，不能删除或修改获奖人"),
    RECORD_CODE_NOT_UNUSED(72013, "仅未使用的兑换码可修改获奖人");

    private final int code;
    private final String message;
}
