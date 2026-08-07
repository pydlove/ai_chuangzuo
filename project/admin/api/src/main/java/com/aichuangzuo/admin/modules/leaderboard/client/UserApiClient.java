package com.aichuangzuo.admin.modules.leaderboard.client;

import com.aichuangzuo.admin.infrastructure.security.JwtUtil;
import com.aichuangzuo.admin.modules.leaderboard.dto.request.ProcessWithdrawRequest;
import com.aichuangzuo.admin.modules.leaderboard.dto.request.RecordEarningsRequest;
import com.aichuangzuo.admin.modules.leaderboard.dto.request.UserCoinGrantRequest;
import com.aichuangzuo.shared.exception.BusinessException;
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
     * 调用用户端记录收益。
     */
    public void recordEarnings(Long userId, String type, String sourceType, String sourceId,
                               String title, String description, BigDecimal amount, String settlementMonth) {
        String token = jwtUtil.generateAccessToken(userId);

        RecordEarningsRequest request = new RecordEarningsRequest();
        request.setUserId(userId);
        request.setType(type);
        request.setSourceType(sourceType);
        request.setSourceId(sourceId);
        request.setTitle(title);
        request.setDescription(description);
        request.setAmount(amount);
        request.setSettlementMonth(settlementMonth);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        HttpEntity<RecordEarningsRequest> entity = new HttpEntity<>(request, headers);

        String url = userApiBaseUrl + "/api/v1/user/internal/earnings/record";
        Result<Void> response = restTemplate.postForObject(url, entity, Result.class);
        if (response == null) {
            throw new RuntimeException("record earnings failed: empty response");
        }
        if (response.getCode() == null || response.getCode() != 0) {
            throw new BusinessException(response.getCode(), "record earnings failed: " + response.getMessage());
        }
    }

    /**
     * 调用用户端处理提现申请。
     */
    public void processWithdraw(String bizNo, Long adminUserId, Integer status, String remark) {
        String token = jwtUtil.generateAccessToken(adminUserId);

        ProcessWithdrawRequest request = new ProcessWithdrawRequest();
        request.setStatus(status);
        request.setRemark(remark);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        HttpEntity<ProcessWithdrawRequest> entity = new HttpEntity<>(request, headers);

        String url = userApiBaseUrl + "/api/v1/user/internal/withdrawals/" + bizNo + "/process";
        Result<Void> response = restTemplate.postForObject(url, entity, Result.class);
        if (response == null) {
            throw new RuntimeException("process withdraw failed: empty response");
        }
        if (response.getCode() == null || response.getCode() != 0) {
            throw new BusinessException(response.getCode(), "process withdraw failed: " + response.getMessage());
        }
    }

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
        request.setBizType(bizType);
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
        if (response.getCode() == null || response.getCode() != 0) {
            throw new BusinessException(response.getCode(), "grant coin failed: " + response.getMessage());
        }
        return response.getData();
    }
}
