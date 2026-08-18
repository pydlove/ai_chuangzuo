package com.aichuangzuo.admin.modules.aiprompt.dto.request;

import lombok.Data;

@Data
public class AiPromptQueryRequest {

    private String module;
    private String category;
    private Integer status;
    private String keyword;
    private Long page = 1L;
    private Long pageSize = 20L;
}
