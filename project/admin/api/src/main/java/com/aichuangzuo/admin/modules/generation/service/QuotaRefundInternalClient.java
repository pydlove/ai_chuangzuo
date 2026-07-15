package com.aichuangzuo.admin.modules.generation.service;

import com.aichuangzuo.shared.enums.error.AdminGenerationErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Admin → User 内部 HTTP 客户端：退文章生成额度。
 *
 * <p>生成任务失败（异常失败或 admin 手动停止）时调用 user-api 退掉预扣的 ai_article_quota。
 */
@Slf4j
@Service
public class QuotaRefundInternalClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String internalKey;

    public QuotaRefundInternalClient(@Value("${user.api.base-url}") String baseUrl,
                                     @Value("${user.api.internal-key}") String internalKey) {
        this.baseUrl = baseUrl;
        this.internalKey = internalKey;
        this.restTemplate = new RestTemplate();
    }

    public void refund(Long taskId, Long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Key", internalKey == null ? "" : internalKey);

        try {
            @SuppressWarnings("rawtypes")
            Map body = Map.of("taskId", taskId, "userId", userId);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    baseUrl + "/api/v1/user/internal/generation/refund-quota",
                    new HttpEntity<>(body, headers),
                    Map.class);
            if (response == null || !Integer.valueOf(0).equals(response.get("code"))) {
                log.warn("退额度失败 user-api 响应: {}", response);
                throw new BusinessException(AdminGenerationErrorCode.GENERATION_OUTPUT_PARSE_FAILED);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            log.warn("调用 user-api 退额度失败：{}", e.getMessage());
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_OUTPUT_PARSE_FAILED);
        }
    }
}
