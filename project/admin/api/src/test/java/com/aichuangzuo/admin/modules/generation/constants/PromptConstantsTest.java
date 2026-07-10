package com.aichuangzuo.admin.modules.generation.constants;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptConstantsTest {

    @Test
    void systemPromptJson_shouldNotBeNullOrBlank() {
        assertNotNull(PromptConstants.SYSTEM_PROMPT_JSON);
        assertFalse(PromptConstants.SYSTEM_PROMPT_JSON.isBlank());
    }

    @Test
    void systemPromptJson_shouldContainRequiredSchemaKeywords() {
        String s = PromptConstants.SYSTEM_PROMPT_JSON;
        assertAll(
                () -> assertTrue(s.contains("title"), "应包含 title 字段定义"),
                () -> assertTrue(s.contains("summary"), "应包含 summary 字段定义"),
                () -> assertTrue(s.contains("sections"), "应包含 sections 字段定义"),
                () -> assertTrue(s.contains("paragraphs"), "应包含 paragraphs 字段定义"),
                () -> assertTrue(s.contains("imageHints"), "应包含 imageHints 字段定义"),
                () -> assertTrue(s.contains("meta"), "应包含 meta 字段定义")
        );
    }

    @Test
    void systemPromptJson_shouldNotContainLegacyFieldNames() {
        String s = PromptConstants.SYSTEM_PROMPT_JSON;
        assertAll(
                () -> assertFalse(s.contains("user_style_guidance"),
                        "不应再提及 user_style_guidance（已从模板移除）"),
                () -> assertFalse(s.contains("system_prompt_json"),
                        "不应再提及 system_prompt_json（已从模板移除）")
        );
    }
}
