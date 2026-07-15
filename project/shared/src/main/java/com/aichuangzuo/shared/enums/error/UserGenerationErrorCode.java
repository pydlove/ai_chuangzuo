package com.aichuangzuo.shared.enums.error;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

/**
 * 用户端-文章生成 错误码。
 */
@Getter
public enum UserGenerationErrorCode implements ErrorCode {

    GENERATION_INPUT_INVALID(212001, "输入参数不合法"),
    GENERATION_TITLE_REQUIRED(212002, "标题不能为空"),
    GENERATION_WORD_LIMIT_TOO_LARGE(212003, "字数不能超过 3000"),
    GENERATION_RATE_LIMIT(212004, "操作过于频繁，请稍候再试"),
    GENERATION_MODEL_UNAVAILABLE(212006, "AI 模型暂不可用"),
    GENERATION_TASK_NOT_FOUND(212007, "生成任务不存在"),
    GENERATION_INTERNAL_FORBIDDEN(212008, "禁止的内部调用"),
    GENERATION_TEMPLATE_DISABLED(212009, "暂无可用的提示词模板，请联系管理员"),
    GENERATION_ARTICLE_SAVE_FAILED(212010, "生成内容保存失败"),
    GENERATION_TEMPLATE_NOT_AVAILABLE(212012, "所选模板不可用"),
    GENERATION_TEMPLATE_NOT_PUBLISHED(212013, "所选模板未发布，无法选择");

    private final int code;
    private final String message;

    UserGenerationErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
