package com.aichuangzuo.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 封禁状态枚举，适用于 IP 注册限制等 0/1 状态。
 */
@Getter
@RequiredArgsConstructor
public enum BlockStatusEnum {

    /** 未封禁 */
    UNBLOCKED(0, "未封禁"),

    /** 已封禁 */
    BLOCKED(1, "已封禁");

    private final int code;
    private final String desc;
}
