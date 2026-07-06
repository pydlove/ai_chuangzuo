package com.aichuangzuo.admin.modules.modelconfig.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ModelConfigSaveRequest {

    @NotBlank
    private String baseUrl;

    private String apiKey;

    @NotBlank
    private String modelCode;

    private String modelName;

    @NotNull
    private Integer isActive;
}
