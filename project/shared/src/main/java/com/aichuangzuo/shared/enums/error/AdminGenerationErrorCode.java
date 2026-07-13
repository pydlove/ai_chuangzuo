package com.aichuangzuo.shared.enums.error;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

/**
 * 管理端-文章生成 错误码。
 */
@Getter
public enum AdminGenerationErrorCode implements ErrorCode {

    PROMPT_TEMPLATE_NOT_FOUND(308001, "提示词模板不存在"),
    PROMPT_TEMPLATE_PLACEHOLDER_INVALID(308002, "模板占位符不合法"),
    GENERATION_TASK_NOT_FOUND(308004, "生成任务不存在"),
    GENERATION_TASK_INVALID_STATUS(308005, "任务状态不允许该操作"),
    GENERATION_AI_PROVIDER_ERROR(308006, "AI 服务调用失败"),
    GENERATION_OUTPUT_PARSE_FAILED(308007, "AI 返回内容解析失败"),
    GENERATION_OUTPUT_SCHEMA_INVALID(308008, "AI 返回内容不符合 schema"),
    GENERATION_ARTICLE_PERSIST_FAILED(308009, "生成内容持久化失败"),
    PROMPT_TEMPLATE_NO_PUBLISHED(308010, "当前没有已发布的提示词模板"),
    GENERATION_CONFIG_NOT_FOUND(308011, "创作运行时配置不存在"),
    PROMPT_TEMPLATE_BUILTIN_IMMUTABLE(308012, "内置模板不可删除"),
    PROMPT_TEMPLATE_INVALID_STATUS(308013, "模板状态不允许该操作"),
    GENERATION_MODEL_PARAMS_INVALID(308014, "模板 AI 参数不合法：temperature ∈ [0,2]、max_tokens ∈ [1,8000]、top_p ∈ [0,1]");

    private final int code;
    private final String message;

    AdminGenerationErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
