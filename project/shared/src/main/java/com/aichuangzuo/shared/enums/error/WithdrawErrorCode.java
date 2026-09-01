package com.aichuangzuo.shared.enums.error;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

/**
 * 提现模块业务错误码。
 *
 * <p>错误码段：120xxx
 */
@Getter
public enum WithdrawErrorCode implements ErrorCode {

    REAL_NAME_NOT_VERIFIED(120001, "请先完成实名认证"),
    REAL_NAME_INVALID(120002, "真实姓名或身份证号格式不正确"),
    WITHDRAW_AMOUNT_INVALID(120003, "提现金额必须大于等于 1000 创作币"),
    WITHDRAW_BALANCE_INSUFFICIENT(120004, "创作币余额不足"),
    WITHDRAW_PENDING_EXISTS(120005, "您有正在审核中的提现申请"),
    WITHDRAW_ACCOUNT_INVALID(120006, "收款账号不能为空"),
    WITHDRAW_NOT_FOUND(120007, "提现申请不存在或已处理"),
    WITHDRAW_ALREADY_PROCESSED(120008, "提现申请已处理，不可重复操作"),
    WITHDRAW_REJECT_REASON_EMPTY(120009, "拒绝原因不能为空");

    private final int code;
    private final String message;

    WithdrawErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
