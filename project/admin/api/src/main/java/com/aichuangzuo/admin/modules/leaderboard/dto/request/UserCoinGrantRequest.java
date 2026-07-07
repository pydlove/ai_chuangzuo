package com.aichuangzuo.admin.modules.leaderboard.dto.request;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 调用用户端内部发奖接口的请求体。
 */
@Data
public class UserCoinGrantRequest {

    private Long userId;
    private BigDecimal amount;
    private String refId;
    private String remark;
}
