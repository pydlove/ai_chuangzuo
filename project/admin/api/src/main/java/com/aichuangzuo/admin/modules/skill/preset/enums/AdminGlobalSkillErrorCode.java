package com.aichuangzuo.admin.modules.skill.preset.enums;

import com.aichuangzuo.shared.result.ErrorCode;

/**
 * 管理端 - 预设风格模块业务错误码。
 *
 * <p>前缀 {@code 303}：紧接 skillreview {@code 302} 系列。
 */
public enum AdminGlobalSkillErrorCode implements ErrorCode {

    GLOBAL_SKILL_NOT_FOUND(303001, "预设风格不存在"),
    GLOBAL_SKILL_NAME_EXISTS(303002, "预设风格名称已存在"),
    ENABLE_STATUS_INVALID(303003, "启用状态参数不合法");

    private final int code;
    private final String message;

    AdminGlobalSkillErrorCode(int code, String message) {
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