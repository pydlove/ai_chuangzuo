package com.aichuangzuo.admin.modules.generation.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PipelineUtilsTest {

    @Test
    void parseAiJson_shouldParsePlainJson() {
        JsonNode root = PipelineUtils.parseAiJson("{\"a\":1}");
        assertEquals(1, root.path("a").asInt());
    }

    @Test
    void parseAiJson_shouldStripClosedCodeFence() {
        JsonNode root = PipelineUtils.parseAiJson("```json\n{\"a\":1}\n```");
        assertEquals(1, root.path("a").asInt());
    }

    @Test
    void parseAiJson_shouldStripUnterminatedCodeFence() {
        // minimax 等模型有时只写开头 ```json 不闭合
        JsonNode root = PipelineUtils.parseAiJson("```json\n{\"a\":1}");
        assertEquals(1, root.path("a").asInt());
    }

    @Test
    void parseAiJson_shouldStripFenceWithoutLanguageTag() {
        JsonNode root = PipelineUtils.parseAiJson("```\n{\"a\":1}\n```");
        assertEquals(1, root.path("a").asInt());
    }

    @Test
    void parseAiJson_shouldThrowOnInvalidJson() {
        assertThrows(RuntimeException.class, () -> PipelineUtils.parseAiJson("not json"));
    }
}
