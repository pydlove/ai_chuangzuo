package com.aichuangzuo.admin.modules.simulation.engine.handler;

import com.aichuangzuo.admin.modules.simulation.engine.RobotContext;
import com.aichuangzuo.admin.modules.simulation.engine.RobotTokenHolder;
import com.aichuangzuo.admin.modules.simulation.engine.StageHandler;
import com.aichuangzuo.admin.modules.simulation.enums.SimulationStage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 登录阶段：预热 token 缓存。
 */
@Component
@RequiredArgsConstructor
public class LoginHandler implements StageHandler {

    private final RobotTokenHolder tokenHolder;

    @Override
    public SimulationStage stage() {
        return SimulationStage.LOGIN;
    }

    @Override
    public void execute(RobotContext ctx) {
        tokenHolder.token(ctx);
    }
}
