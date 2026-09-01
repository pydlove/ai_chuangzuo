package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.aiprompt.service.AiPromptRenderService;
import com.aichuangzuo.admin.modules.generation.entity.PromptTemplateStage;
import com.aichuangzuo.admin.modules.generation.pipeline.AiGateway;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationStep;
import com.aichuangzuo.admin.modules.generation.pipeline.PipelineUtils;
import com.aichuangzuo.admin.modules.generation.pipeline.StepResult;
import com.aichuangzuo.shared.vo.AiPromptRendered;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 第 6 阶段：人类化改写（AI）— 对初稿做一次整体人类化改写，降低 AI 检测率。
 *
 * <p>本 step 与 {@link RhythmRewriteStep} 共用 stageIndex=6，通过 stage_key 区分：
 * <ul>
 *   <li>stage_key = {@code humanize_rewrite}：执行本 step</li>
 *   <li>stage_key = {@code rhythm_rewrite}：由 {@link RhythmRewriteStep} 执行</li>
 * </ul>
 *
 * <p>提示词从 {@code c_ai_prompt}（prompt_code = {@value #PROMPT_CODE}）读取，
 * 便于产品/运营在「AI 提示词管理」中随时调整，无需改代码或发版。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HumanizeRewriteStep implements GenerationStep {

    public static final String PROMPT_CODE = "article_humanize_rewrite_v1";
    public static final String STAGE_KEY = "humanize_rewrite";

    private final AiGateway aiGateway;
    private final AiPromptRenderService aiPromptRenderService;

    @Override
    public int stageIndex() {
        return 6;
    }

    @Override
    public String name() {
        return "humanize-rewrite";
    }

    @Override
    public boolean enabled(GenerationContext ctx) {
        PromptTemplateStage stage = ctx.getStages().get(stageIndex());
        return stage != null
                && stage.getEnabled() != null
                && stage.getEnabled() == 1
                && STAGE_KEY.equals(stage.getStageKey());
    }

    @Override
    public StepResult process(GenerationContext ctx) {
        ctx.putExtra("__currentStageIndex", stageIndex());
        ctx.putExtra("__currentStepName", name());

        String draft = resolveDraft(ctx);
        if (draft == null || draft.isBlank()) {
            log.warn("stage {} ({}) 无可用 draft，跳过", stageIndex(), name());
            return StepResult.CONTINUE;
        }

        AiPromptRendered rendered = aiPromptRenderService.render(PROMPT_CODE, Map.of("draft", draft));

        String aiResp = aiGateway.call(ctx, rendered.systemRole(), rendered.userPrompt(), ctx.getModelParams());
        JsonNode root = PipelineUtils.parseAiJson(aiResp);

        if (!root.path("draft").isArray()) {
            throw new RuntimeException("stage " + stageIndex() + " (" + name() + ") 返回缺少 draft 数组");
        }

        ctx.setFinalDraftJson(root.toString());
        log.info("stage {} ({}) 完成", stageIndex(), name());
        return StepResult.CONTINUE;
    }

    /**
     * 取当前可用的 draft JSON：优先用经过前面 stage 处理后的 finalDraftJson，
     * 否则回退到 stage 4 产出的 draftJson。
     */
    private String resolveDraft(GenerationContext ctx) {
        String finalDraft = ctx.getFinalDraftJson();
        if (finalDraft != null && !finalDraft.isBlank()) {
            return finalDraft;
        }
        return ctx.getDraftJson();
    }
}
