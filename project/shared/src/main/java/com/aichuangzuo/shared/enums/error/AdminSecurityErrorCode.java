package com.aichuangzuo.shared.enums.error;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

@Getter
public enum AdminSecurityErrorCode implements ErrorCode {
    ACCESS_CONTROL_RULE_EXISTS(403001, "访问控制规则已存在"),
    ACCESS_CONTROL_RULE_NOT_FOUND(403002, "访问控制规则不存在"),
    ACCESS_CONTROL_RULE_VALUE_EMPTY(403003, "规则值不能为空");

    private final int code;
    private final String message;

    AdminSecurityErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
