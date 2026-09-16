package com.aichuangzuo.admin.modules.simulation.engine.handler;

import com.aichuangzuo.admin.modules.simulation.engine.RobotContext;
import com.aichuangzuo.admin.modules.simulation.engine.RobotTokenHolder;
import com.aichuangzuo.admin.modules.simulation.engine.StageHandler;
import com.aichuangzuo.admin.modules.simulation.engine.StageSkippedException;
import com.aichuangzuo.admin.modules.simulation.enums.SimulationStage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 抽奖阶段：无进行中活动则跳过。
 */
@Component
@RequiredArgsConstructor
public class LotteryHandler implements StageHandler {

    private final RobotTokenHolder tokenHolder;

    @Override
    public SimulationStage stage() {
        return SimulationStage.LOTTERY;
    }

    @Override
    public void execute(RobotContext ctx) {
        tokenHolder.execute(ctx, token -> {
            Long campaignId = ctx.userApi.currentLotteryCampaignId(token);
            if (campaignId == null) {
                throw new StageSkippedException("无进行中的抽奖活动");
            }
            ctx.userApi.drawLottery(token, campaignId);
        });
    }
}
