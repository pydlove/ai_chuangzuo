package com.aichuangzuo.user.modules.membership.enums;

import com.aichuangzuo.shared.result.ErrorCode;

/**
 * 会员/支付模块错误码，模块编码 16。
 */
public enum MembershipErrorCode implements ErrorCode {

    INVALID_PAY_CODE(116001, "支付码错误"),
    INVALID_PLAN_KEY(116002, "套餐不存在"),
    INVALID_CYCLE(116003, "订阅周期不存在");

    private final int code;
    private final String message;

    MembershipErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
