package com.aichuangzuo.user.modules.leaderboard.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InternalGrantRequest {

    @NotNull(message = "userId 不能为空")
    private Long userId;

    @NotNull(message = "amount 不能为空")
    @DecimalMin(value = "0.0001", message = "amount 必须大于 0")
    private BigDecimal amount;

    @NotBlank(message = "bizType 不能为空")
    private String bizType;

    private String refId;

    private String remark;
}
