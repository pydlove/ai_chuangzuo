package com.aichuangzuo.admin.modules.message.enums;

import com.aichuangzuo.shared.result.ErrorCode;

/**
 * 管理端-消息管理模块业务错误码。
 *
 * <p>前缀 306：紧接 style.market 304 系列；用户端 message 115xxx 互不冲突。
 */
public enum AdminMessageErrorCode implements ErrorCode {

    MESSAGE_NOT_FOUND(306001, "消息不存在"),
    MESSAGE_TYPE_INVALID(306002, "消息类型不合法，仅支持 announcement / feature / promotion"),
    TARGET_USERS_EMPTY(306003, "指定人消息必须选择至少 1 个接收用户"),
    TARGET_USERS_TOO_MANY(306004, "指定人消息单次最多 1000 个用户"),
    FEATURE_BROADCAST_ONLY(306005, "新功能仅支持全体发送");

    private final int code;
    private final String message;

    AdminMessageErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
