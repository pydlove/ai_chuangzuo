package com.aichuangzuo.admin.modules.leaderboard.enums;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

/**
 * 收益排行榜管理端业务错误码。
 *
 * <p>错误码段：300xxx
 */
@Getter
public enum AdminLeaderboardErrorCode implements ErrorCode {

    SUBMISSION_NOT_FOUND(300101, "申报记录不存在或已审核"),
    REJECT_REASON_EMPTY(300102, "拒绝原因不能为空");

    private final int code;
    private final String message;

    AdminLeaderboardErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
