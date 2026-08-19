package com.aichuangzuo.admin.modules.security.accesscontrol.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AccessControlRuleType {
    IP(1, "IP"),
    ACCOUNT(2, "账号");

    private final int code;
    private final String label;

    public static AccessControlRuleType of(Integer code) {
        if (code == null) {
            return null;
        }
        for (AccessControlRuleType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }

    public static boolean isValid(Integer code) {
        return of(code) != null;
    }
}
