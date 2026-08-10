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
    MEMBERSHIP_PLAN_INVALID(220013, "套餐不存在或已停用");

    private final int code;
    private final String message;

    AdminUserErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
