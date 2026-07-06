package com.aichuangzuo.shared.enums.error;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

@Getter
public enum AdminModelConfigErrorCode implements ErrorCode {
    PROVIDER_NOT_SUPPORTED(240001, "厂商类型不支持"),
    MODEL_CONFIG_NOT_FOUND(240002, "模型配置不存在"),
    API_KEY_ENCRYPT_FAILED(240003, "API Key 加密失败"),
    FETCH_MODELS_FAILED(240004, "拉取模型列表失败"),
    TEST_CONNECTION_FAILED(240005, "连接测试失败");

    private final int code;
    private final String message;

    AdminModelConfigErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
