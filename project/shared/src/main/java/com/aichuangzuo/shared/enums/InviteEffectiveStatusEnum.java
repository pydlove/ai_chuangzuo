package com.aichuangzuo.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 邀请关系生效状态枚举。
 *
 * <p>对应表 {@code u_user_invite_relation.effective_status}。
 */
@Getter
@RequiredArgsConstructor
public enum InviteEffectiveStatusEnum {

    /** 待验证 */
    PENDING(0, "待验证"),

    /** 有效 */
    ACTIVE(1, "有效"),

    /** 无效 */
    INVALID(2, "无效");

    private final int code;
    private final String desc;
}
