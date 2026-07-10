package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationStep;
import com.aichuangzuo.admin.modules.generation.pipeline.StepResult;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 第 1 阶段：意图锚定（passthrough）
 *
 * <p>把用户的 4 项输入（标题 / 核心观点 / 目标读者 / 风格）组装成下游 prompt 可嵌入的 user_context_block。
 * 纯字符串拼接，无 AI 调用。
 */
@Component
public class IntentAnchorStep implements GenerationStep {

    @Override
    public int stageIndex() { return 1; }

    @Override
    public String name() { return "intent-anchor"; }

    @Override
    public StepResult process(GenerationContext ctx) {
        Map<String, Object> in = ctx.getInput() == null ? Map.of() : ctx.getInput();
        String title = str(in.get("title"));
        String coreViewpoint = str(in.get("description"));
        String targetReader = str(in.getOrDefault("targetReader", "通用读者"));
        if (targetReader.isBlank()) targetReader = "通用读者";
        String style = str(in.get("userStylePrompt"));

        StringBuilder sb = new StringBuilder();
        sb.append("标题：").append(title).append("\n");
        sb.append("核心观点：").append(coreViewpoint).append("\n");
        sb.append("目标读者：").append(targetReader).append("\n");
        sb.append("风格：").append(style);
        ctx.setUserContextBlock(sb.toString());
        return StepResult.CONTINUE;
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
