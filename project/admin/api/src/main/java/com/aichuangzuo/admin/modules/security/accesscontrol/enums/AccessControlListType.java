package com.aichuangzuo.admin.modules.security.accesscontrol.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AccessControlListType {
    BLACK(1, "黑名单"),
    WHITE(2, "白名单");

    private final int code;
    private final String label;

    public static AccessControlListType of(Integer code) {
        if (code == null) {
            return null;
        }
        for (AccessControlListType type : values()) {
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
