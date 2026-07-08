package com.aichuangzuo.user.modules.earnings.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 收益类型。
 */
@Getter
@RequiredArgsConstructor
public enum EarningsType {

    USAGE("USAGE", "使用收益"),
    MILESTONE("MILESTONE", "里程碑奖励"),
    LEADERBOARD_REWARD("LEADERBOARD_REWARD", "排行榜奖励"),
    INVITE_REWARD("INVITE_REWARD", "邀请奖励"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String label;

    public static EarningsType of(String code) {
        if (code == null) {
            return null;
        }
        for (EarningsType value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
