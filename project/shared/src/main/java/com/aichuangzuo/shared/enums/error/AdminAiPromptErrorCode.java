package com.aichuangzuo.shared.enums.error;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

/**
 * 管理端 AI 提示词配置错误码（2401xx）。
 */
@Getter
public enum AdminAiPromptErrorCode implements ErrorCode {

    AI_PROMPT_NOT_FOUND(240101, "提示词配置不存在"),
    AI_PROMPT_DISABLED(240102, "提示词配置已停用"),
    AI_PROMPT_VARIABLE_MISSING(240103, "提示词必填变量缺失"),
    AI_PROMPT_CODE_EXISTS(240104, "提示词编码已存在"),
    AI_PROMPT_RENDER_ERROR(240105, "提示词渲染异常");

    private final int code;
    private final String message;

    AdminAiPromptErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
