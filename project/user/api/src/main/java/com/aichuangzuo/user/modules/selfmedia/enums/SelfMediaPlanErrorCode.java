package com.aichuangzuo.user.modules.selfmedia.enums;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

@Getter
public enum SelfMediaPlanErrorCode implements ErrorCode {

    SELF_MEDIA_PLAN_NOT_FOUND(113001, "运营方案不存在"),
    SELF_MEDIA_PLAN_AI_FAILED(113002, "AI 推荐失败，请重试"),
    SELF_MEDIA_PLAN_PLATFORM_REQUIRED(113003, "请选择自媒体平台"),
    SELF_MEDIA_PLAN_GOAL_REQUIRED(113004, "请选择运营目标"),
    SELF_MEDIA_PLAN_NICHE_REQUIRED(113005, "请选择细分赛道"),
    SELF_MEDIA_PLAN_PERSONA_REQUIRED(113006, "请选择人设定位");

    private final int code;
    private final String message;

    SelfMediaPlanErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
