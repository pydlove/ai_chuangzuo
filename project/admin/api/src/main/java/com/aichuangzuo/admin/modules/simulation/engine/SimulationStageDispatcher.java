package com.aichuangzuo.admin.modules.simulation.engine;

import com.aichuangzuo.admin.modules.simulation.config.SimulationStageConfig;
import com.aichuangzuo.admin.modules.simulation.enums.SimulationStage;
import com.aichuangzuo.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.List;

/**
 * 模拟运营-阶段调度器：概率/开关判定 + 分发到对应 StageHandler。
 *
 * <p>概率判定只针对四个业务阶段（抽奖/会员/创作/约稿），其余阶段恒执行。
 */
@Component
@RequiredArgsConstructor
public class SimulationStageDispatcher {

    private final List<StageHandler> handlers;
    private final SecureRandom random = new SecureRandom();

    /** 分发到对应 StageHandler，返回阶段明细（写入日志 detail 列）。 */
    public String dispatch(RobotContext ctx) {
        SimulationStage stage = SimulationStage.valueOf(ctx.robot.getCurrentStage());
        if (!enabled(stage, ctx.config)) {
            throw new StageSkippedException("阶段未启用或概率未命中");
        }
        return handlers.stream()
                .filter(h -> h.stage() == stage)
                .findFirst()
                .orElseThrow(() -> new BusinessException(500, "无阶段处理器: " + stage))
                .execute(ctx);
    }

    private boolean enabled(SimulationStage stage, SimulationStageConfig c) {
        return switch (stage) {
            case LOTTERY -> c.isLotteryEnabled() && random.nextInt(100) < c.getLotteryProbability();
            case MEMBERSHIP -> c.isMembershipEnabled() && random.nextInt(100) < c.getMembershipProbability();
            case CREATE -> c.isCreateEnabled() && random.nextInt(100) < c.getCreateProbability();
            case COMMISSION -> c.isCommissionEnabled() && random.nextInt(100) < c.getCommissionProbability();
            default -> true;
        };
    }
}
