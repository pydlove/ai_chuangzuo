package com.aichuangzuo.admin.modules.simulation.engine.handler;

import com.aichuangzuo.admin.modules.simulation.engine.RobotContext;
import com.aichuangzuo.admin.modules.simulation.engine.RobotTokenHolder;
import com.aichuangzuo.admin.modules.simulation.engine.StageHandler;
import com.aichuangzuo.admin.modules.simulation.engine.StageSkippedException;
import com.aichuangzuo.admin.modules.simulation.enums.SimulationStage;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 约稿阶段：用 CREATE 阶段生成的文章随机投递一个投稿中的约稿任务。
 */
@Component
@RequiredArgsConstructor
public class CommissionHandler implements StageHandler {

    private final RobotTokenHolder tokenHolder;

    @Override
    public SimulationStage stage() {
        return SimulationStage.COMMISSION;
    }

    @Override
    public void execute(RobotContext ctx) {
        String articleBizNo = readArticleBizNo(ctx);
        if (articleBizNo == null) {
            throw new StageSkippedException("无可投递文章");
        }
        tokenHolder.execute(ctx, token -> {
            Long commissionTaskId = ctx.userApi.randomOpenCommissionTaskId(token);
            if (commissionTaskId == null) {
                throw new StageSkippedException("无投稿中的约稿任务");
            }
            ctx.userApi.submitCommission(token, commissionTaskId, articleBizNo);
        });
    }

    private String readArticleBizNo(RobotContext ctx) {
        String contextJson = ctx.robot.getContext();
        if (contextJson == null || contextJson.isBlank()) {
            return null;
        }
        try {
            JsonNode node = ctx.objectMapper.readTree(contextJson);
            JsonNode bizNo = node.get("articleBizNo");
            return bizNo == null || bizNo.isNull() ? null : bizNo.asText();
        } catch (Exception e) {
            return null;
        }
    }
}
