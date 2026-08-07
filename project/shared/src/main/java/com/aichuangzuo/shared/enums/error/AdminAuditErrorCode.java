package com.aichuangzuo.shared.enums.error;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

@Getter
public enum AdminAuditErrorCode implements ErrorCode {
    CONFIG_NOT_FOUND(215001, "审计日志配置不存在"),
    INVALID_RETENTION_DAYS(215002, "保留天数必须在 1-365 之间"),
    INVALID_CLEANUP_CRON(215003, "清理定时表达式格式不正确");

    private final int code;
    private final String message;

    AdminAuditErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
