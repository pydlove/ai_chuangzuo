package com.aichuangzuo.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 邀请关系来源类型枚举。
 *
 * <p>对应表 {@code u_user_invite_relation.source_type}。
 */
@Getter
@RequiredArgsConstructor
public enum InviteSourceTypeEnum {

    /** 链接 */
    LINK(1, "链接"),

    /** 手动填写 */
    MANUAL(2, "手动填写");

    private final int code;
    private final String desc;
}
