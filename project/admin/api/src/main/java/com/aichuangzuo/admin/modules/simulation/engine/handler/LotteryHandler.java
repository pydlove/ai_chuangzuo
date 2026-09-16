package com.aichuangzuo.admin.modules.simulation.engine.handler;

import com.aichuangzuo.admin.modules.simulation.client.SimulationUserApiClient;
import com.aichuangzuo.admin.modules.simulation.engine.RobotContext;
import com.aichuangzuo.admin.modules.simulation.engine.RobotTokenHolder;
import com.aichuangzuo.admin.modules.simulation.engine.StageHandler;
import com.aichuangzuo.admin.modules.simulation.engine.StageSkippedException;
import com.aichuangzuo.admin.modules.simulation.enums.SimulationStage;
import com.aichuangzuo.shared.enums.error.LotteryErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
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
    public String execute(RobotContext ctx) {
        return tokenHolder.execute(ctx, token -> {
            Long campaignId = ctx.userApi.currentLotteryCampaignId(token);
            if (campaignId == null) {
                throw new StageSkippedException("无进行中的抽奖活动");
            }
            try {
                SimulationUserApiClient.DrawResult result = ctx.userApi.drawLottery(token, campaignId);
                if (result == null || result.tierName() == null) {
                    return null;
                }
                return "奖项「" + result.tierName() + "」" + (result.message() == null ? "" : " " + result.message());
            } catch (BusinessException e) {
                // 没有抽奖次数（如活动不送免费次数）与真实用户行为一致，视为跳过而非失败
                if (e.getCode() != null && e.getCode() == LotteryErrorCode.NO_DRAW_CHANCE.getCode()) {
                    throw new StageSkippedException("没有可用抽奖次数");
                }
                throw e;
            }
        });
    }
}
