package com.aichuangzuo.admin.modules.simulation.engine.handler;

import com.aichuangzuo.admin.modules.simulation.engine.RobotContext;
import com.aichuangzuo.admin.modules.simulation.engine.StageHandler;
import com.aichuangzuo.admin.modules.simulation.engine.StageSkippedException;
import com.aichuangzuo.admin.modules.simulation.enums.SimulationStage;
import com.aichuangzuo.admin.modules.skill.market.dto.SkillPickRowDTO;
import com.aichuangzuo.admin.modules.skill.market.mapper.SkillMarketStatsMapper;
import com.aichuangzuo.admin.modules.skill.market.service.SkillMarketUsageClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 模拟生成文章阶段：按发布者范围随机选一条市场提示词，调用 user-api 记录一次使用，
 * 使发布者获得创作币与收益明细；不真正创建生成任务，不消耗 token。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FreeCreateHandler implements StageHandler {

    private final SkillMarketStatsMapper statsMapper;
    private final SkillMarketUsageClient usageClient;

    @Override
    public SimulationStage stage() {
        return SimulationStage.FREE_CREATE;
    }

    @Override
    public String execute(RobotContext ctx) {
        Integer userType = ctx.config.getPromptScope().getUserType();
        SkillPickRowDTO skill = statsMapper.selectRandomEnabledSkill(userType);
        if (skill == null) {
            throw new StageSkippedException("无可选市场提示词");
        }
        usageClient.recordUsage(skill.getBizNo(), ctx.robot.getUserId());
        log.info("模拟生成文章 userId={} skill={} publisher={}",
                ctx.robot.getUserId(), skill.getBizNo(), skill.getPublisherUserId());
        return "提示词「" + skill.getSkillName() + "」 作者「" + skill.getPublisherNickname() + "」";
    }
}
