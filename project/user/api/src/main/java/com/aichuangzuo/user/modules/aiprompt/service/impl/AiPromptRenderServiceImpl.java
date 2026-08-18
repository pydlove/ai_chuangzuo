package com.aichuangzuo.user.modules.aiprompt.service.impl;

import com.aichuangzuo.shared.entity.AiPrompt;
import com.aichuangzuo.shared.enums.error.AdminAiPromptErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.utils.AiPromptVariableResolver;
import com.aichuangzuo.shared.vo.AiPromptRendered;
import com.aichuangzuo.user.modules.aiprompt.mapper.AiPromptMapper;
import com.aichuangzuo.user.modules.aiprompt.service.AiPromptRenderService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiPromptRenderServiceImpl implements AiPromptRenderService {

    private final AiPromptMapper aiPromptMapper;
    private final ObjectMapper objectMapper;

    @Override
    public AiPromptRendered render(String promptCode, Map<String, Object> variables) {
        if (StringUtils.isBlank(promptCode)) {
            throw new BusinessException(AdminAiPromptErrorCode.AI_PROMPT_RENDER_ERROR);
        }
        AiPrompt prompt = aiPromptMapper.selectActiveByCode(promptCode.trim());
        if (prompt == null) {
            log.warn("AI 提示词不存在或已停用, promptCode={}", promptCode);
            throw new BusinessException(AdminAiPromptErrorCode.AI_PROMPT_NOT_FOUND);
        }
        return renderInternal(prompt, variables == null ? Map.of() : variables);
    }

    private AiPromptRendered renderInternal(AiPrompt prompt, Map<String, Object> variables) {
        Set<String> requiredVariables = parseRequiredVariables(prompt.getVariableSchema());
        Set<String> missing = new LinkedHashSet<>();
        for (String name : requiredVariables) {
            Object value = variables.get(name);
            if (value == null || StringUtils.isBlank(value.toString())) {
                missing.add(name);
            }
        }
        if (!missing.isEmpty()) {
            log.warn("AI 提示词必填变量缺失, promptCode={}, missing={}", prompt.getPromptCode(), missing);
            throw new BusinessException(AdminAiPromptErrorCode.AI_PROMPT_VARIABLE_MISSING);
        }

        String systemRole = AiPromptVariableResolver.render(prompt.getSystemRole(), variables);
        String userPrompt = AiPromptVariableResolver.render(prompt.getUserPrompt(), variables);
        return new AiPromptRendered(systemRole, userPrompt);
    }

    @SneakyThrows
    private Set<String> parseRequiredVariables(String json) {
        if (StringUtils.isBlank(json)) {
            return Set.of();
        }
        List<Map<String, Object>> list = objectMapper.readValue(json, new TypeReference<>() {});
        Set<String> required = new LinkedHashSet<>();
        for (Map<String, Object> item : list) {
            Boolean requiredFlag = (Boolean) item.get("required");
            if (Boolean.TRUE.equals(requiredFlag)) {
                String name = (String) item.get("name");
                if (StringUtils.isNotBlank(name)) {
                    required.add(name);
                }
            }
        }
        return required;
    }
}
