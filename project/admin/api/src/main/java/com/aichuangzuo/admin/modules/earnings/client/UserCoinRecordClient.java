package com.aichuangzuo.admin.modules.earnings.client;

import com.aichuangzuo.shared.enums.error.AdminEarningsErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCoinRecordClient {

    @Value("${user.api.base-url}")
    private String userApiBaseUrl;

    @Value("${user.api.internal-key}")
    private String internalKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String internalGrant(Long userId, BigDecimal amount, String bizType, String refId, String remark) {
        String url = userApiBaseUrl + "/api/v1/user/coin-records/internal-grant";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Key", internalKey);

        Map<String, Object> body = Map.of(
                "userId", userId,
                "amount", amount,
                "bizType", bizType,
                "refId", refId,
                "remark", remark);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            Result<Map<String, String>> result = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Result<Map<String, String>>>() {}).getBody();
            if (result == null || result.getData() == null) {
                throw new BusinessException(AdminEarningsErrorCode.GRANT_CROSS_SERVICE_FAILED);
            }
            return result.getData().get("coinRecordBizNo");
        } catch (RestClientException e) {
            log.error("internal grant failed, userId={}", userId, e);
            throw new BusinessException(AdminEarningsErrorCode.GRANT_CROSS_SERVICE_FAILED);
        }
    }
}
