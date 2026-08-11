package com.aichuangzuo.admin.modules.share.enums;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

@Getter
public enum AdminShareErrorCode implements ErrorCode {

    CONFIG_NOT_FOUND(500101, "分享配置不存在"),
    SCENE_KEY_EXISTS(500102, "场景标识已存在");

    private final int code;
    private final String message;

    AdminShareErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
