package com.aichuangzuo.admin.modules.modelconfig.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ModelConfigChatTestRequest {

    @NotBlank
    private String baseUrl;

    @NotBlank
    private String apiKey;

    @NotBlank
    private String modelCode;

    @NotBlank
    private String prompt;

    private Boolean stream;
}