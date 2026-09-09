package com.aichuangzuo.admin.modules.skill.review.enums;

import com.aichuangzuo.shared.result.ErrorCode;

/**
 * 管理端 - 风格审核模块业务错误码。
 *
 * <p>前缀 {@code 302}：与 leaderboard {@code 300} 系列同属「内容审核」块。
 */
public enum AdminSkillReviewErrorCode implements ErrorCode {

    SKILL_REVIEW_NOT_FOUND(302001, "风格审核记录不存在"),
    REJECT_REASON_EMPTY(302004, "打回原因不能为空"),
    SKILL_REVIEW_NOT_PENDING(302005, "该提示词不在待审核状态，可能已被用户撤销"),
    SKILL_REVIEW_DELETE_FORBIDDEN(302006, "真实待审核的提交不能删除，请使用通过或打回");

    private final int code;
    private final String message;

    AdminSkillReviewErrorCode(int code, String message) {
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