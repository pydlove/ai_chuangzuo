package com.aichuangzuo.shared.ai;

import lombok.Getter;
import lombok.Setter;

/**
 * a_model_config（管理端表）在 AI 调用侧的精简视图：仅保存调用所需的字段。
 *
 * <p>admin / user 两端共用，避免各自维护一份同名 DTO。
 */
@Getter
@Setter
public class ActiveModelConfig {

    private Long id;

    private String providerType;

    private String modelCode;

    private String baseUrl;

    private String apiKeyEncrypted;
}
