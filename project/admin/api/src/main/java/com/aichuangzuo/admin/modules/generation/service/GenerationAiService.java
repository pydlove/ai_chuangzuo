package com.aichuangzuo.admin.modules.generation.service;

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

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ModelConfigMapper modelConfigMapper;
    private final String apiKeySecret;

    public GenerationAiService(ModelConfigMapper modelConfigMapper,
                               @Value("${admin.model.api-key-secret}") String apiKeySecret) {
        this.modelConfigMapper = modelConfigMapper;
        this.apiKeySecret = apiKeySecret;
        this.objectMapper = new ObjectMapper();
        // generation 可能慢，默认 60s 超时
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 调用模型，返回 AI 原始 assistant content（字符串）。
     */
    public String call(Long modelConfigId, String systemMessage, String userMessage) {
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
        Map<String, Object> body = Map.of(
                "model", cfg.getModelCode(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemMessage),
                        Map.of("role", "user", "content", userMessage)
                ),
                "temperature", 0.7,
                "stream", false
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            return extractAssistantContent(response.getBody(), cfg.getProviderType());
        } catch (HttpClientErrorException e) {
            log.warn("AI 调用 client error provider={} status={}", cfg.getProviderType(), e.getStatusCode());
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_AI_PROVIDER_ERROR);
        } catch (RestClientException e) {
            log.warn("AI 调用 transport error provider={} msg={}", cfg.getProviderType(), e.getMessage());
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_AI_PROVIDER_ERROR);
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

    private String extractAssistantContent(String responseBody, String providerType) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            // OpenAI 兼容 + Kimi: choices[0].message.content
            // Minimax: choices[0].message.content（通常同 schema）
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                String content = choices.get(0).path("message").path("content").asText("");
                if (!content.isEmpty()) return content;
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
}
