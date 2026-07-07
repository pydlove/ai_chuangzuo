package com.aichuangzuo.shared.enums.error;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

/**
 * 管理端收益模块错误码。
 *
 * <p>错误码段：3002xx
 */
@Getter
public enum AdminEarningsErrorCode implements ErrorCode {

    USER_NOT_FOUND(300201, "用户不存在"),
    SETTLEMENT_MONTH_INVALID(300202, "结算月份格式错误"),
    SETTLEMENT_NO_PENDING_RECORDS(300203, "所选月份无待结算记录"),
    SUBMISSION_NOT_FOUND_OR_AUDITED(300204, "申报记录不存在或已审核"),
    REJECT_REASON_EMPTY(300205, "拒绝原因不能为空"),
    LEADERBOARD_TYPE_INVALID(300206, "榜单类型非法"),
    LEADERBOARD_PERIOD_INVALID(300207, "榜单周期格式错误"),
    GRANT_CROSS_SERVICE_FAILED(300208, "发奖时跨端调用失败"),
    GRANT_DUPLICATE(300209, "重复发奖");

    private final int code;
    private final String message;

    AdminEarningsErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
