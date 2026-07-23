package com.aichuangzuo.shared.enums.error;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

@Getter
public enum UserAuthErrorCode implements ErrorCode {
    EMAIL_FORMAT_ERROR(111001, "邮箱格式错误"),
    CAPTCHA_ERROR(111002, "图形验证码错误"),
    EMAIL_CODE_ERROR(111003, "邮箱验证码错误或已过期"),
    ACCOUNT_OR_PASSWORD_ERROR(111004, "账号或密码错误"),
    ACCOUNT_DISABLED(111005, "账号已被禁用"),
    EMAIL_ALREADY_EXISTS(111006, "邮箱已注册"),
    PASSWORD_NOT_MATCH(111007, "两次密码不一致"),
    PASSWORD_FORMAT_ERROR(111008, "密码格式不符合要求"),
    INVITE_CODE_INVALID(111009, "邀请码无效"),
    TOKEN_EXPIRED(111010, "登录已过期，请重新登录"),
    REFRESH_TOKEN_INVALID(111011, "refreshToken 无效或已过期"),
    OPERATION_TOO_FREQUENT(111012, "操作过于频繁，请稍后再试"),
    USER_NOT_FOUND(111013, "用户不存在"),
    EMAIL_SEND_FAILED(111014, "邮件发送失败,请稍后重试"),
    EMAIL_SAME_AS_OLD(111015, "新邮箱与原邮箱相同"),
    PASSWORD_INCORRECT(111016, "原密码错误"),
    INVITE_ALREADY_BOUND(111017, "已绑定邀请人，不可修改"),
    INVITE_SELF_NOT_ALLOWED(111018, "不能绑定自己为邀请人"),
    INVITE_CIRCULAR_NOT_ALLOWED(111019, "不能形成循环邀请关系"),
    INVITE_BINDING_EXPIRED(111020, "邀请人绑定已过期"),
    RESET_PASSWORD_FAILED(111021, "重置失败，请检查邮箱或验证码");

    private final int code;
    private final String message;

    UserAuthErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
