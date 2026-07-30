package com.aichuangzuo.admin.modules.generation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin → User 内部 HTTP 客户端：记录提示词使用。
 *
 * <p>调用 user-api 的 {@code /api/v1/user/internal/skills/use}。
 */
@Slf4j
@Service
public class SkillUsageInternalClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String internalKey;

    public SkillUsageInternalClient(@Value("${user.api.base-url:}") String baseUrl,
                                    @Value("${user.api.internal-key:}") String internalKey) {
        this.baseUrl = baseUrl;
        this.internalKey = internalKey;
        this.restTemplate = new RestTemplate();
    }

    public void recordUsage(Long taskId, Long userId, String skillRef) {
        if (userId == null || skillRef == null || skillRef.isBlank()) {
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Key", internalKey == null ? "" : internalKey);

        Map<String, Object> body = new HashMap<>();
        body.put("taskId", taskId);
        body.put("userId", userId);
        body.put("skillRef", skillRef);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    baseUrl + "/api/v1/user/internal/skills/use",
                    new HttpEntity<>(body, headers),
                    Map.class);
            if (response == null || !Integer.valueOf(0).equals(response.get("code"))) {
                log.warn("记录提示词使用失败 user-api 响应: {}", response);
            } else {
                log.info("记录提示词使用完成 taskId={} userId={} skillRef={}", taskId, userId, skillRef);
            }
        } catch (RestClientException e) {
            log.warn("调用 user-api 记录提示词使用失败：{}", e.getMessage());
            throw e;
        }
    }
}
