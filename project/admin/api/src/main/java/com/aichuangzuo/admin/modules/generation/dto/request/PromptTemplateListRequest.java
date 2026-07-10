package com.aichuangzuo.admin.modules.generation.dto.request;

import lombok.Data;

@Data
public class PromptTemplateListRequest {
    private String keyword;
    private long page = 1;
    private long pageSize = 20;
}
