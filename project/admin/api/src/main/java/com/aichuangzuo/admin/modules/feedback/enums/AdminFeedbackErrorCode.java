package com.aichuangzuo.admin.modules.feedback.enums;

import com.aichuangzuo.shared.result.ErrorCode;

public enum AdminFeedbackErrorCode implements ErrorCode {
    FEEDBACK_NOT_FOUND(217001, "反馈不存在"),
    ALREADY_REPLIED(217002, "该反馈已回复，不可重复");

    private final int code;
    private final String message;

    AdminFeedbackErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override public int getCode() { return code; }
    @Override public String getMessage() { return message; }
}
