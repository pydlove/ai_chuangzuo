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

    COIN_BALANCE_INSUFFICIENT(114001, "创作币余额不足"),
    INCOME_AMOUNT_INVALID(114002, "申报金额必须大于 0"),
    INCOME_PERIOD_INVALID(114003, "申报月份格式不正确"),
    INCOME_SCREENSHOT_REQUIRED(114004, "请至少上传一张收益截图"),
    INCOME_FILE_INVALID(114005, "截图必须是 jpg/png 且不超过 5MB");

    private final int code;
    private final String message;

    LeaderboardErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
