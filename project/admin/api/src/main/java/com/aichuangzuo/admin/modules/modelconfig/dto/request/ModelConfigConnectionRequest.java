package com.aichuangzuo.admin.modules.modelconfig.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ModelConfigConnectionRequest {

    @NotBlank
    private String baseUrl;

    @NotBlank
    private String apiKey;
}
