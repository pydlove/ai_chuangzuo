package com.aichuangzuo.admin.modules.leaderboard.client;

import com.aichuangzuo.admin.infrastructure.security.JwtUtil;
import com.aichuangzuo.admin.modules.leaderboard.dto.request.UserCoinGrantRequest;
import com.aichuangzuo.shared.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

/**
 * 用户端内部接口客户端。
 */
@Component
@RequiredArgsConstructor
public class UserApiClient {

    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${leaderboard.user-api.base-url:http://localhost:25050}")
    private String userApiBaseUrl;

    /**
     * 调用用户端发放创作币。
     *
     * @return 用户端流水业务编号
     */
    @SuppressWarnings("unchecked")
    public String grantCoin(Long userId, String bizType, BigDecimal amount, String refId, String remark) {
        String token = jwtUtil.generateAccessToken(userId);

        UserCoinGrantRequest request = new UserCoinGrantRequest();
        request.setUserId(userId);
        request.setAmount(amount);
        request.setRefId(refId);
        request.setRemark(remark);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        HttpEntity<UserCoinGrantRequest> entity = new HttpEntity<>(request, headers);

        String url = userApiBaseUrl + "/api/v1/user/internal/coin-records/grant";
        Result<String> response = restTemplate.postForObject(url, entity, Result.class);
        if (response == null || response.getData() == null) {
            throw new RuntimeException("grant coin failed: empty response");
        }
        return response.getData();
    }
}
