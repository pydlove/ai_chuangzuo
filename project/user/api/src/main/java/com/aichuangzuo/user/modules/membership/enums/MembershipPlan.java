package com.aichuangzuo.user.modules.membership.enums;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * 会员套餐枚举。
 */
@Getter
public enum MembershipPlan {

    BASIC("basic", "基础版", new BigDecimal("3")),
    PRO("pro", "专业版", new BigDecimal("5")),
    FLAGSHIP("flagship", "旗舰版", new BigDecimal("10"));

    private final String key;
    private final String displayName;
    private final BigDecimal inviterReward;

    MembershipPlan(String key, String displayName, BigDecimal inviterReward) {
        this.key = key;
        this.displayName = displayName;
        this.inviterReward = inviterReward;
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
