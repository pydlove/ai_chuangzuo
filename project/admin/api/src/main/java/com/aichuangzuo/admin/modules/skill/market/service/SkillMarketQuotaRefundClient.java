package com.aichuangzuo.admin.modules.skill.market.service;

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
 * Admin → User 内部 HTTP 客户端：退提示词市场发布额度。
 *
 * <p>管理端审核打回时调用 user-api，释放用户发布提示词时消耗的 skill_market_publish 额度。
 */
@Slf4j
@Service
public class SkillMarketQuotaRefundClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String internalKey;

    public SkillMarketQuotaRefundClient(@Value("${user.api.base-url}") String baseUrl,
                                        @Value("${user.api.internal-key}") String internalKey) {
        this.baseUrl = baseUrl;
        this.internalKey = internalKey;
        this.restTemplate = new RestTemplate();
    }

    public void refundPublishQuota(Long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Key", internalKey == null ? "" : internalKey);

        try {
            @SuppressWarnings("rawtypes")
            Map body = Map.of("userId", userId);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    baseUrl + "/api/v1/user/internal/market-skills/refund-publish-quota",
                    new HttpEntity<>(body, headers),
                    Map.class);
            if (response == null || !Integer.valueOf(0).equals(response.get("code"))) {
                log.warn("退提示词市场发布额度失败 user-api 响应: {}", response);
                throw new BusinessException(AdminGenerationErrorCode.GENERATION_OUTPUT_PARSE_FAILED);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            log.warn("调用 user-api 退提示词市场发布额度失败：{}", e.getMessage());
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_OUTPUT_PARSE_FAILED);
        }
    }
}
