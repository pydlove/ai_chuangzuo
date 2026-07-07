package com.aichuangzuo.admin.modules.modelconfig.vo;

import lombok.Data;

@Data
public class ModelConfigVO {

    private Long id;
    private String providerType;
    private String providerName;
    private String baseUrl;
    private String apiKey;
    private String modelCode;
    private String modelName;
    private Integer isActive;
}
