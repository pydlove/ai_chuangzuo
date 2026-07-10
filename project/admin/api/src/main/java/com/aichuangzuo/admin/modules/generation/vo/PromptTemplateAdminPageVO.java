package com.aichuangzuo.admin.modules.generation.vo;

import lombok.Data;

import java.util.List;

@Data
public class PromptTemplateAdminPageVO {
    private List<PromptTemplateAdminVO> list;
    private long total;
    private long page;
    private long pageSize;
}
