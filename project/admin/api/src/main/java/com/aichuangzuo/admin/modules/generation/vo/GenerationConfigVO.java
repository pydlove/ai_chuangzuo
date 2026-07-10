package com.aichuangzuo.admin.modules.generation.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GenerationConfigVO {
    private Long id;
    private Integer poolSize;
    private Integer claimBatchSize;
    private Integer leaseMinutes;
    private Integer maxRetry;
    private Integer pollIntervalMs;
    private String retentionCron;
    private String workerId;
    private Integer llmRetryMaxAttempts;
    private Integer llmRetryBaseDelayMs;
    private Integer llmRetryBackoffMultiplier;
    private String remark;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}
