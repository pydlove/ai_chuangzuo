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
    RESET_PASSWORD_FAILED(111021, "重置失败，请检查邮箱或验证码"),
    TOKEN_INVALID(111022, "token 无效或已过期"),
    TOKEN_BLACKLISTED(111023, "token 已被登出"),
    ACCESS_DENIED(111024, "访问受限，请联系管理员"),
    PHONE_FORMAT_ERROR(111025, "手机号格式错误"),
    PHONE_ALREADY_EXISTS(111026, "手机号已注册"),
    PHONE_NOT_FOUND(111027, "手机号未注册"),
    SMS_SEND_FAILED(111028, "短信发送失败，请稍后重试"),
    SMS_CODE_ERROR(111029, "短信验证码错误或已过期"),
    SMS_CONFIG_NOT_ENABLED(111030, "短信验证未启用"),
    PHONE_OR_EMAIL_REQUIRED(111031, "邮箱或手机号至少填写一项"),
    AVATAR_FILE_INVALID(111032, "头像必须是 jpg/png 且不超过 5MB"),
    PHONE_SAME_AS_OLD(111033, "新手机号与原手机号相同"),
    QR_LOGIN_SESSION_NOT_FOUND(111040, "二维码不存在或已过期"),
    QR_LOGIN_SESSION_EXPIRED(111041, "二维码已过期，请刷新重试"),
    QR_LOGIN_SESSION_ALREADY_USED(111042, "二维码已被使用"),
    QR_LOGIN_SESSION_ALREADY_SCANNED(111043, "二维码已被其他设备扫描"),
    QR_LOGIN_STATUS_INVALID(111044, "二维码状态异常"),
    QR_LOGIN_UNAUTHORIZED(111045, "请先登录手机端"),
    EXPERIENCE_TOKEN_INVALID(111046, "体验链接无效"),
    EXPERIENCE_TOKEN_USED(111047, "体验链接已被使用"),
    EXPERIENCE_TOKEN_EXPIRED(111048, "体验链接已过期"),
    REGISTER_PARAM_INVALID(111049, "邀请码和体验链接不能同时填写");

    private final int code;
    private final String message;

    UserAuthErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
