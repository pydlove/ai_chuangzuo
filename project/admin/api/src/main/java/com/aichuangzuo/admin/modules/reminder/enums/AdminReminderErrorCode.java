package com.aichuangzuo.admin.modules.reminder.enums;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

/**
 * 到期提醒模块错误码，段位 26xxxx。
 */
@Getter
public enum AdminReminderErrorCode implements ErrorCode {

    CONFIG_NOT_FOUND(260001, "提醒配置不存在"),
    INVALID_NOTIFY_HOUR(260002, "提醒时间点非法（0-23）"),
    INVALID_NOTIFY_CHANNEL(260003, "通知形式非法（message/email/message_email）"),
    INVALID_ADVANCE_DAYS(260004, "提前天数非法（1-90）"),
    TARGET_USER_NOT_FOUND(260005, "目标用户不存在或无会员到期时间");

    private final int code;
    private final String message;

    AdminReminderErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}