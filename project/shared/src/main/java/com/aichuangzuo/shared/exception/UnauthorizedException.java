package com.aichuangzuo.shared.exception;

import com.aichuangzuo.shared.result.ErrorCode;

public class UnauthorizedException extends BaseException {
    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage());
    }
}
