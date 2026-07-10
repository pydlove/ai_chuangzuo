package com.aichuangzuo.admin.modules.generation.pipeline;

import com.aichuangzuo.shared.entity.GenerationTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 12 阶段流水线编排器。
 *
 * <p>Spring 自动注入所有 {@link GenerationStep} bean（13 个：12 阶段 + 1 PersistArticleStep），
 * 按 stageIndex 升序逐个执行。
 *
 * <p>入口：{@link #run(GenerationTask)} — worker 调用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationPipeline {

    private final List<GenerationStep> allSteps;
    private final PipelineTemplateResolver templateResolver;

    /**
     * 跑完整流水线：从加载模板 → 12 阶段 → persist article。
     * 任意 step 抛异常 → 整条 task 失败（让 worker 走 retry / 退币）。
     */
    public GenerationContext run(GenerationTask task) {
        GenerationContext ctx = new GenerationContext();
        ctx.setTask(task);
        ctx.setInput(parseInput(task.getInputParam()));

        // 1. 加载模板 + 12 stages（阶段 3+：按任务锁定的 templateId/version，老任务走 fallback）
        templateResolver.resolveInto(ctx, task.getPromptTemplateId(), task.getPromptTemplateVersion());

        // 2. 按 stageIndex 升序执行
        List<GenerationStep> sorted = allSteps.stream()
                .sorted(Comparator.comparingInt(GenerationStep::stageIndex))
                .toList();
        for (GenerationStep step : sorted) {
            if (!step.enabled(ctx)) {
                log.debug("stage {} ({}) disabled, skip", step.stageIndex(), step.name());
                continue;
            }
            log.info("→ stage {} ({})", step.stageIndex(), step.name());
            try {
                StepResult r = step.process(ctx);
                if (r == StepResult.STOP) {
                    log.info("stage {} ({}) 要求 STOP，pipeline 提前结束", step.stageIndex(), step.name());
                    break;
                }
            } catch (RuntimeException e) {
                log.error("✗ stage {} ({}) 失败: {}", step.stageIndex(), step.name(), e.getMessage());
                throw e;
            }
        }
        log.info("pipeline 完成 task={} aiCalls={} aiFailed={} totalMs={}",
                task.getId(), ctx.getAiCallUsed(), ctx.getAiCallFailed(), ctx.getAiCallTotalMs());
        return ctx;
    }

    private Map<String, Object> parseInput(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(json, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception e) {
            log.warn("inputParam 解析失败，用空 map", e);
            return new HashMap<>();
        }
    }
}
