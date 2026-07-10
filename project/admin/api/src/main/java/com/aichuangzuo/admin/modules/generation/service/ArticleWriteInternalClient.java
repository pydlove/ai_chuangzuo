package com.aichuangzuo.admin.modules.generation.service;

import com.aichuangzuo.shared.enums.error.AdminGenerationErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Admin → User 内部 HTTP 客户端：让 user-api 写 u_article。
 *
 * <p>调用 user-api 的 {@code /api/v1/user/internal/generation/save-article}，
 * 通过 {@code X-Internal-Key} 走 InternalKeyAuthenticationFilter 校验。
 */
@Slf4j
@Service
public class ArticleWriteInternalClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String internalKey;

    public ArticleWriteInternalClient(@Value("${user.api.base-url}") String baseUrl,
                                     @Value("${user.api.internal-key}") String internalKey) {
        this.baseUrl = baseUrl;
        this.internalKey = internalKey;
        this.restTemplate = new RestTemplate();
    }

    public String saveArticle(Map<String, Object> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Key", internalKey == null ? "" : internalKey);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/api/v1/user/internal/generation/save-article",
                    new HttpEntity<>(payload, headers),
                    Map.class);
            Map<?, ?> body = response.getBody();
            if (body == null || !Integer.valueOf(0).equals(body.get("code"))) {
                log.warn("保存 article 失败 user-api 响应: {}", body);
                throw new BusinessException(AdminGenerationErrorCode.GENERATION_ARTICLE_PERSIST_FAILED);
            }
            Object data = body.get("data");
            if (data == null) {
                throw new BusinessException(AdminGenerationErrorCode.GENERATION_ARTICLE_PERSIST_FAILED);
            }
            return data.toString();
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            log.warn("调用 user-api 写 article 失败：{}", e.getMessage());
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_ARTICLE_PERSIST_FAILED);
        }
    }
}
