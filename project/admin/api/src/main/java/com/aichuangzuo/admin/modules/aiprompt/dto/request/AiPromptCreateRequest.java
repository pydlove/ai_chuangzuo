package com.aichuangzuo.admin.modules.aiprompt.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AiPromptCreateRequest {

    @NotBlank
    @Size(max = 64)
    private String promptCode;

    @NotBlank
    @Size(max = 128)
    private String promptName;

    @NotBlank
    private String module;

    @Size(max = 64)
    private String category;

    private String systemRole;

    @NotBlank
    private String userPrompt;

    private List<AiPromptVariableRequest> variableSchema;

    @NotNull
    private Integer status;

    private Integer sortOrder;

    @Size(max = 500)
    private String description;

    @Data
    public static class AiPromptVariableRequest {
        private String name;
        private Boolean required;
        private String description;
        private String example;
    }
}
