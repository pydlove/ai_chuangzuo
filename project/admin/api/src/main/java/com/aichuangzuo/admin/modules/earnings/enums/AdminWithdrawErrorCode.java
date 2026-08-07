package com.aichuangzuo.admin.modules.earnings.enums;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

/**
 * 收益管理-提现管理业务错误码。
 *
 * <p>错误码段：200xxx
 */
@Getter
public enum AdminWithdrawErrorCode implements ErrorCode {

    WITHDRAW_NOT_FOUND(200001, "提现申请不存在或已处理"),
    WITHDRAW_ALREADY_PROCESSED(200002, "提现申请已处理，不可重复操作"),
    WITHDRAW_REJECT_REASON_EMPTY(200003, "拒绝原因不能为空");

    private final int code;
    private final String message;

    AdminWithdrawErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
