package com.aichuangzuo.admin.modules.simulation.engine.handler;

import com.aichuangzuo.admin.modules.simulation.client.AvatarFetcher;
import com.aichuangzuo.admin.modules.simulation.engine.RobotContext;
import com.aichuangzuo.admin.modules.simulation.engine.RobotTokenHolder;
import com.aichuangzuo.admin.modules.simulation.engine.StageHandler;
import com.aichuangzuo.admin.modules.simulation.enums.SimulationStage;
import com.aichuangzuo.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 资料阶段：LLM 生成昵称+签名，pravatar 真人头像，走用户端资料更新接口。
 *
 * <p>昵称与头像编号均跨批次去重（查 a_simulation_robot 已用记录），
 * 昵称 LLM 生成最多重试 {@link #MAX_NICKNAME_TRIES} 次。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileHandler implements StageHandler {

    private static final int MAX_NICKNAME_TRIES = 5;

    private final RobotTokenHolder tokenHolder;

    @Override
    public SimulationStage stage() {
        return SimulationStage.PROFILE;
    }

    @Override
    public String execute(RobotContext ctx) {
        List<String> usedNicknames = new ArrayList<>(ctx.robotMapper.selectUsedNicknames());
        String nickname = null;
        for (int i = 0; i < MAX_NICKNAME_TRIES && nickname == null; i++) {
            String candidate = ctx.profileGenerator.generateNickname(usedNicknames);
            if (!usedNicknames.contains(candidate)) {
                nickname = candidate;
            }
        }
        if (nickname == null) {
            throw new BusinessException(500, "模拟运营昵称去重失败，请重试");
        }
        String finalNickname = nickname;
        String bio = ctx.profileGenerator.generateBio(finalNickname);
        Set<Integer> usedImgs = new HashSet<>(ctx.robotMapper.selectUsedAvatarImgs());
        AvatarFetcher.Avatar avatar = ctx.avatarFetcher.fetchRandomAvatar(usedImgs);
        tokenHolder.execute(ctx, token -> {
            ctx.userApi.updateNickname(token, finalNickname);
            ctx.userApi.updateProfileBio(token, bio);
            ctx.userApi.uploadAvatar(token, avatar.bytes(), "avatar.jpg");
        });
        ctx.robot.setNickname(finalNickname);
        ctx.robot.setAvatarImg(avatar.img());
        log.info("模拟机器人资料更新完成 robotId={} nickname={} avatarImg={}",
                ctx.robot.getId(), finalNickname, avatar.img());
        return "昵称「" + finalNickname + "」";
    }
}
