package com.aichuangzuo.user.modules.hotsearch.enums;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

/**
 * 热搜模块业务错误码。
 *
 * <p>错误码段：113xxx
 */
@Getter
public enum HotSearchErrorCode implements ErrorCode {

    PLATFORM_NOT_FOUND(113001, "热搜平台不存在"),
    PLATFORM_DISABLED(113002, "热搜平台已停用");

    private final int code;
    private final String message;

    HotSearchErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
