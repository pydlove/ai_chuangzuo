package com.aichuangzuo.admin.modules.simulation.engine.handler;

import com.aichuangzuo.admin.modules.simulation.engine.RobotContext;
import com.aichuangzuo.admin.modules.simulation.engine.RobotTokenHolder;
import com.aichuangzuo.admin.modules.simulation.engine.StageHandler;
import com.aichuangzuo.admin.modules.simulation.enums.SimulationStage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 资料阶段：LLM 生成昵称+签名，网络随机图，走用户端资料更新接口。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileHandler implements StageHandler {

    private final RobotTokenHolder tokenHolder;

    @Override
    public SimulationStage stage() {
        return SimulationStage.PROFILE;
    }

    @Override
    public String execute(RobotContext ctx) {
        String nickname = ctx.profileGenerator.generateNickname();
        String bio = ctx.profileGenerator.generateBio(nickname);
        byte[] avatar = ctx.avatarFetcher.fetchRandomAvatar();
        tokenHolder.execute(ctx, token -> {
            ctx.userApi.updateNickname(token, nickname);
            ctx.userApi.updateProfileBio(token, bio);
            ctx.userApi.uploadAvatar(token, avatar, "avatar.jpg");
        });
        log.info("模拟机器人资料更新完成 robotId={} nickname={}", ctx.robot.getId(), nickname);
        return "昵称「" + nickname + "」";
    }
}
