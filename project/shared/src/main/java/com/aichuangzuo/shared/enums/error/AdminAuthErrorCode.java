package com.aichuangzuo.shared.enums.error;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

@Getter
public enum AdminAuthErrorCode implements ErrorCode {
    ACCOUNT_OR_PASSWORD_ERROR(211001, "账号或密码错误"),
    ACCOUNT_DISABLED(211002, "账号已被禁用"),
    TOKEN_EXPIRED(211003, "登录已过期，请重新登录"),
    REFRESH_TOKEN_INVALID(211004, "refreshToken 无效或已过期"),
    OPERATION_TOO_FREQUENT(211005, "操作过于频繁，请稍后再试");

    private final int code;
    private final String message;

    AdminAuthErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
