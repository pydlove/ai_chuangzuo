package com.aichuangzuo.admin.modules.generation.pipeline;

import com.aichuangzuo.shared.enums.error.AdminGenerationErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;

/**
 * 校验 {@code t_prompt_template_stage.model_params} JSON 内容。
 *
 * <p>仅允许以下白名单 key，且数值在合理范围内：
 * <ul>
 *   <li>{@code temperature}: 0.0 - 2.0</li>
 *   <li>{@code max_tokens}: 1 - 8000</li>
 *   <li>{@code top_p}: 0.0 - 1.0</li>
 * </ul>
 *
 * <p>任何越界 / 非法 key / 类型不匹配 → 抛 {@code BusinessException(GENERATION_MODEL_PARAMS_INVALID)}。
 *
 * <p>为什么单独拎出来：12 阶段配置由管理员在 Web 后台编辑，参数错配会导致 AI 调用代价飙升
 * （如 max_tokens=100000 单次 1 美元）或完全失去随机性（temperature=0 + top_p=0），
 * 需要在校验阶段就拦截，而不是等到任务跑了才发现。
 */
@Slf4j
public final class PromptTemplateStageValidator {

    private static final Set<String> ALLOWED_KEYS = Set.of("temperature", "max_tokens", "top_p");

    private static final double TEMP_MIN = 0.0;
    private static final double TEMP_MAX = 2.0;
    private static final int MAX_TOKENS_MIN = 1;
    private static final int MAX_TOKENS_MAX = 8000;
    private static final double TOP_P_MIN = 0.0;
    private static final double TOP_P_MAX = 1.0;

    private PromptTemplateStageValidator() {
    }

    /**
     * 校验内存中的 params map（直接由 DTO 传入）。
     *
     * @param params 可为 null 或空（视为「使用全局默认」）
     * @throws BusinessException 校验失败
     */
    public static void validate(Map<String, Object> params) {
        if (params == null || params.isEmpty()) return;
        for (Map.Entry<String, Object> e : params.entrySet()) {
            String key = e.getKey();
            if (!ALLOWED_KEYS.contains(key)) {
                log.warn("modelParams 含非法 key={}", key);
                throw new BusinessException(AdminGenerationErrorCode.GENERATION_MODEL_PARAMS_INVALID);
            }
            Object v = e.getValue();
            if (v == null) {
                log.warn("modelParams.{} 为 null", key);
                throw new BusinessException(AdminGenerationErrorCode.GENERATION_MODEL_PARAMS_INVALID);
            }
            switch (key) {
                case "temperature":
                    ensureRange(key, toDouble(key, v), TEMP_MIN, TEMP_MAX);
                    break;
                case "max_tokens":
                    int iv = toInt(key, v);
                    ensureRange(key, iv, MAX_TOKENS_MIN, MAX_TOKENS_MAX);
                    break;
                case "top_p":
                    ensureRange(key, toDouble(key, v), TOP_P_MIN, TOP_P_MAX);
                    break;
                default:
                    // 理论不可达（已被 ALLOWED_KEYS 过滤）
                    break;
            }
        }
    }

    /**
     * 校验数据库里的 JSON 字符串（t_prompt_template_stage.model_params）。
     *
     * @param modelParamsJson 可为 null 或空白（视为「使用全局默认」）
     * @throws BusinessException 解析或内容校验失败
     */
    public static void validateJson(String modelParamsJson) {
        if (modelParamsJson == null || modelParamsJson.isBlank()) return;
        java.util.Map<String, Object> map;
        try {
            map = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(modelParamsJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception ex) {
            log.warn("modelParams JSON 解析失败: {}", ex.getMessage());
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_MODEL_PARAMS_INVALID);
        }
        validate(map);
    }

    private static double toDouble(String key, Object v) {
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException e) {
                log.warn("modelParams.{} 无法解析为数字: {}", key, s);
                throw new BusinessException(AdminGenerationErrorCode.GENERATION_MODEL_PARAMS_INVALID);
            }
        }
        log.warn("modelParams.{} 类型非法: {}", key, v.getClass().getSimpleName());
        throw new BusinessException(AdminGenerationErrorCode.GENERATION_MODEL_PARAMS_INVALID);
    }

    private static int toInt(String key, Object v) {
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException e) {
                log.warn("modelParams.{} 无法解析为整数: {}", key, s);
                throw new BusinessException(AdminGenerationErrorCode.GENERATION_MODEL_PARAMS_INVALID);
            }
        }
        log.warn("modelParams.{} 类型非法: {}", key, v.getClass().getSimpleName());
        throw new BusinessException(AdminGenerationErrorCode.GENERATION_MODEL_PARAMS_INVALID);
    }

    private static void ensureRange(String key, double v, double min, double max) {
        if (v < min || v > max) {
            log.warn("modelParams.{}={} 超出范围 [{}, {}]", key, v, min, max);
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_MODEL_PARAMS_INVALID);
        }
    }
}
