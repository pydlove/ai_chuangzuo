package com.aichuangzuo.admin.modules.style.review.entity;

import lombok.Getter;

/**
 * 风格审核状态。
 */
@Getter
public enum AuditStatus {

    /** 待审核 */
    PENDING(0, "待审核"),

    /** 已通过 */
    APPROVED(1, "已通过"),

    /** 已拒绝 */
    REJECTED(2, "已拒绝");

    private final int code;
    private final String desc;

    AuditStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据 code 获取枚举；null 或未知值返回 {@link #PENDING}。
     */
    public static AuditStatus of(Integer code) {
        if (code == null) {
            return PENDING;
        }
        for (AuditStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return PENDING;
    }
}