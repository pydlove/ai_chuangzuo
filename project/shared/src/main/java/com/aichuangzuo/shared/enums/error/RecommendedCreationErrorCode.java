package com.aichuangzuo.shared.enums.error;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

@Getter
public enum RecommendedCreationErrorCode implements ErrorCode {

    SELF_MEDIA_PLAN_REQUIRED(118001, "请先制定自媒体运营方案"),
    SESSION_NOT_FOUND(118002, "创作会话不存在，请重新开始"),
    TOPIC_NOT_FOUND(118003, "选题不存在，请重新生成选题"),
    SESSION_INCOMPLETE(118004, "请完成选题、观点、字数、提示词和模板选择"),
    AI_RESPONSE_INVALID(118005, "AI 返回格式异常，请重试");

    private final int code;
    private final String message;

    RecommendedCreationErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
