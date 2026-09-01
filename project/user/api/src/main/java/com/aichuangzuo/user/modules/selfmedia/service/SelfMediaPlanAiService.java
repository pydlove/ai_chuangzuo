package com.aichuangzuo.user.modules.selfmedia.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.utils.AesUtil;
import com.aichuangzuo.shared.utils.LlmJsonParser;
import com.aichuangzuo.shared.vo.AiPromptRendered;
import com.aichuangzuo.user.modules.aiprompt.service.AiPromptRenderService;
import com.aichuangzuo.user.modules.article.dto.ActiveModelConfig;
import com.aichuangzuo.user.modules.article.mapper.ArticleModelConfigMapper;
import com.aichuangzuo.shared.enums.error.SelfMediaPlanErrorCode;
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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SelfMediaPlanAiService {

    private final ArticleModelConfigMapper modelConfigMapper;
    private final AiPromptRenderService aiPromptRenderService;
    private final String apiKeySecret;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public SelfMediaPlanAiService(ArticleModelConfigMapper modelConfigMapper,
                                  AiPromptRenderService aiPromptRenderService,
                                  @Value("${user.model.api-key-secret}") String apiKeySecret,
                                  ObjectMapper objectMapper,
                                  RestTemplate restTemplate) {
        this.modelConfigMapper = modelConfigMapper;
        this.aiPromptRenderService = aiPromptRenderService;
        this.apiKeySecret = apiKeySecret;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    public JsonNode callPrompt(String promptCode, Map<String, Object> variables) {
        AiPromptRendered rendered = aiPromptRenderService.render(promptCode, variables);
        ActiveModelConfig cfg = modelConfigMapper.selectActive();
        if (cfg == null) {
            log.warn("自媒体方案 AI 调用失败：无 active 模型配置");
            throw new BusinessException(SelfMediaPlanErrorCode.SELF_MEDIA_PLAN_AI_FAILED);
        }
        String apiKey;
        try {
            apiKey = AesUtil.decrypt(cfg.getApiKeyEncrypted(), apiKeySecret);
        } catch (Exception e) {
            log.warn("自媒体方案 AI api key 解密失败", e);
            throw new BusinessException(SelfMediaPlanErrorCode.SELF_MEDIA_PLAN_AI_FAILED);
        }

        String url = resolveUrl(cfg);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", cfg.getModelCode());
        body.put("messages", List.of(
                Map.of("role", "system", "content", rendered.systemRole()),
                Map.of("role", "user", "content", rendered.userPrompt())
        ));
        body.put("temperature", 0.5);
        body.put("max_tokens", 4096);
        body.put("top_p", 1.0);
        body.put("stream", false);
        body.put("response_format", Map.of("type", "json_object"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        String content = null;
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            String responseBody = response.getBody();
            log.debug("[自媒体方案 AI] 原始响应: {}", responseBody);
            content = extractContent(responseBody, cfg.getProviderType());
            return LlmJsonParser.parseLenient(objectMapper, content);
        } catch (RestClientException e) {
            log.warn("自媒体方案 AI 调用失败 provider={} msg={}", cfg.getProviderType(), e.getMessage());
            throw new BusinessException(SelfMediaPlanErrorCode.SELF_MEDIA_PLAN_AI_FAILED);
        } catch (Exception e) {
            log.warn("自媒体方案 AI 响应解析失败，原始内容: {}", content, e);
            throw new BusinessException(SelfMediaPlanErrorCode.SELF_MEDIA_PLAN_AI_FAILED);
        }
    }

    private String resolveUrl(ActiveModelConfig cfg) {
        String base = cfg.getBaseUrl() == null ? "" : cfg.getBaseUrl().trim().replaceAll("/+$", "");
        int schemeEnd = base.indexOf("://");
        if (schemeEnd >= 0) {
            int pathStart = base.indexOf('/', schemeEnd + 3);
            if (pathStart > 0) base = base.substring(0, pathStart);
        }
        String suffix = "minimax".equalsIgnoreCase(cfg.getProviderType())
                ? "/v1/text/chatcompletion_v2"
                : "/v1/chat/completions";
        return base + suffix;
    }

    private String extractContent(String responseBody, String providerType) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                String content = choices.get(0).path("message").path("content").asText("");
                if (!content.isEmpty()) {
                    return content;
                }
            }
        } catch (Exception e) {
            log.warn("自媒体方案 AI 响应提取失败", e);
        }
        throw new BusinessException(SelfMediaPlanErrorCode.SELF_MEDIA_PLAN_AI_FAILED);
    }
}
