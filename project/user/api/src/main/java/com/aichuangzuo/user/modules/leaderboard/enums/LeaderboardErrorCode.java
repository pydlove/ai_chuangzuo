package com.aichuangzuo.user.modules.leaderboard.enums;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

/**
 * 收益排行榜模块业务错误码。
 *
 * <p>错误码段：114xxx
 */
@Getter
public enum LeaderboardErrorCode implements ErrorCode {

    COIN_BALANCE_INSUFFICIENT(114001, "创作币余额不足");

    private final int code;
    private final String message;

    LeaderboardErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
