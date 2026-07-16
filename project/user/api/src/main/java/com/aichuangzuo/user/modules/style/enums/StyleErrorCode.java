package com.aichuangzuo.user.modules.style.enums;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

/**
 * 用户风格模块业务错误码。
 *
 * <p>错误码段：112xxx
 */
@Getter
public enum StyleErrorCode implements ErrorCode {

    STYLE_NAME_EXISTS(112001, "风格名称已存在"),
    STYLE_NOT_FOUND(112002, "风格不存在或无权访问"),
    STYLE_NAME_EMPTY(112003, "风格名称不能为空"),
    STYLE_PROMPT_EMPTY(112004, "风格提示词不能为空"),
    STYLE_SCOPE_TOO_LONG(112005, "适用范围标签过多或过长"),
    STYLE_ANALYZE_FAILED(112006, "风格分析失败，请重试");

    private final int code;
    private final String message;

    StyleErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
