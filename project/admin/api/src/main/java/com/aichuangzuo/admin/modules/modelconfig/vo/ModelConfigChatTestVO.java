package com.aichuangzuo.admin.modules.modelconfig.vo;

import lombok.Data;

@Data
public class ModelConfigChatTestVO {

    private Integer statusCode;
    private String requestHeaders;
    private String requestBody;
    private String responseBody;
}