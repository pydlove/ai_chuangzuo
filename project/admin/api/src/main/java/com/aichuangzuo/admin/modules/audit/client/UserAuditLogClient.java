package com.aichuangzuo.admin.modules.audit.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin → User 内部 HTTP 客户端：查询和清理用户端审计日志。
 *
 * <p>通过 {@code X-Internal-Key} 走 user-api 的 {@code InternalKeyAuthenticationFilter} 校验。
 */
@Slf4j
@Component
public class UserAuditLogClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String internalKey;

    public UserAuditLogClient(@Value("${user.api.base-url}") String baseUrl,
                              @Value("${user.api.internal-key}") String internalKey) {
        this.baseUrl = baseUrl;
        this.internalKey = internalKey;
        this.restTemplate = new RestTemplate();
    }

    /**
     * 查询用户端审计日志。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> queryLogs(Long userId, String startDate, String endDate,
                                         long page, long pageSize) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(
                baseUrl + "/api/v1/user/internal/audit-logs");
        if (userId != null) {
            builder.queryParam("userId", userId);
        }
        if (startDate != null && !startDate.isBlank()) {
            builder.queryParam("startDate", startDate);
        }
        if (endDate != null && !endDate.isBlank()) {
            builder.queryParam("endDate", endDate);
        }
        builder.queryParam("page", page);
        builder.queryParam("pageSize", pageSize);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Key", internalKey == null ? "" : internalKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            Map<String, Object> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    Map.class).getBody();
            if (response == null || !Integer.valueOf(0).equals(response.get("code"))) {
                log.warn("查询 user-api 审计日志失败 响应: {}", response);
                throw new RuntimeException("查询用户端审计日志失败");
            }
            return (Map<String, Object>) response.get("data");
        } catch (RestClientException e) {
            log.warn("调用 user-api 审计日志查询失败：{}", e.getMessage());
            throw new RuntimeException("查询用户端审计日志失败", e);
        }
    }

    /**
     * 触发用户端清理过期审计日志。
     */
    @SuppressWarnings("unchecked")
    public void cleanupLogs(int retentionDays) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Key", internalKey == null ? "" : internalKey);

        Map<String, Object> body = new HashMap<>();
        body.put("retentionDays", retentionDays);

        try {
            Map<String, Object> response = restTemplate.postForObject(
                    baseUrl + "/api/v1/user/internal/audit-logs/cleanup",
                    new HttpEntity<>(body, headers),
                    Map.class);
            if (response == null || !Integer.valueOf(0).equals(response.get("code"))) {
                log.warn("清理 user-api 审计日志失败 响应: {}", response);
                throw new RuntimeException("清理用户端审计日志失败");
            }
        } catch (RestClientException e) {
            log.warn("调用 user-api 审计日志清理失败：{}", e.getMessage());
            throw new RuntimeException("清理用户端审计日志失败", e);
        }
    }
}
