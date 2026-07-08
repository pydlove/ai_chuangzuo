package com.aichuangzuo.shared.enums.error;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

@Getter
public enum AdminUserErrorCode implements ErrorCode {
    USER_NOT_FOUND(220001, "用户不存在"),
    NO_PERMISSION(220002, "无权限访问"),
    STATUS_INVALID(220003, "状态参数非法"),
    EMAIL_ALREADY_EXISTS(220004, "邮箱已注册"),
    USER_TYPE_INVALID(220005, "用户类型参数非法"),
    PASSWORD_FORMAT_ERROR(220006, "密码格式不符合要求"),
    NICKNAME_FORMAT_ERROR(220007, "昵称格式不符合要求"),
    EMAIL_FORMAT_ERROR(220008, "邮箱格式不符合要求");

    private final int code;
    private final String message;

    AdminUserErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
