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
    EMAIL_FORMAT_ERROR(220008, "邮箱格式不符合要求"),
    PERIOD_FORMAT_ERROR(220009, "周期格式不正确，应为 yyyy-MM"),
    EXCEL_FILE_INVALID(220010, "Excel 文件无效，请使用正确的模板"),
    EXCEL_PARSE_ERROR(220011, "Excel 解析失败"),
    EXCEL_IMPORT_EMPTY(220012, "Excel 数据为空"),
    MEMBERSHIP_PLAN_INVALID(220013, "套餐不存在或已停用"),
    AVATAR_FILE_INVALID(220014, "头像文件无效，请上传 jpg/png 格式且不超过 5MB 的图片"),
    AVATAR_UPLOAD_FAILED(220015, "头像上传失败"),
    PHONE_ALREADY_EXISTS(220016, "手机号已注册"),
    PHONE_OR_EMAIL_REQUIRED(220017, "邮箱和手机号至少填写一项"),
    MONTHLY_COIN_EARNINGS_INVALID(220018, "当月创作币收益必须大于或等于 0");

    private final int code;
    private final String message;

    AdminUserErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
