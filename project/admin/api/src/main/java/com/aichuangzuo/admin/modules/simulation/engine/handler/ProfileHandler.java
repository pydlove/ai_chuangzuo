package com.aichuangzuo.admin.modules.simulation.engine.handler;

import com.aichuangzuo.admin.modules.simulation.engine.RobotContext;
import com.aichuangzuo.admin.modules.simulation.engine.RobotTokenHolder;
import com.aichuangzuo.admin.modules.simulation.engine.StageHandler;
import com.aichuangzuo.admin.modules.simulation.enums.SimulationStage;
import com.aichuangzuo.admin.modules.simulation.service.SimulationLibraryService;
import com.aichuangzuo.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 资料阶段：昵称/头像从素材库随机取用（用后即删），签名由 LLM 按昵称生成，走用户端资料更新接口。
 *
 * <p>素材库为空时直接失败并提示补充，避免机器人静默缺少资料。
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
        String nickname = ctx.libraryService.claimNickname();
        if (nickname == null) {
            throw new BusinessException(500, "模拟运营昵称库已用完，请先上传昵称");
        }
        SimulationLibraryService.AvatarPick avatar = ctx.libraryService.claimAvatar();
        if (avatar == null) {
            throw new BusinessException(500, "模拟运营头像库已用完，请先上传头像");
        }
        String bio = generateBioQuietly(ctx, nickname);
        String finalNickname = nickname;
        tokenHolder.execute(ctx, token -> {
            ctx.userApi.updateNickname(token, finalNickname);
            if (bio != null) {
                ctx.userApi.updateProfileBio(token, bio);
            }
            ctx.userApi.uploadAvatar(token, avatar.bytes(), "avatar.jpg");
        });
        ctx.robot.setNickname(finalNickname);
        ctx.robot.setAvatarImg(avatar.id().intValue());
        log.info("模拟机器人资料更新完成 robotId={} nickname={} avatarId={} bio={}",
                ctx.robot.getId(), finalNickname, avatar.id(), bio != null ? "ok" : "skipped");
        return "昵称「" + finalNickname + "」";
    }

    /** 签名尽力而为：AI 不可用时跳过签名，不阻断资料阶段。 */
    private String generateBioQuietly(RobotContext ctx, String nickname) {
        try {
            return ctx.profileGenerator.generateBio(nickname);
        } catch (Exception e) {
            log.warn("模拟机器人签名生成失败，跳过签名 nickname={} err={}", nickname, e.getMessage());
            return null;
        }
    }
}
