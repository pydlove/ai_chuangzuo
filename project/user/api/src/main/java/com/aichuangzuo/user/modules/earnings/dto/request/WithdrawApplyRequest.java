package com.aichuangzuo.user.modules.earnings.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 申请提现请求。
 */
@Data
public class WithdrawApplyRequest {

    @NotNull(message = "提现金额不能为空")
    @Positive(message = "提现金额必须大于 0")
    private BigDecimal amount;

    @NotBlank(message = "收款账号不能为空")
    @Size(max = 128, message = "收款账号不能超过 128 个字符")
    private String account;
}
