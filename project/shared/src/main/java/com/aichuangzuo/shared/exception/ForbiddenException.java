package com.aichuangzuo.shared.exception;

import com.aichuangzuo.shared.result.ErrorCode;

public class ForbiddenException extends BaseException {
    public ForbiddenException(ErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage());
    }
}
