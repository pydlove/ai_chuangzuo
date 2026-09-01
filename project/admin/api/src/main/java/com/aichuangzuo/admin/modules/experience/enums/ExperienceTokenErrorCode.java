package com.aichuangzuo.admin.modules.experience.enums;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

/**
 * 体验会员管理错误码，段位 5200xx。
 */
@Getter
public enum ExperienceTokenErrorCode implements ErrorCode {

    GENERATE_COUNT_INVALID(520001, "生成数量必须在 1-1000 之间"),
    MEMBERSHIP_DAYS_INVALID(520002, "会员天数必须大于 0"),
    TOKEN_NOT_FOUND(520003, "体验令牌不存在");

    private final int code;
    private final String message;

    ExperienceTokenErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
