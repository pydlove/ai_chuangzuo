package com.aichuangzuo.user.modules.benefit.enums;

import com.aichuangzuo.shared.result.ErrorCode;

/**
 * 权益模块错误码，模块编码 18。
 */
public enum BenefitErrorCode implements ErrorCode {

    BENEFIT_NOT_FOUND(118001, "权益不存在"),
    BENEFIT_NOT_SUPPORTED(118002, "当前套餐不支持此功能"),
    QUOTA_EXHAUSTED(118003, "额度已用完"),
    NOT_QUOTA_BENEFIT(118004, "仅配额类权益可消费");

    private final int code;
    private final String message;

    BenefitErrorCode(int code, String message) {
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
