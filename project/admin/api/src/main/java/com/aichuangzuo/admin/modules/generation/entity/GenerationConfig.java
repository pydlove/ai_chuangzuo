package com.aichuangzuo.admin.modules.generation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 创作运行时配置，对应表 {@code a_generation_config}。
 *
 * <p>单行（id=1），由 admin 端 GET/PUT 改写。worker 启动时读 + 每 10s 刷新。
 * <p>{@code poolSize} 变更后需重启 admin-api 生效（重建线程池）；其他字段 worker 会在下一个轮询周期读最新值。
 */
@Getter
@Setter
@TableName("a_generation_config")
public class GenerationConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** worker 线程池大小（1-10，重启生效）。 */
    private Integer poolSize;

    /** 每轮单次拉取任务数（1-10）。 */
    private Integer claimBatchSize;

    /** lease 持续分钟（1-60）。 */
    private Integer leaseMinutes;

    /** 单任务最大重试次数（1-10）。 */
    private Integer maxRetry;

    /** 空轮询间隔 ms（100-5000）。 */
    private Integer pollIntervalMs;

    /** 归档定时任务 cron 表达式。 */
    private String retentionCron;

    /** worker 实例 ID（单实例约定）。 */
    private String workerId;

    /** 单 stage AI 调用最大尝试次数（含首次，1-10）。 */
    private Integer llmRetryMaxAttempts;

    /** 首次重试前等待 ms（100-10000）。 */
    private Integer llmRetryBaseDelayMs;

    /** 指数退避倍数（1-5）。 */
    private Integer llmRetryBackoffMultiplier;

    /** AI temperature 默认值（0.00-2.00；stage model_params 可覆盖）。 */
    private java.math.BigDecimal defaultTemperature;

    /** AI max_tokens 默认值（1-128000；MiniMax-M3 等推理模型 reasoning 也吃此预算）。 */
    private Integer defaultMaxTokens;

    /** AI top_p 默认值（0.00-1.00；stage model_params 可覆盖）。 */
    private java.math.BigDecimal defaultTopP;

    /** 备注。 */
    private String remark;

    private Long tenantId;
    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
}
