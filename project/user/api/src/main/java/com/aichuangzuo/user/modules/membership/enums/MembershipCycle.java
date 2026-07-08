package com.aichuangzuo.user.modules.membership.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 会员周期枚举。
 */
@Getter
public enum MembershipCycle {

    MONTH("month", 30),
    QUARTER("quarter", 90),
    YEAR("year", 365);

    private final String code;
    private final int days;

    MembershipCycle(String code, int days) {
        this.code = code;
        this.days = days;
    }

    /**
     * 根据 code 解析，未匹配返回 null。
     */
    public static MembershipCycle of(String code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(c -> c.code.equals(code))
                .findFirst()
                .orElse(null);
    }
}
