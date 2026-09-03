package com.aichuangzuo.admin.modules.skill.market.enums;

import com.aichuangzuo.shared.result.ErrorCode;

/**
 * 管理端 - 风格市场模块业务错误码。
 *
 * <p>前缀 {@code 304}：紧接 globalskill {@code 303} 系列。
 */
public enum AdminSkillMarketErrorCode implements ErrorCode {

    SKILL_MARKET_NOT_FOUND(304001, "风格市场条目不存在"),
    SKILL_MARKET_NAME_EXISTS(304002, "风格市场名称已存在"),
    PUBLISHER_NOT_FOUND(304003, "发布者用户不存在"),
    ENABLE_STATUS_INVALID(304004, "启用状态参数不合法"),
    TOTAL_USES_INVALID(304005, "使用量不能为负数"),
    FEATURED_STATUS_INVALID(304006, "官方精选状态参数不合法"),
    CONSUMER_USER_NOT_FOUND(304007, "使用者用户不存在"),
    SKILL_MARKET_NAME_CONFLICTS_WITH_USER(304008, "提示词名称与用户端已有提示词重复");

    private final int code;
    private final String message;

    AdminSkillMarketErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
