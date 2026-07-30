package com.aichuangzuo.shared.exception;

import com.aichuangzuo.shared.result.ErrorCode;

public class BusinessException extends BaseException {
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage());
    }

    public BusinessException(Integer code, String message) {
        super(code, message);
    }
}
