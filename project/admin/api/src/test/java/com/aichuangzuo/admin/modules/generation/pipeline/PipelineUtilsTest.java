package com.aichuangzuo.admin.modules.generation.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void parseAiJson_shouldTolerateLiteralNewlineInString() {
        // 用户实际报错样本：stage 3 material-list 的字符串值里夹了字面换行符
        // （CTRL-CHAR code 10），Jackson 严格模式拒绝。应容忍并读成 \n
        String broken = "{\"description\": \"第一行\n第二行\"}";
        JsonNode root = PipelineUtils.parseAiJson(broken);
        assertEquals("第一行\n第二行", root.path("description").asText());
    }

    @Test
    void parseAiJson_shouldTolerateLiteralTabInString() {
        // 制表符（code 9）同属未转义控制字符，一并容忍
        String broken = "{\"description\": \"列一\t列二\"}";
        JsonNode root = PipelineUtils.parseAiJson(broken);
        assertEquals("列一\t列二", root.path("description").asText());
    }

    @Test
    void parseAiJson_shouldHintOnTruncation() {
        // AI 输出被 max_tokens 截断（JSON 在字段名中间戛然而止）：解析不可恢复，
        // 但要让 admin/用户一眼看出是截断，调大 max_tokens 即可
        String truncated = "{\"draft\":[{\"paragraph_index\":1,\"para";
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> PipelineUtils.parseAiJson(truncated));
        assertEquals(true, ex.getMessage().contains("截断") || ex.getMessage().contains("max_tokens"),
                "Expected truncation hint, got: " + ex.getMessage());
    }

    @Test
    void parseAiJson_shouldAcceptSingleQuotedStrings() {
        // 第 4 类瑕疵：M3 在某些字段（中文术语 like 'AI写作变现'/'数据'）用 '...'
        // 单引号当定界符（Python literal 风格），Jackson 严格模式认作 expected a value
        String singleQuoted = "{'paragraph_index':1,'responsibility':'用反差感开场','items':[1,'two',3]}";
        JsonNode root = PipelineUtils.parseAiJson(singleQuoted);
        assertEquals(1, root.path("paragraph_index").asInt());
        assertEquals("用反差感开场", root.path("responsibility").asText());
        assertEquals("two", root.path("items").get(1).asText());
    }

    @Test
    void parseAiJson_shouldAcceptUnquotedFieldNames() {
        // 第 5 类瑕疵：M3 偶尔用裸 key（Python/JS literal 风格 `{a:1}`）
        String unquoted = "{paragraph_index:1, responsibility:\"用反差感开场\"}";
        JsonNode root = PipelineUtils.parseAiJson(unquoted);
        assertEquals(1, root.path("paragraph_index").asInt());
        assertEquals("用反差感开场", root.path("responsibility").asText());
    }

    @Test
    void normalizeQuotes_shouldReplaceChineseSingleQuotes() {
        assertEquals("她说“你好”然后走了", PipelineUtils.normalizeQuotes("她说‘你好’然后走了"));
        assertEquals("“躺平”不是“摆烂”", PipelineUtils.normalizeQuotes("‘躺平’不是‘摆烂’"));
    }

    @Test
    void normalizeQuotes_shouldKeepAsciiApostrophe() {
        // ASCII ' 可能是英文撇号（Don't / it's），无法和引号区分，不动
        assertEquals("Don't stop, it's fine", PipelineUtils.normalizeQuotes("Don't stop, it's fine"));
    }

    @Test
    void normalizeQuotes_shouldKeepDoubleQuotesUntouched() {
        assertEquals("已是“双引号”", PipelineUtils.normalizeQuotes("已是“双引号”"));
    }

    @Test
    void normalizeQuotes_shouldHandleNullAndEmpty() {
        assertNull(PipelineUtils.normalizeQuotes(null));
        assertEquals("", PipelineUtils.normalizeQuotes(""));
    }
}
