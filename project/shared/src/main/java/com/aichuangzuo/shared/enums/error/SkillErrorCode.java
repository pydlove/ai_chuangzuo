package com.aichuangzuo.shared.enums.error;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

/**
 * 用户风格模块业务错误码。
 *
 * <p>错误码段：112xxx
 */
@Getter
public enum SkillErrorCode implements ErrorCode {

    SKILL_NAME_EXISTS(112001, "提示词名称已存在"),
    SKILL_NOT_FOUND(112002, "风格不存在或无权访问"),
    SKILL_NAME_EMPTY(112003, "风格名称不能为空"),
    SKILL_PROMPT_EMPTY(112004, "风格提示词不能为空"),
    SKILL_SCOPE_TOO_LONG(112005, "适用范围标签过多或过长"),
    SKILL_ANALYZE_FAILED(112006, "风格分析失败，请重试"),
    SKILL_QUOTA_EXCEEDED(112007, "当前套餐我的风格数量已达上限，升级套餐可保存更多风格"),
    SKILL_MARKET_NOT_OWNER(112008, "无权操作该提示词"),
    SKILL_MARKET_PUBLISH_QUOTA_EXCEEDED(112009, "本月提示词发布次数已达上限，升级套餐可发布更多提示词"),
    SKILL_ANALYZE_DAILY_LIMIT_EXCEEDED(112010, "今日的分析额度已经用完，明天再来吧"),
    SKILL_LEARN_QUOTA_EXCEEDED(112011, "本月学习额度已用完，升级套餐可获得更多额度");

    private final int code;
    private final String message;

    SkillErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
