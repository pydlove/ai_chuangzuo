package com.aichuangzuo.admin.modules.leaderboard.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 榜单类型枚举。
 */
@Getter
@RequiredArgsConstructor
public enum LeaderboardType {

    COIN(1, "创作币榜"),
    INCOME(2, "自媒体收入榜");

    private final int code;
    private final String desc;
}
