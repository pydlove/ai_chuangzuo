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
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * Admin → User 内部 HTTP 客户端：模拟使用一次市场提示词。
 *
 * <p>管理端在提示词编辑弹框中点击“+1”时调用 user-api，让指定用户模拟使用该提示词，
 * user-api 会自动给发布者发创作币并写入收益明细。
 */
@Slf4j
@Service
public class SkillMarketUsageClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String internalKey;

    public SkillMarketUsageClient(@Value("${user.api.base-url}") String baseUrl,
                                  @Value("${user.api.internal-key}") String internalKey) {
        this.baseUrl = baseUrl;
        this.internalKey = internalKey;
        this.restTemplate = new RestTemplate();
    }

    public void recordUsage(String bizNo, Long consumerUserId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Key", internalKey == null ? "" : internalKey);

        String url = UriComponentsBuilder.fromHttpUrl(
                        baseUrl + "/api/v1/user/internal/market-skills/" + bizNo + "/use")
                .queryParam("consumerUserId", consumerUserId)
                .toUriString();

        try {
            @SuppressWarnings("rawtypes")
            Map response = restTemplate.postForObject(
                    url,
                    new HttpEntity<>(null, headers),
                    Map.class);
            if (response == null || !Integer.valueOf(0).equals(response.get("code"))) {
                log.warn("模拟使用提示词失败 user-api 响应: {}", response);
                throw new BusinessException(AdminGenerationErrorCode.GENERATION_OUTPUT_PARSE_FAILED);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            log.warn("调用 user-api 模拟使用提示词失败：{}", e.getMessage());
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_OUTPUT_PARSE_FAILED);
        }
    }
}
