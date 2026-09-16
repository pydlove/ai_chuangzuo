package com.aichuangzuo.admin.modules.simulation.service;

import com.aichuangzuo.admin.modules.simulation.client.AvatarFetcher;
import com.aichuangzuo.admin.modules.simulation.client.SimulationUserApiClient;
import com.aichuangzuo.admin.modules.simulation.config.SimulationStageConfig;
import com.aichuangzuo.admin.modules.simulation.engine.RobotContext;
import com.aichuangzuo.admin.modules.simulation.engine.SimulationStageDispatcher;
import com.aichuangzuo.admin.modules.simulation.engine.StageSkippedException;
import com.aichuangzuo.admin.modules.simulation.entity.SimulationBatch;
import com.aichuangzuo.admin.modules.simulation.entity.SimulationRobot;
import com.aichuangzuo.admin.modules.simulation.entity.SimulationRobotLog;
import com.aichuangzuo.admin.modules.simulation.enums.SimulationBatchStatus;
import com.aichuangzuo.admin.modules.simulation.enums.SimulationRobotStatus;
import com.aichuangzuo.admin.modules.simulation.enums.SimulationStage;
import com.aichuangzuo.admin.modules.simulation.mapper.SimulationBatchMapper;
import com.aichuangzuo.admin.modules.simulation.mapper.SimulationRobotLogMapper;
import com.aichuangzuo.admin.modules.simulation.mapper.SimulationRobotMapper;
import com.aichuangzuo.shared.utils.AesUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 模拟运营-机器人推进服务：消费器每 tick 领取一个机器人推进一个阶段。
 *
 * <p>每 tick：先补完结批次，再取一条可执行机器人（WAITING 首次领取 /
 * IN_PROGRESS 到点），构建上下文交给调度器，按结果写日志并推进状态。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationRobotService {

    private final SimulationRobotMapper robotMapper;
    private final SimulationBatchMapper batchMapper;
    private final SimulationRobotLogMapper robotLogMapper;
    private final SimulationStageDispatcher dispatcher;
    private final SimulationUserApiClient userApi;
    private final SimulationProfileGenerator profileGenerator;
    private final AvatarFetcher avatarFetcher;
    private final ObjectMapper objectMapper;

    @Value("${simulation.password-secret}")
    private String passwordSecret;

    private final SecureRandom random = new SecureRandom();

    public void tick() {
        completeFinishedBatches();
        SimulationRobot robot = robotMapper.selectNextExecutable(LocalDateTime.now());
        if (robot == null) {
            return;
        }
        process(robot);
    }

    /**
     * 所有机器人已终态但批次仍在 RUNNING 的批次补完结。
     */
    private void completeFinishedBatches() {
        List<Long> batchIds = robotMapper.selectRunnableBatchIdsToComplete();
        LocalDateTime now = LocalDateTime.now();
        for (Long batchId : batchIds) {
            SimulationBatch batch = batchMapper.selectById(batchId);
            if (batch == null) {
                continue;
            }
            batch.setStatus(SimulationBatchStatus.COMPLETED.name());
            batch.setFinishedAt(now);
            batchMapper.updateById(batch);
            log.info("模拟批次自动完结 batchId={}", batchId);
        }
    }

    private void process(SimulationRobot robot) {
        LocalDateTime now = LocalDateTime.now();
        SimulationBatch batch = null;
        try {
            batch = doProcess(robot, now);
        } catch (Exception e) {
            // 兜底：任何未预期异常都要让机器人失败落库，避免卡在 IN_PROGRESS
            log.error("模拟机器人推进异常 robotId={} stage={}", robot.getId(), robot.getCurrentStage(), e);
            try {
                if (batch == null) {
                    batch = new SimulationBatch();
                    batch.setId(robot.getBatchId());
                }
                failStage(robot, batch, SimulationStage.valueOf(robot.getCurrentStage()), e);
            } catch (Exception inner) {
                log.error("模拟机器人失败落库也异常 robotId={}", robot.getId(), inner);
            }
        }
    }

    private SimulationBatch doProcess(SimulationRobot robot, LocalDateTime now) {
        SimulationBatch batch;
        if (SimulationRobotStatus.WAITING.name().equals(robot.getStatus())) {
            int claimed = robotMapper.claimWaiting(robot.getId(), now);
            if (claimed == 0) {
                return null;
            }
            robot.setStatus(SimulationRobotStatus.IN_PROGRESS.name());
            robot.setNextRunAt(now);
            batch = batchMapper.selectById(robot.getBatchId());
            // 批次内用户间隔：推迟其余 WAITING 机器人
            SimulationStageConfig batchConfig = parseStageConfig(batch.getStageConfig());
            robotMapper.deferWaitingRobots(robot.getBatchId(), now,
                    randomSeconds(batchConfig.getUserIntervalMin(), batchConfig.getUserIntervalMax()));
            batch.setStatus(SimulationBatchStatus.RUNNING.name());
            if (batch.getStartedAt() == null) {
                batch.setStartedAt(now);
            }
            batchMapper.updateById(batch);
        } else {
            batch = batchMapper.selectById(robot.getBatchId());
        }

        SimulationStageConfig config;
        String plainPassword = null;
        try {
            config = parseStageConfig(batch.getStageConfig());
            // 模拟生成文章批次面向存量真实用户，无初始密码，直接走内部使用接口
            if (!SimulationStage.FREE_CREATE.name().equals(robot.getCurrentStage())) {
                plainPassword = AesUtil.decrypt(robot.getPasswordEncrypted(), passwordSecret);
            }
        } catch (Exception e) {
            log.warn("模拟机器人上下文构建失败 robotId={}", robot.getId(), e);
            failStage(robot, batch, SimulationStage.valueOf(robot.getCurrentStage()), e);
            return batch;
        }

        RobotContext ctx = new RobotContext(batch, config, robot, plainPassword,
                userApi, profileGenerator, avatarFetcher, robotMapper, objectMapper);
        SimulationStage stage = SimulationStage.valueOf(robot.getCurrentStage());
        try {
            String detail = dispatcher.dispatch(ctx);
            advance(robot, batch, config, stage, "SUCCESS", detail, null);
        } catch (StageSkippedException e) {
            log.info("模拟机器人阶段跳过 robotId={} stage={} reason={}", robot.getId(), stage, e.getMessage());
            advance(robot, batch, config, stage, "SKIPPED", null, e.getMessage());
        } catch (Exception e) {
            log.warn("模拟机器人阶段失败 robotId={} stage={}", robot.getId(), stage, e);
            failStage(robot, batch, stage, e);
        }
        return batch;
    }

    /** 成功/跳过：写日志 → 推进到下一阶段或完结；机器人行的 userId/context 等变更一并落库。 */
    private void advance(SimulationRobot robot, SimulationBatch batch, SimulationStageConfig config,
                         SimulationStage stage, String status, String detail, String note) {
        LocalDateTime now = LocalDateTime.now();
        writeLog(robot, stage, status, detail, note);

        SimulationStage next = stage.next();
        if (next != null) {
            robot.setCurrentStage(next.name());
            robot.setNextRunAt(now.plusSeconds(
                    randomSeconds(config.getStageIntervalMin(), config.getStageIntervalMax())));
            robotMapper.updateById(robot);
            return;
        }
        robot.setStatus(SimulationRobotStatus.COMPLETED.name());
        robot.setFinishedAt(now);
        robotMapper.updateById(robot);
        incrementBatchCount(batch, true);
        log.info("模拟机器人旅程完成 robotId={} batchId={}", robot.getId(), robot.getBatchId());
    }

    /** 失败：写 FAILED 日志 → 机器人 FAILED + 批次失败数 +1。 */
    private void failStage(SimulationRobot robot, SimulationBatch batch, SimulationStage stage, Exception e) {
        LocalDateTime now = LocalDateTime.now();
        String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        if (reason.length() > 500) {
            reason = reason.substring(0, 500);
        }
        writeLog(robot, stage, "FAILED", null, reason);
        robot.setStatus(SimulationRobotStatus.FAILED.name());
        robot.setFailStage(stage.name());
        robot.setFailReason(reason);
        robot.setFinishedAt(now);
        robotMapper.updateById(robot);
        incrementBatchCount(batch, false);
    }

    private void incrementBatchCount(SimulationBatch batch, boolean completed) {
        if (completed) {
            batch.setCompletedCount((batch.getCompletedCount() == null ? 0 : batch.getCompletedCount()) + 1);
        } else {
            batch.setFailedCount((batch.getFailedCount() == null ? 0 : batch.getFailedCount()) + 1);
        }
        batchMapper.updateById(batch);
    }

    private void writeLog(SimulationRobot robot, SimulationStage stage, String status,
                          String detail, String errorMsg) {
        if (detail != null && detail.length() > 500) {
            detail = detail.substring(0, 500);
        }
        SimulationRobotLog logEntry = new SimulationRobotLog();
        logEntry.setRobotId(robot.getId());
        logEntry.setBatchId(robot.getBatchId());
        logEntry.setStage(stage.name());
        logEntry.setStatus(status);
        logEntry.setDetail(detail);
        logEntry.setErrorMsg(errorMsg);
        logEntry.setCreatedAt(LocalDateTime.now());
        robotLogMapper.insert(logEntry);
    }

    private SimulationStageConfig parseStageConfig(String json) {
        try {
            if (json != null && !json.isBlank()) {
                return objectMapper.readValue(json, SimulationStageConfig.class);
            }
        } catch (Exception e) {
            log.warn("模拟批次阶段配置解析失败，使用默认配置", e);
        }
        return new SimulationStageConfig();
    }

    private int randomSeconds(int min, int max) {
        if (min >= max) {
            return min;
        }
        return min + random.nextInt(max - min + 1);
    }
}
