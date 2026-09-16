package com.aichuangzuo.admin.modules.simulation.enums;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

@Getter
public enum AdminSimulationErrorCode implements ErrorCode {
    PARAM_INVALID(121001, "模拟批次参数不合法"),
    BATCH_NOT_FOUND(121002, "模拟批次不存在"),
    BATCH_STATUS_INVALID(121003, "批次状态不允许此操作");

    private final int code;
    private final String message;

    AdminSimulationErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
