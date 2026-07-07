package com.aichuangzuo.user.modules.message.enums;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

/**
 * 消息模块业务错误码。
 *
 * <p>错误码段：115xxx
 */
@Getter
public enum MessageErrorCode implements ErrorCode {

    MESSAGE_NOT_FOUND(115001, "消息不存在或无权访问");

    private final int code;
    private final String message;

    MessageErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
