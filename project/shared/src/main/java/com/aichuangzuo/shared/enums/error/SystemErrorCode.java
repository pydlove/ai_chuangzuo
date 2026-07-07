package com.aichuangzuo.shared.enums.error;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

@Getter
public enum SystemErrorCode implements ErrorCode {
    SYSTEM_ERROR(100001, "系统繁忙，请稍后重试"),
    PARAM_VALIDATION_ERROR(100002, "参数校验失败"),
    RATE_LIMIT_ERROR(100003, "操作过于频繁，请稍后再试");

    private final int code;
    private final String message;

    SystemErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
