package com.aichuangzuo.admin.modules.generation.service;

import com.aichuangzuo.admin.modules.generation.entity.GenerationConfig;
import com.aichuangzuo.admin.modules.modelconfig.entity.ModelConfig;
import com.aichuangzuo.admin.modules.modelconfig.mapper.ModelConfigMapper;
import com.aichuangzuo.shared.enums.error.AdminGenerationErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.utils.AesUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * 文章生成 AI 调度器：根据 model_config 选 Kimi / Minimax endpoint，发送 system + user messages。
 *
 * <p>故意独立于 {@code AiProviderClient} 接口，避免改动 modelconfig 模块现有代码。
 */
@Slf4j
@Service
public class GenerationAiService {

    private final ObjectMapper objectMapper;
    private final ModelConfigMapper modelConfigMapper;
    private final GenerationConfigService generationConfigService;
    private final String apiKeySecret;

    public GenerationAiService(ModelConfigMapper modelConfigMapper,
                               GenerationConfigService generationConfigService,
                               @Value("${admin.model.api-key-secret}") String apiKeySecret) {
        this.modelConfigMapper = modelConfigMapper;
        this.generationConfigService = generationConfigService;
        this.apiKeySecret = apiKeySecret;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 调用模型，返回 AI 原始 assistant content + token 消耗。
     *
     * @param modelParams 可选；非空时 merge 进请求体（覆盖默认 temperature）
     */
    public AiCallResult call(Long modelConfigId, String systemMessage, String userMessage,
                       Map<String, Object> modelParams) {
        ModelConfig cfg = modelConfigMapper.selectById(modelConfigId);
        if (cfg == null || cfg.getIsActive() == null || cfg.getIsActive() != 1) {
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_AI_PROVIDER_ERROR);
        }
        String apiKey;
        try {
            apiKey = AesUtil.decrypt(cfg.getApiKeyEncrypted(), apiKeySecret);
        } catch (Exception e) {
            log.warn("api key 解密失败 modelConfigId={}", modelConfigId, e);
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_AI_PROVIDER_ERROR);
        }

        String url = resolveUrl(cfg);

        // 三级回退：stage model_params > 创作设置全局默认 > 硬编码兜底
        GenerationConfig genCfg = generationConfigService.getCurrent();
        double temperatureDefault = genCfg.getDefaultTemperature() != null
                ? genCfg.getDefaultTemperature().doubleValue() : 0.7;
        // 32768：MiniMax-M3 是推理模型，max_tokens 与 reasoning 共享预算。
        // 整稿改写/润色类 stage（rhythm-rewrite、rhythm-polish）reasoning + 全文 rewrite
        // 输出合计常超 16k，曾导致 JSON 在字段中间被截断。
        int maxTokensDefault = genCfg.getDefaultMaxTokens() != null
                ? genCfg.getDefaultMaxTokens() : 32768;
        double topPDefault = genCfg.getDefaultTopP() != null
                ? genCfg.getDefaultTopP().doubleValue() : 1.0;

