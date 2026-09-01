package com.aichuangzuo.shared.utils;

import com.fasterxml.jackson.databind.JsonNode;
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
    void parseLenient_fixesUnescapedQuotesInTitleField() throws Exception {
        String raw = "{\"mainPlatform\":{\"platform\":\"小红书\",\"publishTime\":\"每晚 20:00\",\"reason\":\"\"},\"coldStart\":{\"immediateActions\":[],\"duration\":\"\",\"sharingTips\":\"\"},\"reposts\":[{\"platform\":\"知乎\",\"publishTime\":\"当天 21:00\",\"title\":\"他说 \"这3个方法\" 真的有效\",\"tags\":[],\"imageSuggestions\":\"\",\"tips\":\"\"}]}";
        JsonNode node = LlmJsonParser.parseLenient(mapper, raw);
        assertEquals("他说 \"这3个方法\" 真的有效", node.path("reposts").get(0).path("title").asText());
    }

    @Test
    void parseLenient_fixesUnescapedQuotesInTipsField() throws Exception {
        String raw = "{\"reposts\":[{\"tips\":\"开头可以写：\"你是否也遇到过\"，引导共鸣\"}]}";
        JsonNode node = LlmJsonParser.parseLenient(mapper, raw);
        assertEquals("开头可以写：\"你是否也遇到过\"，引导共鸣", node.path("reposts").get(0).path("tips").asText());
    }

    @Test
    void parseLenient_keepsValidJsonUnchanged() throws Exception {
        String raw = "{\"title\":\"正常标题\",\"tags\":[\"tag1\",\"tag2\"]}";
        JsonNode node = LlmJsonParser.parseLenient(mapper, raw);
        assertEquals("正常标题", node.path("title").asText());
        assertEquals(2, node.path("tags").size());
    }

    @Test
    void parseLenient_extractsAndFixesUnescapedQuotes() throws Exception {
        String raw = "好的，这是发布计划：{\"title\":\"他说 \"你好\"\"}。请查收。";
        JsonNode node = LlmJsonParser.parseLenient(mapper, raw);
        assertEquals("他说 \"你好\"", node.path("title").asText());
    }

    @Test
    void parseLenient_extractsLastValidObjectWhenMultipleJsonBlocksExist() throws Exception {
        String raw = "示例：{\"questions\":[{\"key\":\"example\",\"text\":\"示例问题\"}]}\n最终输出：{\"questions\":[{\"key\":\"real\",\"text\":\"真实问题\"}]}";
        JsonNode node = LlmJsonParser.parseLenient(mapper, raw);
        assertEquals("real", node.path("questions").get(0).path("key").asText());
        assertEquals("真实问题", node.path("questions").get(0).path("text").asText());
    }

    @Test
    void parseLenient_extractsTopLevelArray() throws Exception {
        String raw = "下面是结果：[{\"key\":\"q1\"},{\"key\":\"q2\"}]";
        JsonNode node = LlmJsonParser.parseLenient(mapper, raw);
        assertTrue(node.isArray());
        assertEquals(2, node.size());
        assertEquals("q2", node.get(1).path("key").asText());
    }

    @Test
    void parseLenient_unwrapsJsonStringWrapper() throws Exception {
        String inner = "{\"questions\":[{\"key\":\"q1\"}]}";
        String raw = mapper.writeValueAsString(inner);
        JsonNode node = LlmJsonParser.parseLenient(mapper, raw);
        assertEquals("q1", node.path("questions").get(0).path("key").asText());
    }

    @Test
    void parseLenient_allowsTrailingComma() throws Exception {
        String raw = "{\"questions\":[{\"key\":\"q1\",},],}";
        JsonNode node = LlmJsonParser.parseLenient(mapper, raw);
        assertEquals("q1", node.path("questions").get(0).path("key").asText());
    }

    @Test
    void parseLenient_throwsOnInvalidJson() {
        assertThrows(Exception.class, () -> LlmJsonParser.parseLenient(mapper, "这不是 JSON"));
    }
}
