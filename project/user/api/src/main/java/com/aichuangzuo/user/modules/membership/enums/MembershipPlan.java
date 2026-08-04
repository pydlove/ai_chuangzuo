package com.aichuangzuo.user.modules.membership.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 会员套餐枚举。
 */
@Getter
public enum MembershipPlan {

    BASIC("basic", "基础版", 0),
    PRO("pro", "专业版", 1),
    FLAGSHIP("flagship", "旗舰版", 2);

    private final String key;
    private final String displayName;
    private final int rank;

    MembershipPlan(String key, String displayName, int rank) {
        this.key = key;
        this.displayName = displayName;
        this.rank = rank;
    }

    /**
     * 根据 key 解析，未匹配返回 null。
     */
    public static MembershipPlan of(String key) {
        if (key == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(p -> p.key.equals(key))
                .findFirst()
                .orElse(null);
    }
}
