package com.aichuangzuo.user.modules.feedback.enums;

import com.aichuangzuo.shared.result.ErrorCode;

public enum FeedbackErrorCode implements ErrorCode {
    DAILY_LIMIT_EXCEEDED(117001, "今日反馈次数已达上限，明天再来"),
    CONTENT_REQUIRED(117002, "反馈内容不能为空");

    private final int code;
    private final String message;

    FeedbackErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() { return code; }

    @Override
    public String getMessage() { return message; }
}
