package com.aichuangzuo.shared.enums.error;

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
    INVALID_COIN_AMOUNT(116006, "创作币抵扣金额不正确"),
    INVALID_COUPON(116007, "优惠券无效或已过期"),
    COUPON_NOT_APPLICABLE(116008, "优惠券不适用于当前套餐或周期"),
    PAYMENT_NOT_ENABLED(116009, "支付功能未启用"),
    PAYMENT_GATEWAY_ERROR(116010, "支付网关调用失败"),
    ORDER_NOT_FOUND(116011, "订单不存在"),
    ORDER_ALREADY_PAID(116012, "订单已支付"),
    INVALID_PAYMENT_SIGNATURE(116013, "支付签名验证失败"),
    PAYMENT_CONFIRM_FAILED(116014, "订单确认失败");

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
