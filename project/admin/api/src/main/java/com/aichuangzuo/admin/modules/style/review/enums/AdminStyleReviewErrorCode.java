package com.aichuangzuo.admin.modules.style.review.enums;

import com.aichuangzuo.shared.result.ErrorCode;

/**
 * 管理端 - 风格审核模块业务错误码。
 *
 * <p>前缀 {@code 302}：与 leaderboard {@code 300} 系列同属「内容审核」块。
 */
public enum AdminStyleReviewErrorCode implements ErrorCode {

    STYLE_REVIEW_NOT_FOUND(302001, "风格审核记录不存在"),
    STYLE_REVIEW_ALREADY_APPROVED(302002, "该风格已通过审核"),
    STYLE_REVIEW_ALREADY_REJECTED(302003, "该风格已被打回"),
    REJECT_REASON_EMPTY(302004, "打回原因不能为空");

    private final int code;
    private final String message;

    AdminStyleReviewErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}