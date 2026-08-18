package com.aichuangzuo.admin.modules.aiprompt.service;

import com.aichuangzuo.admin.modules.aiprompt.mapper.AiPromptMapper;
import com.aichuangzuo.admin.modules.aiprompt.service.impl.AiPromptRenderServiceImpl;
import com.aichuangzuo.shared.entity.AiPrompt;
import com.aichuangzuo.shared.enums.error.AdminAiPromptErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.vo.AiPromptRendered;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiPromptRenderServiceTest {

    private final AiPromptMapper mapper = mock(AiPromptMapper.class);
    private final AiPromptRenderService service = new AiPromptRenderServiceImpl(mapper, new ObjectMapper());

    @Test
    void shouldRenderPrompt() {
        AiPrompt prompt = new AiPrompt();
        prompt.setPromptCode("test_v1");
        prompt.setStatus(1);
        prompt.setIsDeleted(0);
        prompt.setSystemRole("你是 {{role}}。");
        prompt.setUserPrompt("请生成 {{count}} 条。");
        prompt.setVariableSchema("[{\"name\":\"role\",\"required\":true},{\"name\":\"count\",\"required\":true}]");

        when(mapper.selectOne(any())).thenReturn(prompt);

        AiPromptRendered rendered = service.render("test_v1", Map.of("role", "专家", "count", 5));

        assertEquals("你是 专家。", rendered.systemRole());
        assertEquals("请生成 5 条。", rendered.userPrompt());
    }

    @Test
    void shouldThrowWhenRequiredVariableMissing() {
        AiPrompt prompt = new AiPrompt();
        prompt.setPromptCode("test_v1");
        prompt.setStatus(1);
        prompt.setIsDeleted(0);
        prompt.setUserPrompt("请生成 {{count}} 条。");
        prompt.setVariableSchema("[{\"name\":\"count\",\"required\":true}]");

        when(mapper.selectOne(any())).thenReturn(prompt);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.render("test_v1", Map.of()));
        assertEquals(AdminAiPromptErrorCode.AI_PROMPT_VARIABLE_MISSING.getCode(), ex.getCode());
    }

    @Test
    void shouldThrowWhenPromptNotFound() {
        when(mapper.selectOne(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.render("missing_v1", Map.of()));
        assertEquals(AdminAiPromptErrorCode.AI_PROMPT_NOT_FOUND.getCode(), ex.getCode());
    }
}
