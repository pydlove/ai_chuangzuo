package com.aichuangzuo.user.modules.commission.enums;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

@Getter
public enum CommissionErrorCode implements ErrorCode {
    TASK_NOT_FOUND(119101, "约稿任务不存在"),
    TASK_NOT_RECRUITING(119102, "约稿任务已停止投稿"),
    ARTICLE_NOT_FOUND(119103, "平台生成文章不存在或无权访问"),
    ARTICLE_NOT_COMPLETED(119104, "文章尚未生成完成"),
    ARTICLE_WORD_COUNT_INVALID(119105, "文章字数不符合任务要求"),
    ACTIVE_SUBMISSION_EXISTS(119106, "该任务已有进行中的投稿"),
    SUBMISSION_NOT_FOUND(119107, "投稿不存在或无权访问"),
    SUBMISSION_NOT_WITHDRAWABLE(119108, "当前投稿无法撤回"),
    ARTICLE_ALREADY_SUBMITTED(119109, "该文章已投递其他任务");

    private final int code;
    private final String message;

    CommissionErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
