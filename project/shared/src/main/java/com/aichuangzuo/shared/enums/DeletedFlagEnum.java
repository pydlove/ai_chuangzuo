package com.aichuangzuo.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 逻辑删除标记枚举。
 */
@Getter
@RequiredArgsConstructor
public enum DeletedFlagEnum {

    /** 未删除 */
    NOT_DELETED(0, "未删除"),

    /** 已删除 */
    DELETED(1, "已删除");

    private final int code;
    private final String desc;
}
