package com.aichuangzuo.user.modules.skill.enums;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

/**
 * 用户风格模块业务错误码。
 *
 * <p>错误码段：112xxx
 */
@Getter
public enum SkillErrorCode implements ErrorCode {

    SKILL_NAME_EXISTS(112001, "风格名称已存在"),
    SKILL_NOT_FOUND(112002, "风格不存在或无权访问"),
    SKILL_NAME_EMPTY(112003, "风格名称不能为空"),
    SKILL_PROMPT_EMPTY(112004, "风格提示词不能为空"),
    SKILL_SCOPE_TOO_LONG(112005, "适用范围标签过多或过长"),
    SKILL_ANALYZE_FAILED(112006, "风格分析失败，请重试"),
    SKILL_QUOTA_EXCEEDED(112007, "当前套餐我的风格数量已达上限，升级套餐可保存更多风格");

    private final int code;
    private final String message;

    SkillErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
