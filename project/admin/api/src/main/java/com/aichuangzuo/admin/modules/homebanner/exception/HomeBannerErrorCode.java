package com.aichuangzuo.admin.modules.homebanner.exception;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 首页 Banner 模块错误码，段位 2800xx。
 */
@Getter
@AllArgsConstructor
public enum HomeBannerErrorCode implements ErrorCode {

    BANNER_NOT_FOUND(280001, "首页 Banner 不存在");

    private final int code;
    private final String message;
}
