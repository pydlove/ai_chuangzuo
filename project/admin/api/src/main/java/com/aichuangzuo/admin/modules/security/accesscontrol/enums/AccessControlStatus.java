package com.aichuangzuo.admin.modules.security.accesscontrol.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AccessControlStatus {
    DISABLED(0, "禁用"),
    ENABLED(1, "启用");

    private final int code;
    private final String label;

    public static AccessControlStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (AccessControlStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }

    public static boolean isValid(Integer code) {
        return of(code) != null;
    }
}
