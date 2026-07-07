package com.aichuangzuo.shared.enums.error;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

@Getter
public enum AdminUserErrorCode implements ErrorCode {
    USER_NOT_FOUND(220001, "用户不存在"),
    NO_PERMISSION(220002, "无权限访问"),
    STATUS_INVALID(220003, "状态参数非法");

    private final int code;
    private final String message;

    AdminUserErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
