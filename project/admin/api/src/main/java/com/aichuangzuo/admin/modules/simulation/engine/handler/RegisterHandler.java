package com.aichuangzuo.admin.modules.simulation.engine.handler;

import com.aichuangzuo.admin.modules.simulation.engine.RobotContext;
import com.aichuangzuo.admin.modules.simulation.engine.StageHandler;
import com.aichuangzuo.admin.modules.simulation.enums.SimulationStage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 注册阶段：绑随机机器人邀请码（可空）→ 走用户端内部接口创建机器人账号。
 */
@Slf4j
@Component
public class RegisterHandler implements StageHandler {

    @Override
    public SimulationStage stage() {
        return SimulationStage.REGISTER;
    }

    @Override
    public void execute(RobotContext ctx) {
        List<String> codes = ctx.userApi.randomRobotInviteCodes(1);
        String inviteCode = codes.isEmpty() ? null : codes.get(0);
        var created = ctx.userApi.createRobot(ctx.robot.getEmail(), ctx.plainPassword, inviteCode);
        ctx.robot.setUserId(created.userId());
        ctx.robot.setInviteCode(inviteCode);
        log.info("模拟机器人注册成功 robotId={} userId={} inviteCode={}",
                ctx.robot.getId(), created.userId(), inviteCode);
    }
}
