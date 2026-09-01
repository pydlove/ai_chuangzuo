package com.aichuangzuo.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 用户账号状态枚举。
 */
@Getter
@RequiredArgsConstructor
public enum UserStatusEnum {

    /** 禁用 */
    DISABLED(0, "禁用"),

    /** 正常 */
    ENABLED(1, "正常");

    private final int code;
    private final String desc;
}
