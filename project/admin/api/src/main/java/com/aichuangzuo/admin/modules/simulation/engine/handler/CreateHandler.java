package com.aichuangzuo.admin.modules.simulation.engine.handler;

import com.aichuangzuo.admin.modules.simulation.client.SimulationUserApiClient.MarketSkill;
import com.aichuangzuo.admin.modules.simulation.engine.RobotContext;
import com.aichuangzuo.admin.modules.simulation.engine.RobotTokenHolder;
import com.aichuangzuo.admin.modules.simulation.engine.StageHandler;
import com.aichuangzuo.admin.modules.simulation.engine.StageSkippedException;
import com.aichuangzuo.admin.modules.simulation.enums.SimulationStage;
import com.aichuangzuo.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 自由创作阶段：按发布者范围随机选市场提示词 → 创建生成任务 → 轮询完成 → 记录文章 bizNo。
 *
 * <p>skillRef 传市场提示词 bizNo，使发布者获得真实收益分成。轮询间隔 5s、上限 60 次。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateHandler implements StageHandler {

    private static final long POLL_INTERVAL_MS = 5000;
    private static final int MAX_POLLS = 60;

    private final RobotTokenHolder tokenHolder;

    @Override
    public SimulationStage stage() {
        return SimulationStage.CREATE;
    }

    @Override
    public void execute(RobotContext ctx) {
        tokenHolder.execute(ctx, token -> {
            List<MarketSkill> skills = ctx.userApi.listMarketSkills(
                    token, ctx.config.getPromptScope().getUserType(), 50);
            if (skills.isEmpty()) {
                throw new StageSkippedException("无可选市场提示词");
            }
            MarketSkill skill = skills.get(ThreadLocalRandom.current().nextInt(skills.size()));
            String title = "模拟运营自动创作-" + ctx.robot.getSeq();
            Long taskId = ctx.userApi.createGenerationTask(token, skill.bizNo(), title);
            log.info("模拟机器人创建生成任务 robotId={} taskId={} skill={}",
                    ctx.robot.getId(), taskId, skill.bizNo());

            String articleBizNo = pollUntilDone(ctx, token, taskId);
            ObjectNode node = ctx.objectMapper.createObjectNode();
            node.put("generationTaskId", taskId);
            node.put("articleBizNo", articleBizNo);
            node.put("marketSkillBizNo", skill.bizNo());
            ctx.robot.setContext(node.toString());
            log.info("模拟机器人创作完成 robotId={} taskId={} articleBizNo={}",
                    ctx.robot.getId(), taskId, articleBizNo);
        });
    }

    private String pollUntilDone(RobotContext ctx, String token, Long taskId) {
        for (int i = 0; i < MAX_POLLS; i++) {
            int status = ctx.userApi.generationTaskStatus(token, taskId);
            if (status == 2) {
                return ctx.userApi.generationTaskArticleBizNo(token, taskId);
            }
            if (status == 3) {
                throw new BusinessException(500, "模拟运营生成任务失败 taskId=" + taskId);
            }
            sleep();
        }
        throw new BusinessException(500, "模拟运营生成任务轮询超时 taskId=" + taskId);
    }

    private void sleep() {
        try {
            Thread.sleep(POLL_INTERVAL_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(500, "模拟运营轮询被中断");
        }
    }
}
