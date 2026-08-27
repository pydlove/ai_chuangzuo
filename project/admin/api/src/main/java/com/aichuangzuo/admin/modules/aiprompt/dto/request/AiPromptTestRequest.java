package com.aichuangzuo.admin.modules.aiprompt.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class AiPromptTestRequest {

    @NotNull(message = "变量参数不能为空")
    private Map<String, Object> variables;
}
