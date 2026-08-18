package com.aichuangzuo.admin.modules.aiprompt.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AiPromptDetailVO {

    private Long id;
    private String promptCode;
    private String promptName;
    private String module;
    private String category;
    private String systemRole;
    private String userPrompt;
    private List<AiPromptVariableVO> variableSchema;
    private Integer status;
    private Integer sortOrder;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    public static class AiPromptVariableVO {
        private String name;
        private Boolean required;
        private String description;
        private String example;
    }
}