        // 请求体改用 LinkedHashMap，允许覆盖默认值；Map.of() 不允许 null 值
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("model", cfg.getModelCode());
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemMessage),
                Map.of("role", "user", "content", userMessage)
        ));
        body.put("temperature", pickDouble(modelParams, "temperature", temperatureDefault));
        body.put("max_tokens", pickInt(modelParams, "max_tokens", maxTokensDefault));
        body.put("top_p", pickDouble(modelParams, "top_p", topPDefault));
        body.put("stream", false);
        // API 层强制 JSON 模式：让模型必须输出合法 JSON，不再裸 prose。
        // 多数 OpenAI 兼容厂商（含 chatcompletion_v2）支持；不支持时会静默忽略，
        // 不至于报错崩任务。配合解析器侧开启 ALLOW_UNQUOTED_FIELD_NAMES 等
        // 兜底，覆盖绝大多数 M3 输出瑕疵。
        body.put("response_format", Map.of("type", "json_object"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            ResponseEntity<String> response = getRestTemplate().exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            return extractResult(response.getBody(), cfg.getProviderType());
        } catch (HttpClientErrorException e) {
            log.warn("AI 调用 client error provider={} status={}", cfg.getProviderType(), e.getStatusCode());
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_AI_PROVIDER_ERROR);
        } catch (RestClientException e) {
            log.warn("AI 调用 transport error provider={} msg={}", cfg.getProviderType(), e.getMessage());
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_AI_PROVIDER_ERROR);
        }
    }

    /** 保留旧 3 参签名（向后兼容），内部 delegate 到 4 参版本。 */
    public AiCallResult call(Long modelConfigId, String systemMessage, String userMessage) {
        return call(modelConfigId, systemMessage, userMessage, null);
    }

    /** 测试用：暴露 RestTemplate 给子类 override。 */
    protected RestTemplate getRestTemplate() {
        GenerationConfig genCfg = generationConfigService.getCurrent();
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        int readTimeoutSec = genCfg.getAiReadTimeoutSeconds() != null
                ? genCfg.getAiReadTimeoutSeconds() : 180;
        factory.setReadTimeout(readTimeoutSec * 1000);
        return new RestTemplate(factory);
    }

    private static Double pickDouble(Map<String, Object> params, String key, double def) {
        if (params == null) return def;
        Object v = params.get(key);
        if (v == null) return def;
        if (v instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(v.toString());
        } catch (NumberFormatException e) {
            log.warn("AI 参数 {} 不是合法数字，使用默认 {}: value={}", key, def, v);
            return def;
        }
    }

    private static Integer pickInt(Map<String, Object> params, String key, int def) {
        if (params == null) return def;
        Object v = params.get(key);
        if (v == null) return def;
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            log.warn("AI 参数 {} 不是合法整数，使用默认 {}: value={}", key, def, v);
            return def;
        }
    }

    private String resolveUrl(ModelConfig cfg) {
        String base = cfg.getBaseUrl() == null ? "" : cfg.getBaseUrl().trim().replaceAll("/+$", "");
        int schemeEnd = base.indexOf("://");
        if (schemeEnd >= 0) {
            int pathStart = base.indexOf('/', schemeEnd + 3);
            if (pathStart > 0) base = base.substring(0, pathStart);
        }
        String suffix = switch (cfg.getProviderType() == null ? "" : cfg.getProviderType().toLowerCase()) {
            case "minimax" -> "/v1/text/chatcompletion_v2";
            default -> "/v1/chat/completions"; // kimi 等 OpenAI 兼容
        };
        return base + suffix;
    }

    private AiCallResult extractResult(String responseBody, String providerType) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            // OpenAI 兼容 + Kimi: choices[0].message.content
            // Minimax: choices[0].message.content（通常同 schema）
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode first = choices.get(0);
                String content = first.path("message").path("content").asText("");
                if (!content.isEmpty()) {
                    return new AiCallResult(content,
                            readIntOrNull(root.path("usage").path("prompt_tokens")),
                            readIntOrNull(root.path("usage").path("completion_tokens")),
                            readIntOrNull(root.path("usage").path("total_tokens")));
                }
                // content 为空：打印 finish_reason + 是否带 reasoning，便于定位
                // MiniMax-M3 等推理模型 max_tokens 被 reasoning 吃光时，
                // finish_reason=length 且 content 空、reasoning_content 非空
                String finishReason = first.path("finish_reason").asText("");
                JsonNode msg = first.path("message");
                boolean hasReasoning = !msg.path("reasoning_content").asText("").isEmpty()
                        || !msg.path("reasoning_details").isMissingNode();
                log.warn("AI 返回 content 为空 provider={} finish_reason={} hasReasoning={} (若是 length，说明 max_tokens 被 reasoning 耗尽，需调大)",
                        providerType, finishReason, hasReasoning);
            }
            // Minimax 有时返回 base_resp：失败但 HTTP 200
            JsonNode baseResp = root.path("base_resp");
            if (!baseResp.isMissingNode()) {
                int code = baseResp.path("status_code").asInt(-1);
                if (code != 0) {
                    log.warn("Minimax 业务失败 base_resp={}", baseResp);
                    throw new BusinessException(AdminGenerationErrorCode.GENERATION_AI_PROVIDER_ERROR);
                }
            }
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_OUTPUT_PARSE_FAILED);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("解析 AI 响应失败", e);
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_OUTPUT_PARSE_FAILED);
        }
    }

    /** usage 字段缺失或非数字时返回 null（部分 provider 不上报）。 */
    private static Integer readIntOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        if (node.isNumber()) return node.intValue();
        try {
            return Integer.parseInt(node.asText(""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
