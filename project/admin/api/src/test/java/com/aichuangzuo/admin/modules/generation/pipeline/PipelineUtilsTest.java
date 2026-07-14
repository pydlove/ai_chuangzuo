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

    @Test
    void parseAiJson_shouldRepairInnerAsciiQuotes() {
        // 用户实际报错样本：MiniMax-M3 在 responsibility 里直接写 "30岁"，
        // 字符串值内出现裸 " 破坏 JSON 结构
        String broken = "{ \"paragraphs\": ["
                + " { \"index\": 1, \"responsibility\": \"建立共鸣入口：从\"30岁\"这一时间节点切入\" },"
                + " { \"index\": 2, \"responsibility\": \"颠覆惯性认知：拆解\"关系越亲近就越不需要边界\"的常见误区\" }"
                + " ] }";
        JsonNode root = PipelineUtils.parseAiJson(broken);
        assertEquals(2, root.path("paragraphs").size());
        assertEquals("建立共鸣入口：从\"30岁\"这一时间节点切入",
                root.path("paragraphs").get(0).path("responsibility").asText());
        assertEquals("颠覆惯性认知：拆解\"关系越亲近就越不需要边界\"的常见误区",
                root.path("paragraphs").get(1).path("responsibility").asText());
    }

    @Test
    void parseAiJson_shouldKeepValidJsonUntouched() {
        // 合法 JSON 走快速路径，不被修复逻辑改写
        String valid = "{\"a\":\"x\",\"b\":[1,2,3],\"c\":{\"d\":true}}";
        JsonNode root = PipelineUtils.parseAiJson(valid);
        assertEquals("x", root.path("a").asText());
        assertEquals(2, root.path("b").get(1).asInt());
        assertEquals(true, root.path("c").path("d").asBoolean());
    }

    @Test
    void parseAiJson_shouldPreservePreEscapedQuotes() {
        // AI 已经正确转义的情况：修复逻辑不应重复转义
        String valid = "{\"text\":\"她说 \\\"你好\\\" 然后走了\"}";
        JsonNode root = PipelineUtils.parseAiJson(valid);
        assertEquals("她说 \"你好\" 然后走了", root.path("text").asText());
    }

    @Test
    void parseAiJson_shouldThrowWhenRepairAlsoFails() {
        // 修复无能为力的情况（结构性破坏）：仍要抛异常
        assertThrows(RuntimeException.class,
                () -> PipelineUtils.parseAiJson("{\"a\": [1, 2"));
    }
}
