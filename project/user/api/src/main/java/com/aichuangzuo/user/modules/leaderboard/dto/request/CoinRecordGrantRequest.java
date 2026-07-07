package com.aichuangzuo.user.modules.leaderboard.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 内部发奖请求（仅管理端 JWT 可调用）。
 */
@Data
public class CoinRecordGrantRequest {

    @NotNull
    private Long userId;

    @NotNull
    @Positive
    private BigDecimal amount;

    private String refId;

    private String remark;
}
