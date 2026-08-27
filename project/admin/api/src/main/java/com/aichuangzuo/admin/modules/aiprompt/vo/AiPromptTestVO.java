package com.aichuangzuo.admin.modules.aiprompt.vo;

import lombok.Data;

@Data
public class AiPromptTestVO {

    private String content;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private String renderedSystemRole;
    private String renderedUserPrompt;
    private Long modelConfigId;
}
