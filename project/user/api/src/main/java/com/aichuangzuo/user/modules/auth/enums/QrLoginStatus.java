package com.aichuangzuo.user.modules.auth.enums;

import lombok.Getter;

@Getter
public enum QrLoginStatus {
    PENDING(0, "待扫描"),
    SCANNED(1, "已扫描"),
    AUTHORIZED(2, "已授权"),
    CANCELLED(3, "已取消"),
    EXPIRED(4, "已过期");

    private final int code;
    private final String label;

    QrLoginStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static QrLoginStatus of(int code) {
        for (QrLoginStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
