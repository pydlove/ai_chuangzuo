package com.aichuangzuo.user.modules.leaderboard.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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

    @NotBlank
    @Size(max = 32)
    private String bizType;

    private String refId;

    private String remark;
}
