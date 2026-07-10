package com.aichuangzuo.admin.modules.generation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创作运行时配置更新请求。
 */
@Data
public class GenerationConfigUpdateRequest {

    @NotNull
    @Min(1)
    @Max(10)
    private Integer poolSize;

    @NotNull
    @Min(1)
    @Max(10)
    private Integer claimBatchSize;

    @NotNull
    @Min(1)
    @Max(60)
    private Integer leaseMinutes;

    @NotNull
    @Min(1)
    @Max(10)
    private Integer maxRetry;

    @NotNull
    @Min(100)
    @Max(5000)
    private Integer pollIntervalMs;

    @NotBlank
    @Size(max = 64)
    private String retentionCron;

    @NotBlank
    @Size(max = 64)
    private String workerId;

    @NotNull
    @Min(1)
    @Max(10)
    private Integer llmRetryMaxAttempts;

    @NotNull
    @Min(100)
    @Max(10000)
    private Integer llmRetryBaseDelayMs;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer llmRetryBackoffMultiplier;

    @Size(max = 256)
    private String remark;
}
