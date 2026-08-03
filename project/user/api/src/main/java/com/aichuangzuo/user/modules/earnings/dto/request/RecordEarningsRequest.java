package com.aichuangzuo.user.modules.earnings.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 内部记录收益请求（仅管理端 JWT 可调用）。
 */
@Data
public class RecordEarningsRequest {

    @NotNull
    private Long userId;

    @NotBlank
    @Size(max = 32)
    private String type;

    @Size(max = 64)
    private String sourceType;

    @Size(max = 128)
    private String sourceId;

    @Size(max = 128)
    private String title;

    @Size(max = 512)
    private String description;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "月份格式应为 YYYY-MM")
    private String settlementMonth;
}
