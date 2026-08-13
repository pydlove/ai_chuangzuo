package com.aichuangzuo.admin.modules.scheduler.client;

import com.aichuangzuo.admin.modules.scheduler.vo.ScheduledTaskVO;
import com.aichuangzuo.shared.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin → User 内部 HTTP 客户端：查询和触发用户端定时任务。
 */
@Slf4j
@Component
public class UserScheduledTaskClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String internalKey;

    public UserScheduledTaskClient(@Value("${user.api.base-url}") String baseUrl,
                                   @Value("${user.api.internal-key}") String internalKey) {
        this.baseUrl = baseUrl;
        this.internalKey = internalKey;
        this.restTemplate = new RestTemplate();
    }

    /**
     * 查询用户端定时任务列表。
     */
    public List<ScheduledTaskVO> listTasks() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Key", internalKey == null ? "" : internalKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            Result<List<ScheduledTaskVO>> response = restTemplate.exchange(
                    baseUrl + "/api/v1/user/internal/scheduled-tasks",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Result<List<ScheduledTaskVO>>>() {}).getBody();
            if (response == null || !Integer.valueOf(0).equals(response.getCode())) {
                log.warn("查询 user-api 定时任务失败 响应: {}", response);
                throw new RuntimeException("查询用户端定时任务失败");
            }
            return response.getData() == null ? List.of() : response.getData();
        } catch (RestClientException e) {
            log.warn("调用 user-api 定时任务查询失败：{}", e.getMessage());
            throw new RuntimeException("查询用户端定时任务失败", e);
        }
    }

    /**
     * 触发用户端定时任务。
     *
     * @return 执行日志ID（user-api 侧）
     */
    @SuppressWarnings("unchecked")
    public Long triggerTask(String taskKey, Long adminId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Key", internalKey == null ? "" : internalKey);

        Map<String, Object> body = new HashMap<>();
        body.put("adminId", adminId);

        try {
            Map<String, Object> response = restTemplate.postForObject(
                    baseUrl + "/api/v1/user/internal/scheduled-tasks/" + taskKey + "/actions/trigger",
                    new HttpEntity<>(body, headers),
                    Map.class);
            if (response == null || !Integer.valueOf(0).equals(response.get("code"))) {
                log.warn("触发 user-api 定时任务失败 响应: {}", response);
                throw new RuntimeException("触发用户端定时任务失败");
            }
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            return data == null ? null : ((Number) data.get("logId")).longValue();
        } catch (RestClientException e) {
            log.warn("调用 user-api 定时任务触发失败：{}", e.getMessage());
            throw new RuntimeException("触发用户端定时任务失败", e);
        }
    }
}
