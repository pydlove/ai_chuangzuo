package com.aichuangzuo.user.modules.earnings.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 收益记录状态。
 */
@Getter
@RequiredArgsConstructor
public enum EarningsStatus {

    UNSETTLED(0, "未结算"),
    SETTLED(1, "已结算");

    private final int code;
    private final String label;

    public static EarningsStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (EarningsStatus value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }
}
