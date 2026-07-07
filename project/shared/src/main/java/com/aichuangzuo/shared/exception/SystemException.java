package com.aichuangzuo.shared.exception;

import com.aichuangzuo.shared.enums.error.SystemErrorCode;

public class SystemException extends BaseException {
    public SystemException(String message) {
        super(SystemErrorCode.SYSTEM_ERROR.getCode(), message);
    }
}
