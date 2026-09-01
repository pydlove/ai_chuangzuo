package com.aichuangzuo.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 通用验证状态枚举，适用于邮箱验证、手机验证等 0/1 状态。
 */
@Getter
@RequiredArgsConstructor
public enum VerifyStatusEnum {

    /** 未验证 */
    UNVERIFIED(0, "未验证"),

    /** 已验证 */
    VERIFIED(1, "已验证");

    private final int code;
    private final String desc;
}
