package com.aichuangzuo.admin.modules.simulation.engine.handler;

import com.aichuangzuo.admin.modules.simulation.engine.RobotContext;
import com.aichuangzuo.admin.modules.simulation.engine.RobotTokenHolder;
import com.aichuangzuo.admin.modules.simulation.engine.StageHandler;
import com.aichuangzuo.admin.modules.simulation.enums.SimulationStage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 会员阶段：订阅批次配置的套餐（测试支付绿通：payCode=123456 + X-Internal-Key）。
 */
@Component
@RequiredArgsConstructor
public class MembershipHandler implements StageHandler {

    private final RobotTokenHolder tokenHolder;

    @Override
    public SimulationStage stage() {
        return SimulationStage.MEMBERSHIP;
    }

    @Override
    public String execute(RobotContext ctx) {
        return tokenHolder.execute(ctx, token -> {
            ctx.userApi.subscribeMembership(token, ctx.batch.getPlanKey(), ctx.batch.getCycle());
            String planName = ctx.batch.getPlanName() == null || ctx.batch.getPlanName().isBlank()
                    ? ctx.batch.getPlanKey() : ctx.batch.getPlanName();
            return "「" + planName + "」" + ctx.batch.getCycle();
        });
    }
}
