package com.aichuangzuo.admin.modules.order.enums;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

@Getter
public enum AdminOrderErrorCode implements ErrorCode {
    ORDER_NOT_FOUND(117001, "订单不存在"),
    ORDER_STATUS_NOT_ALLOWED(117002, "订单状态不允许此操作"),
    USER_NOT_FOUND(117003, "用户不存在"),
    REFUND_REASON_REQUIRED(117004, "退款原因不能为空"),
    PAYMENT_CONFIG_NOT_FOUND(117005, "支付配置不存在"),
    PAYMENT_GATEWAY_ERROR(117006, "支付网关调用失败"),
    REFUND_FAILED(117007, "退款失败"),
    INVALID_REFUND_SIGNATURE(117008, "退款响应签名验证失败");

    private final int code;
    private final String message;

    AdminOrderErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
