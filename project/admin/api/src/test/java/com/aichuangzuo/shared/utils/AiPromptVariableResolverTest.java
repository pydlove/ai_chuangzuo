package com.aichuangzuo.shared.utils;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AiPromptVariableResolverTest {

    @Test
    void shouldExtractVariables() {
        String text = "请生成 {{count}} 条标题，方向：{{direction}}。";
        Set<String> vars = AiPromptVariableResolver.extractVariables(text);
        assertEquals(Set.of("count", "direction"), vars);
    }

    @Test
    void shouldRenderVariables() {
        String template = "请生成 {{count}} 条标题，方向：{{direction}}。";
        String result = AiPromptVariableResolver.render(template, Map.of("count", 10, "direction", "职场"));
        assertEquals("请生成 10 条标题，方向：职场。", result);
    }

    @Test
    void shouldRenderMissingVariableAsEmptyString() {
        String template = "标题：{{title}}，备注：{{remark}}。";
        String result = AiPromptVariableResolver.render(template, Map.of("title", "A"));
        assertEquals("标题：A，备注：。", result);
    }

    @Test
    void shouldHandleNoVariables() {
        String template = "没有变量的纯文本。";
        assertTrue(AiPromptVariableResolver.extractVariables(template).isEmpty());
        assertEquals(template, AiPromptVariableResolver.render(template, Map.of()));
    }
}
