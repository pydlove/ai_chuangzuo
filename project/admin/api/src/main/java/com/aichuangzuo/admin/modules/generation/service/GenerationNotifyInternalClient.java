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

import java.util.HashMap;
import java.util.Map;

/**
 * Admin → User 内部 HTTP 客户端：让 user-api 给用户推送 generation 类消息。
 *
 * <p>调用 user-api 的 {@code /api/v1/user/internal/generation/notify-completion}，
 * 通过 {@code X-Internal-Key} 走 InternalKeyAuthenticationFilter 校验。
 *
 * <p>复用 {@link QuotaRefundInternalClient} 的 HTTP 形态：成功返回 null，
 * 调用失败（连接 / 4xx / 5xx）抛 {@link BusinessException}，由 worker 决定是否吞掉
 * （当前实现：吞掉，失败不影响任务主流程的 markCompleted / markFailed 状态）。</p>
 */
@Slf4j
@Service
public class GenerationNotifyInternalClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String internalKey;

    public GenerationNotifyInternalClient(@Value("${user.api.base-url}") String baseUrl,
                                          @Value("${user.api.internal-key}") String internalKey) {
        this.baseUrl = baseUrl;
        this.internalKey = internalKey;
        this.restTemplate = new RestTemplate();
    }

    /**
     * 通知 user-api：任务已生成文章完成，向消息中心推"创作完成"通知。
     *
     * @param taskId        当前生成任务 id（仅日志用）
     * @param userId        目标用户 id
     * @param articleBizNo  落库的文章业务号（用于构造跳转链接）
     * @param articleTitle  文章标题（用户原始输入的 title，用于摘要展示；可空）
     */
    public void notifyCompleted(Long taskId, Long userId, String articleBizNo, String articleTitle) {
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", taskId);
        body.put("userId", userId);
        body.put("status", "completed");
        body.put("articleBizNo", articleBizNo);
        body.put("articleTitle", articleTitle);
        postNotify(taskId, userId, body);
    }

    /**
     * 通知 user-api：任务失败已退款，向消息中心推"创作失败"通知。
     *
     * @param taskId       当前生成任务 id
     * @param userId       目标用户 id
     * @param failReason   失败原因（仅日志用，不展示给用户；可空）
     * @param articleTitle 文章标题（用户原始输入的 title，用于标题展示；可空）
     */
    public void notifyFailed(Long taskId, Long userId, String failReason, String articleTitle) {
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", taskId);
        body.put("userId", userId);
        body.put("status", "failed");
        body.put("failReason", failReason);
        body.put("articleTitle", articleTitle);
        postNotify(taskId, userId, body);
    }

    private void postNotify(Long taskId, Long userId, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Key", internalKey == null ? "" : internalKey);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    baseUrl + "/api/v1/user/internal/generation/notify-completion",
                    new HttpEntity<>(body, headers),
                    Map.class);
            if (response == null || !Integer.valueOf(0).equals(response.get("code"))) {
                log.warn("通知 user-api 推消息失败 task={} userId={} 响应: {}", taskId, userId, response);
                throw new BusinessException(AdminGenerationErrorCode.GENERATION_OUTPUT_PARSE_FAILED);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            log.warn("调用 user-api notify-completion 失败 task={} userId={}：{}", taskId, userId, e.getMessage());
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_OUTPUT_PARSE_FAILED);
        }
    }
}
