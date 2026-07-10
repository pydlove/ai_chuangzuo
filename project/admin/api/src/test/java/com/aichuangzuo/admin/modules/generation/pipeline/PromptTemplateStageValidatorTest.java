package com.aichuangzuo.admin.modules.generation.pipeline;

import com.aichuangzuo.shared.enums.error.AdminGenerationErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PromptTemplateStageValidator 行为测试。
 *
 * <p>覆盖：
 * <ul>
 *   <li>null / 空 map → 直接通过</li>
 *   <li>合法范围内所有组合 → 通过</li>
 *   <li>非法 key</li>
 *   <li>温度 / max_tokens / top_p 越界</li>
 *   <li>类型错误（字符串无法 parse 为数字）</li>
 * </ul>
 */
class PromptTemplateStageValidatorTest {

    @Test
    void validate_nullMap_passes() {
        assertDoesNotThrow(() -> PromptTemplateStageValidator.validate(null));
    }

    @Test
    void validate_emptyMap_passes() {
        assertDoesNotThrow(() -> PromptTemplateStageValidator.validate(Map.of()));
    }

    @Test
    void validate_allValidInRange_passes() {
        Map<String, Object> m = new HashMap<>();
        m.put("temperature", 0.7);
        m.put("max_tokens", 2000);
        m.put("top_p", 1.0);
        assertDoesNotThrow(() -> PromptTemplateStageValidator.validate(m));
    }

    @Test
    void validate_temperatureBoundary_passes() {
        assertDoesNotThrow(() -> PromptTemplateStageValidator.validate(Map.of("temperature", 0.0)));
        assertDoesNotThrow(() -> PromptTemplateStageValidator.validate(Map.of("temperature", 2.0)));
    }

    @Test
    void validate_maxTokensBoundary_passes() {
        assertDoesNotThrow(() -> PromptTemplateStageValidator.validate(Map.of("max_tokens", 1)));
        assertDoesNotThrow(() -> PromptTemplateStageValidator.validate(Map.of("max_tokens", 8000)));
    }

    @Test
    void validate_unknownKey_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> PromptTemplateStageValidator.validate(Map.of("frequency_penalty", 0.5)));
        assertEquals(AdminGenerationErrorCode.GENERATION_MODEL_PARAMS_INVALID.getCode(),
                ex.getCode());
    }

    @Test
    void validate_temperatureTooHigh_throws() {
        assertThrows(BusinessException.class,
                () -> PromptTemplateStageValidator.validate(Map.of("temperature", 2.1)));
    }

    @Test
    void validate_temperatureNegative_throws() {
        assertThrows(BusinessException.class,
                () -> PromptTemplateStageValidator.validate(Map.of("temperature", -0.1)));
    }

    @Test
    void validate_maxTokensTooLarge_throws() {
        assertThrows(BusinessException.class,
                () -> PromptTemplateStageValidator.validate(Map.of("max_tokens", 10000)));
    }

    @Test
    void validate_maxTokensZero_throws() {
        assertThrows(BusinessException.class,
                () -> PromptTemplateStageValidator.validate(Map.of("max_tokens", 0)));
    }

    @Test
    void validate_topPOverOne_throws() {
        assertThrows(BusinessException.class,
                () -> PromptTemplateStageValidator.validate(Map.of("top_p", 1.1)));
    }

    @Test
    void validate_nullValue_throws() {
        Map<String, Object> m = new HashMap<>();
        m.put("temperature", null);
        assertThrows(BusinessException.class,
                () -> PromptTemplateStageValidator.validate(m));
    }

    @Test
    void validate_stringNumberParses_passes() {
        // 数字用字符串形式传入也能解析
        assertDoesNotThrow(() -> PromptTemplateStageValidator.validate(Map.of("temperature", "0.5")));
        assertDoesNotThrow(() -> PromptTemplateStageValidator.validate(Map.of("max_tokens", "1024")));
    }

    @Test
    void validate_stringNumberOutOfRange_throws() {
        assertThrows(BusinessException.class,
                () -> PromptTemplateStageValidator.validate(Map.of("max_tokens", "9999")));
    }

    @Test
    void validate_invalidStringNumber_throws() {
        assertThrows(BusinessException.class,
                () -> PromptTemplateStageValidator.validate(Map.of("temperature", "abc")));
    }

    @Test
    void validate_typedWrong_throws() {
        // 传 boolean 不会强转
        Map<String, Object> m = new HashMap<>();
        m.put("temperature", true);
        assertThrows(BusinessException.class,
                () -> PromptTemplateStageValidator.validate(m));
    }

    @Test
    void validateJson_valid_passes() {
        assertDoesNotThrow(() -> PromptTemplateStageValidator.validateJson(
                "{\"temperature\":0.7,\"max_tokens\":2000}"));
    }

    @Test
    void validateJson_nullOrBlank_passes() {
        assertDoesNotThrow(() -> PromptTemplateStageValidator.validateJson(null));
        assertDoesNotThrow(() -> PromptTemplateStageValidator.validateJson(""));
        assertDoesNotThrow(() -> PromptTemplateStageValidator.validateJson("   "));
    }

    @Test
    void validateJson_invalidJson_throws() {
        assertThrows(BusinessException.class,
                () -> PromptTemplateStageValidator.validateJson("{bad json}"));
    }

    @Test
    void validateJson_unknownKey_throws() {
        assertThrows(BusinessException.class,
                () -> PromptTemplateStageValidator.validateJson(
                        "{\"temperature\":0.7,\"mystery\":1}"));
    }
}
