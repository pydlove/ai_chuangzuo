package com.aichuangzuo.shared.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LlmJsonParserTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void stripCodeFence_removesMarkdownFence() {
        assertEquals("{\"a\":1}", LlmJsonParser.stripCodeFence("```json\n{\"a\":1}\n```"));
    }

    @Test
    void extractJsonObject_pullsObjectFromSurroundingText() {
        String result = LlmJsonParser.extractJsonObject("好的，结果是：{\"a\":1}，请查收。");
        assertEquals("{\"a\":1}", result);
    }

    @Test
    void parseLenient_fixesUnescapedQuotesInPromptField() throws Exception {
        String raw = "{\"prompt\":\"他说 \"你好\"\"}";
        assertEquals("他说 \"你好\"", LlmJsonParser.parseLenient(mapper, raw).path("prompt").asText());
    }

    @Test
    void parseLenient_throwsOnInvalidJson() {
        assertThrows(Exception.class, () -> LlmJsonParser.parseLenient(mapper, "这不是 JSON"));
    }
}
