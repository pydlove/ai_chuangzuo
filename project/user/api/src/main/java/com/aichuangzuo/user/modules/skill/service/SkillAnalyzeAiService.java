package com.aichuangzuo.user.modules.skill.service;

import com.aichuangzuo.shared.ai.ActiveModelConfig;
import com.aichuangzuo.shared.ai.ModelConfigSelector;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.utils.AesUtil;
import com.aichuangzuo.user.modules.article.mapper.ArticleModelConfigMapper;
import com.aichuangzuo.shared.enums.error.SkillErrorCode;
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

/**
 * AI 风格分析的模型调用器：读 a_model_config 选 endpoint，发送 system + user messages。
 *
 * <p>仿 {@code TitleOptimizeAiService}，唯一差异：temperature=0.3（分析任务要稳定，标题优化是 0.8）。
 */
@Slf4j
@Service
public class SkillAnalyzeAiService {

    // MiniMax-M3 是推理模型，max_tokens 与 reasoning 共享预算；调大避免 content 为空。
    // GLM-4.5 同样是推理模型，reasoning 会占用输出预算，一并调大。
    private static final int MAX_TOKENS_MINIMAX = 32768;
    private static final int MAX_TOKENS_DEFAULT = 4096;

    private final ArticleModelConfigMapper modelConfigMapper;
    private final ModelConfigSelector modelConfigSelector;
    private final String apiKeySecret;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public SkillAnalyzeAiService(ArticleModelConfigMapper modelConfigMapper,
                                 ModelConfigSelector modelConfigSelector,
                                 @Value("${user.model.api-key-secret}") String apiKeySecret,
                                 RestTemplate restTemplate) {
        this.modelConfigMapper = modelConfigMapper;
        this.modelConfigSelector = modelConfigSelector;
        this.apiKeySecret = apiKeySecret;
        this.objectMapper = new ObjectMapper();
        this.restTemplate = restTemplate;
    }

    /** AI 调用失败自动重试次数：每次重试重新轮询下一个供应商/key，全部失败才抛给用户手动重试。 */
    private static final int MAX_ATTEMPTS = 3;

    /**
     * 调用当前 active 模型，返回 assistant content 原文。
     *
     * @throws BusinessException SKILL_ANALYZE_FAILED 配置缺失/传输失败/响应解析失败
     */
    public String call(String systemMessage, String userMessage) {
        BusinessException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return callOnce(systemMessage, userMessage, attempt);
            } catch (BusinessException e) {
                lastError = e;
                if (attempt < MAX_ATTEMPTS) {
                    log.warn("AI 风格分析第 {}/{} 次调用失败，切换模型配置重试", attempt, MAX_ATTEMPTS);
                }
            }
        }
        throw lastError;
    }

    private String callOnce(String systemMessage, String userMessage, int attempt) {
        ActiveModelConfig cfg = modelConfigSelector.next(modelConfigMapper.selectActiveList());
        if (cfg == null) {
            log.warn("AI 风格分析失败：无 active 模型配置");
            throw new BusinessException(SkillErrorCode.SKILL_ANALYZE_FAILED);
        }
        log.info("AI 风格分析调用模型 第 {}/{} 次 configId={} provider={} model={}",
                attempt, MAX_ATTEMPTS, cfg.getId(), cfg.getProviderType(), cfg.getModelCode());
        String apiKey;
        try {
            apiKey = AesUtil.decrypt(cfg.getApiKeyEncrypted(), apiKeySecret);
        } catch (Exception e) {
            log.warn("AI 风格分析 api key 解密失败 modelConfigId={}", cfg.getId(), e);
            throw new BusinessException(SkillErrorCode.SKILL_ANALYZE_FAILED);
        }

        String url = resolveUrl(cfg);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", cfg.getModelCode());
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemMessage),
                Map.of("role", "user", "content", userMessage)
        ));
        // kimi-for-coding 等模型限制 temperature/top_p 只能为 1，硬编码值会 400；
        // 对 kimi 不传这两个参数，让服务端用自身默认值（与 admin GenerationAiService 一致）
        if (!"kimi".equalsIgnoreCase(cfg.getProviderType())) {
            body.put("temperature", 0.3);
            body.put("top_p", 1.0);
        }
        int maxTokens = isReasoningProvider(cfg.getProviderType())
                ? MAX_TOKENS_MINIMAX : MAX_TOKENS_DEFAULT;
        body.put("max_tokens", maxTokens);
        body.put("stream", false);
        body.put("response_format", Map.of("type", "json_object"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            return extractContent(response.getBody(), cfg.getProviderType());
        } catch (RestClientException e) {
            log.warn("AI 风格分析调用失败 configId={} provider={} model={} msg={}",
                    cfg.getId(), cfg.getProviderType(), cfg.getModelCode(), e.getMessage());
            throw new BusinessException(SkillErrorCode.SKILL_ANALYZE_FAILED);
        }
    }

    private String resolveUrl(ActiveModelConfig cfg) {
        String base = cfg.getBaseUrl() == null ? "" : cfg.getBaseUrl().trim().replaceAll("/+$", "");
        String provider = cfg.getProviderType() == null ? "" : cfg.getProviderType();
        if ("glm".equalsIgnoreCase(provider)) {
            // 智谱 GLM：OpenAI 兼容，版本路径 v4；只填域名时补全官方路径 /api/paas
            base = base.replaceAll("/v\\d+$", "");
            int schemeEnd = base.indexOf("://");
            if (schemeEnd < 0 || base.indexOf('/', schemeEnd + 3) < 0) {
                base = base + "/api/paas";
            }
            return base + "/v4/chat/completions";
        }
        // 仅去掉末尾版本段（/v1 等），保留其它代理路径（如 https://api.kimi.com/coding），
        // 与 admin 端 KimiProviderClient.trim 保持一致
        base = base.replaceAll("/v\\d+/$", "").replaceAll("/v\\d+$", "");
        String suffix = "minimax".equalsIgnoreCase(provider)
                ? "/v1/text/chatcompletion_v2"
                : "/v1/chat/completions";
        return base + suffix;
    }

    /** MiniMax-M3 / GLM-4.5 等推理模型：reasoning 与 content 共享 max_tokens 预算。 */
    private static boolean isReasoningProvider(String providerType) {
        return "minimax".equalsIgnoreCase(providerType) || "glm".equalsIgnoreCase(providerType);
    }

    private String extractContent(String responseBody, String providerType) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode first = choices.get(0);
                String content = first.path("message").path("content").asText("");
                if (!content.isEmpty()) {
                    return content;
                }
                JsonNode msg = first.path("message");
                boolean hasReasoning = !msg.path("reasoning_content").asText("").isEmpty()
                        || !msg.path("reasoning_details").isMissingNode();
                log.warn("AI 风格分析返回 content 为空 provider={} finish_reason={} hasReasoning={} (length 说明 max_tokens 被 reasoning 耗尽)",
                        providerType, first.path("finish_reason").asText(""), hasReasoning);
            }
        } catch (Exception e) {
            log.warn("AI 风格分析响应解析失败", e);
        }
        throw new BusinessException(SkillErrorCode.SKILL_ANALYZE_FAILED);
    }
}
