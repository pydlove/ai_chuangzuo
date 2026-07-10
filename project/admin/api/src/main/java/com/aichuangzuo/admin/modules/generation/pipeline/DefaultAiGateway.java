package com.aichuangzuo.admin.modules.generation.pipeline;

import com.aichuangzuo.admin.modules.generation.service.GenerationAiService;
import com.aichuangzuo.shared.entity.GenerationTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 默认 {@link AiGateway} 实现：复用 {@link GenerationAiService} 调真实 LLM，加：
 * <ul>
 *   <li>budget 校验</li>
 *   <li>失败重试（指数退避）：读 {@code GenerationConfigService.llmRetryMaxAttempts/BaseDelay/Multiplier}</li>
 *   <li>每次失败把上次错误信息塞进 userMsg 头部让模型自纠</li>
 *   <li>调用留痕到 ctx.aiCallHistory</li>
 * </ul>
 *
 * <p>设计文档 §4：同 prompt 最多重试 N 次，按 baseDelay * multiplier^(attempt-1) 退避。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultAiGateway implements AiGateway {

    private final GenerationAiService aiService;
    private final com.aichuangzuo.admin.modules.generation.service.GenerationConfigService configService;

    /** 测试可注入的「当前时间」抽象（ms）。 */
    private final java.util.function.LongSupplier clock = System::currentTimeMillis;

    /** 测试可注入的 sleep 抽象。 */
    private final java.util.function.LongConsumer sleeper = (ms) -> {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    };

    @Override
    public String call(GenerationContext ctx, String systemMsg, String userMsg) {
        int maxAttempts = readMaxAttempts(ctx);
        int baseDelay = readBaseDelay(ctx);
        int multiplier = readMultiplier(ctx);
        long started = clock.getAsLong();
        String currentUserMsg = userMsg;
        String lastError = null;
        int attempt = 0;
        String successContent = null;
        Throwable lastThrowable = null;

        while (attempt < maxAttempts) {
            attempt++;
            // 1. budget 校验（每次尝试都算 1 次预算）
            if (ctx.getAiCallUsed() >= ctx.getAiCallBudget()) {
                throw new AiBudgetExhaustedException(
                        "AI 调用预算耗尽 budget=" + ctx.getAiCallBudget()
                                + "（含已重试 " + ctx.getAiCallRetried() + " 次）");
            }

            // 2. 调真实 AI
            long attemptStart = clock.getAsLong();
            GenerationTask task = ctx.getTask();
            Long modelConfigId = task == null ? null : task.getModelConfigId();
            String content = null;
            Throwable err = null;
            try {
                content = aiService.call(modelConfigId, systemMsg, currentUserMsg);
            } catch (Throwable t) {
                err = t;
                lastThrowable = t;
            }
            long attemptDuration = clock.getAsLong() - attemptStart;

            // 3. 留痕
            AiCallRecord rec = new AiCallRecord();
            rec.setStageIndex(currentStageIndex(ctx));
            rec.setStepName(currentStepName(ctx));
            rec.setCalledAt(java.time.LocalDateTime.now());
            rec.setDurationMs(attemptDuration);
            rec.setSuccess(err == null);
            rec.setError(err == null ? null : err.getClass().getSimpleName() + ":" + err.getMessage());
            rec.setAttempt(attempt);
            ctx.getAiCallHistory().add(rec);
            ctx.setAiCallUsed(ctx.getAiCallUsed() + 1);
            ctx.setAiCallTotalMs(ctx.getAiCallTotalMs() + attemptDuration);
            if (err != null) {
                ctx.setAiCallFailed(ctx.getAiCallFailed() + 1);
                lastError = rec.getError();
                log.warn("AI 调用失败 stage={} attempt={}/{} err={}",
                        rec.getStepName(), attempt, maxAttempts, rec.getError());
                // 不是最后一次 → 等退避 + 把错误塞进下一轮 prompt
                if (attempt < maxAttempts) {
                    long delayMs = backoffMs(baseDelay, multiplier, attempt);
                    log.info("AI 重试 stage={} attempt={} → {} sleepMs={}",
                            rec.getStepName(), attempt + 1, attempt + 1, delayMs);
                    sleeper.accept(delayMs);
                    currentUserMsg = injectErrorContext(userMsg, lastError, attempt);
                }
                continue;
            }

            // 成功
            successContent = content;
            if (attempt > 1) {
                ctx.setAiCallRetried(ctx.getAiCallRetried() + (attempt - 1));
                log.info("AI 调用成功 stage={} 第 {} 次尝试成功（重试了 {} 次）",
                        rec.getStepName(), attempt, attempt - 1);
            } else {
                log.info("AI 调用成功 stage={} attempt={} duration={}ms",
                        rec.getStepName(), attempt, attemptDuration);
            }
            break;
        }

        if (successContent == null) {
            // 所有尝试都失败
            log.error("AI 调用全部失败 stage={} maxAttempts={} lastError={}",
                    currentStepName(ctx), maxAttempts, lastError);
            if (lastThrowable instanceof RuntimeException re) throw re;
            throw new RuntimeException(lastThrowable);
        }
        return successContent;
    }

    // ---------- 退避计算 / 错误注入 ----------

    private static long backoffMs(int baseDelay, int multiplier, int justFailedAttempt) {
        // attempt=1 失败 → 下次重试睡 baseDelay * multiplier^0 = baseDelay
        // attempt=2 失败 → 下次重试睡 baseDelay * multiplier^1
        long v = (long) baseDelay;
        for (int i = 1; i < justFailedAttempt; i++) {
            v *= multiplier;
            if (v > 60_000L) {
                v = 60_000L;
                break;
            }
        }
        return v;
    }

    private static String injectErrorContext(String originalUserMsg, String lastError, int attempt) {
        // 把上次失败原因塞到 userMsg 头部，提示模型自纠
        String prefix = "[上一次尝试失败 reason=" + (lastError == null ? "unknown" : lastError)
                + "；请检查输出格式是否正确]\n";
        return prefix + originalUserMsg;
    }

    private int readMaxAttempts(GenerationContext ctx) {
        try {
            var cfg = configService.getCurrent();
            if (cfg != null && cfg.getLlmRetryMaxAttempts() != null && cfg.getLlmRetryMaxAttempts() >= 1) {
                return cfg.getLlmRetryMaxAttempts();
            }
        } catch (Exception e) {
            // 配置未就绪时用默认
        }
        return 3;
    }

    private int readBaseDelay(GenerationContext ctx) {
        try {
            var cfg = configService.getCurrent();
            if (cfg != null && cfg.getLlmRetryBaseDelayMs() != null && cfg.getLlmRetryBaseDelayMs() >= 100) {
                return cfg.getLlmRetryBaseDelayMs();
            }
        } catch (Exception ignore) { }
        return 500;
    }

    private int readMultiplier(GenerationContext ctx) {
        try {
            var cfg = configService.getCurrent();
            if (cfg != null && cfg.getLlmRetryBackoffMultiplier() != null && cfg.getLlmRetryBackoffMultiplier() >= 1) {
                return cfg.getLlmRetryBackoffMultiplier();
            }
        } catch (Exception ignore) { }
        return 2;
    }

    private int currentStageIndex(GenerationContext ctx) {
        return ctx.getExtra("__currentStageIndex") == null ? 0
                : (Integer) ctx.getExtra("__currentStageIndex");
    }

    private String currentStepName(GenerationContext ctx) {
        return ctx.getExtra("__currentStepName") == null ? "?"
                : (String) ctx.getExtra("__currentStepName");
    }
}
