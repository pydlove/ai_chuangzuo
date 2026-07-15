package com.aichuangzuo.admin.modules.generation.pipeline;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单次 AI 调用的留痕记录（ctx 持有列表）。
 *
 * <p>用于审计 / 监控 / 失败排查：知道「哪一步的 AI 调炸了、用了多久、什么错」。
 */
@Data
public class AiCallRecord {
    /** 阶段序号。 */
    private int stageIndex;
    /** 步骤名。 */
    private String stepName;
    /** 调用开始时间。 */
    private LocalDateTime calledAt;
    /** 耗时（ms）。 */
    private long durationMs;
    /** 失败原因（成功时为 null）。 */
    private String error;
    /** 是否成功。 */
    private boolean success;
    /** 本次尝试实际发送给 AI 的完整 userMsg（含变量替换结果；重试时包含注入的错误上下文）。 */
    private String userMsg;
    /** AI 完整返回内容（成功时）；失败时为 null。 */
    private String responseContent;
    /** 第几次尝试（1=首次，2=第 1 次重试，...）。 */
    private int attempt;
    /** prompt tokens（成功且 provider 上报时有值；否则 null）。 */
    private Integer promptTokens;
    /** completion tokens（成功且 provider 上报时有值；否则 null）。 */
    private Integer completionTokens;
    /** 总 tokens（成功且 provider 上报时有值；否则 null）。 */
    private Integer totalTokens;
}
