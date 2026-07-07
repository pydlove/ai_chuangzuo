package com.aichuangzuo.user.modules.leaderboard.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 创作币流水方向枚举。
 */
@Getter
@RequiredArgsConstructor
public enum CoinDirection {

    INCOME(1, "收入"),
    EXPENSE(2, "支出");

    private final int code;
    private final String desc;
}
