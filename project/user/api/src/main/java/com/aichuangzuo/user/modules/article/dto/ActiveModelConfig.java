package com.aichuangzuo.user.modules.article.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 用户端只读 a_model_config（管理端表）的精简视图：仅 AI 标题优化调用所需的字段。
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
