package com.aichuangzuo.user.modules.style.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.utils.AesUtil;
import com.aichuangzuo.user.modules.article.dto.ActiveModelConfig;
import com.aichuangzuo.user.modules.article.mapper.ArticleModelConfigMapper;
import com.aichuangzuo.user.modules.style.enums.StyleErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 风格分析的模型调用器：读 a_model_config 选 endpoint，发送 system + user messages。
 *
 * <p>仿 {@code TitleOptimizeAiService}，唯一差异：temperature=0.3（分析任务要稳定，标题优化是 0.8）。
 */
@Slf4j
@Service
public class StyleAnalyzeAiService {

    private final ArticleModelConfigMapper modelConfigMapper;
    private final String apiKeySecret;
    private final ObjectMapper objectMapper;

    public StyleAnalyzeAiService(ArticleModelConfigMapper modelConfigMapper,
                                 @Value("${user.model.api-key-secret}") String apiKeySecret) {
        this.modelConfigMapper = modelConfigMapper;
        this.apiKeySecret = apiKeySecret;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 调用当前 active 模型，返回 assistant content 原文。
     *
     * @throws BusinessException STYLE_ANALYZE_FAILED 配置缺失/传输失败/响应解析失败
     */
    public String call(String systemMessage, String userMessage) {
        ActiveModelConfig cfg = modelConfigMapper.selectActive();
        if (cfg == null) {
            log.warn("AI 风格分析失败：无 active 模型配置");
            throw new BusinessException(StyleErrorCode.STYLE_ANALYZE_FAILED);
        }
        String apiKey;
        try {
            apiKey = AesUtil.decrypt(cfg.getApiKeyEncrypted(), apiKeySecret);
        } catch (Exception e) {
            log.warn("AI 风格分析 api key 解密失败 modelConfigId={}", cfg.getId(), e);
            throw new BusinessException(StyleErrorCode.STYLE_ANALYZE_FAILED);
        }

        String url = resolveUrl(cfg);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", cfg.getModelCode());
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemMessage),
                Map.of("role", "user", "content", userMessage)
        ));
        body.put("temperature", 0.3);
        body.put("max_tokens", 4096);
        body.put("top_p", 1.0);
        body.put("stream", false);
        body.put("response_format", Map.of("type", "json_object"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);

        try {
            ResponseEntity<String> response = new RestTemplate(factory).exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            return extractContent(response.getBody(), cfg.getProviderType());
        } catch (RestClientException e) {
            log.warn("AI 风格分析调用失败 provider={} msg={}", cfg.getProviderType(), e.getMessage());
            throw new BusinessException(StyleErrorCode.STYLE_ANALYZE_FAILED);
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
                log.warn("AI 风格分析返回 content 为空 provider={} finish_reason={}",
                        providerType, choices.get(0).path("finish_reason").asText(""));
            }
        } catch (Exception e) {
            log.warn("AI 风格分析响应解析失败", e);
        }
        throw new BusinessException(StyleErrorCode.STYLE_ANALYZE_FAILED);
    }
}
