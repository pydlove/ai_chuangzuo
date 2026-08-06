package com.aichuangzuo.user.modules.membership.enums;

import com.aichuangzuo.shared.result.ErrorCode;

/**
 * 会员/支付模块错误码，模块编码 16。
 */
public enum MembershipErrorCode implements ErrorCode {

    INVALID_PAY_CODE(116001, "支付码错误"),
    INVALID_PLAN_KEY(116002, "套餐不存在"),
    INVALID_CYCLE(116003, "订阅周期不存在"),
    INVALID_AMOUNT(116004, "支付金额与系统计算不符"),
    UPGRADE_CYCLE_MISMATCH(116005, "升级不能缩短当前订阅周期"),
    INVALID_COIN_AMOUNT(116006, "创作币抵扣金额不正确");

    private final int code;
    private final String message;

    MembershipErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
