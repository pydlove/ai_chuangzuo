package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.generation.pipeline.AiGateway;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationStep;
import com.aichuangzuo.admin.modules.generation.pipeline.PipelineUtils;
import com.aichuangzuo.admin.modules.generation.pipeline.StepResult;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 阶段基类：处理 ai_prompt 渲染 → 调 AI → 解析 JSON → 把结果存到 ctx。
 *
 * <p>子类只需实现 {@link #parseAndStore(JsonNode, GenerationContext)}。
 *
 * <p>modelParams 取自 {@code ctx.modelParams}，由 {@code GenerationPipeline.setupStageModelParams}
 * 在调用 {@link #process} 前根据当前阶段模板的 {@code model_params} 列注入；为 null 时
 * {@code AiGateway} 走默认参数（由 {@code GenerationAiService} 提供）。
 */
@Slf4j
public abstract class AbstractAiStep implements GenerationStep {

    protected final AiGateway aiGateway;

    protected AbstractAiStep(AiGateway aiGateway) {
        this.aiGateway = aiGateway;
    }

    @Override
    public final StepResult process(GenerationContext ctx) {
        // 标记当前 step（让 AiGateway 留痕时知道是哪一步）
        ctx.putExtra("__currentStageIndex", stageIndex());
        ctx.putExtra("__currentStepName", name());

        String userPrompt = PipelineUtils.renderAiPrompt(ctx, stageIndex());
        if (userPrompt.isBlank()) {
            throw new RuntimeException("stage " + stageIndex() + " (" + name() + ") 的 ai_prompt 为空");
        }
        // 用 4 参版本：把 ctx.modelParams（由 pipeline 在 step.process 之前注入）传给 Gateway
        String aiResp = aiGateway.call(ctx, systemMessage(), userPrompt, ctx.getModelParams());
        JsonNode root = PipelineUtils.parseAiJson(aiResp);
        parseAndStore(root, ctx);
        log.debug("stage {} ({}) 完成", stageIndex(), name());
        return StepResult.CONTINUE;
    }

    /**
     * 子类实现：解析 AI 返回的 JSON，把需要的内容塞 ctx。
     */
    protected abstract void parseAndStore(JsonNode root, GenerationContext ctx);

    /**
     * system message，默认空。子类可重写加 role 描述。
     */
    protected String systemMessage() {
        return "";
    }
}
