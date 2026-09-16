package com.aichuangzuo.admin.modules.simulation.service.impl;

import com.aichuangzuo.admin.modules.earnings.vo.PageResult;
import com.aichuangzuo.admin.modules.simulation.config.SimulationStageConfig;
import com.aichuangzuo.admin.modules.simulation.dto.request.SimulationBatchCreateRequest;
import com.aichuangzuo.admin.modules.simulation.dto.request.SimulationBatchQueryRequest;
import com.aichuangzuo.admin.modules.simulation.entity.SimulationBatch;
import com.aichuangzuo.admin.modules.simulation.entity.SimulationRobot;
import com.aichuangzuo.admin.modules.simulation.entity.SimulationRobotLog;
import com.aichuangzuo.admin.modules.simulation.enums.AdminSimulationErrorCode;
import com.aichuangzuo.admin.modules.simulation.enums.SimulationBatchStatus;
import com.aichuangzuo.admin.modules.simulation.enums.SimulationRobotStatus;
import com.aichuangzuo.admin.modules.simulation.enums.SimulationStage;
import com.aichuangzuo.admin.modules.simulation.mapper.SimulationBatchMapper;
import com.aichuangzuo.admin.modules.simulation.mapper.SimulationRobotLogMapper;
import com.aichuangzuo.admin.modules.simulation.mapper.SimulationRobotMapper;
import com.aichuangzuo.admin.modules.simulation.service.SimulationBatchService;
import com.aichuangzuo.admin.modules.simulation.vo.SimulationBatchDetailVO;
import com.aichuangzuo.admin.modules.simulation.vo.SimulationBatchVO;
import com.aichuangzuo.admin.modules.simulation.vo.SimulationRobotLogVO;
import com.aichuangzuo.admin.modules.simulation.vo.SimulationRobotVO;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.utils.AesUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SimulationBatchServiceImpl implements SimulationBatchService {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final String[] EMAIL_WORDS = {
            "shanke", "xingguang", "moli", "qinghe", "luoye", "zhixia", "wanfeng", "suyu",
            "chunri", "qiuye", "baiyun", "hongdou", "zaoan", "muyu", "xingye", "qingkong"};

    private final SimulationBatchMapper batchMapper;
    private final SimulationRobotMapper robotMapper;
    private final SimulationRobotLogMapper robotLogMapper;
    private final ObjectMapper objectMapper;
    private final String passwordSecret;
    private final SecureRandom random = new SecureRandom();

    public SimulationBatchServiceImpl(SimulationBatchMapper batchMapper,
                                      SimulationRobotMapper robotMapper,
                                      SimulationRobotLogMapper robotLogMapper,
                                      ObjectMapper objectMapper,
                                      @Value("${simulation.password-secret}") String passwordSecret) {
        this.batchMapper = batchMapper;
        this.robotMapper = robotMapper;
        this.robotLogMapper = robotLogMapper;
        this.objectMapper = objectMapper;
        this.passwordSecret = passwordSecret;
    }

    @Override
    @Transactional
    public Long create(SimulationBatchCreateRequest request) {
        validate(request);

        SimulationStageConfig stageConfig = new SimulationStageConfig();
        stageConfig.setLotteryEnabled(request.isLotteryEnabled());
        stageConfig.setLotteryProbability(request.getLotteryProbability());
        stageConfig.setMembershipEnabled(request.isMembershipEnabled());
        stageConfig.setMembershipProbability(request.getMembershipProbability());
        stageConfig.setCreateEnabled(request.isCreateEnabled());
        stageConfig.setCreateProbability(request.getCreateProbability());
        stageConfig.setCommissionEnabled(request.isCommissionEnabled());
        stageConfig.setCommissionProbability(request.getCommissionProbability());
        stageConfig.setPromptScope(request.getPromptScope());
        stageConfig.setUserIntervalMin(request.getUserIntervalMin());
        stageConfig.setUserIntervalMax(request.getUserIntervalMax());
        stageConfig.setStageIntervalMin(request.getStageIntervalMin());
        stageConfig.setStageIntervalMax(request.getStageIntervalMax());

        String stageConfigJson;
        try {
            stageConfigJson = objectMapper.writeValueAsString(stageConfig);
        } catch (Exception e) {
            throw new BusinessException(AdminSimulationErrorCode.PARAM_INVALID.getCode(), "阶段配置序列化失败");
        }

        SimulationBatch batch = new SimulationBatch();
        batch.setBatchNo(nextBatchNo());
        batch.setUserCount(request.getUserCount());
        batch.setPlanKey(request.getPlanKey());
        batch.setPlanName(request.getPlanName());
        batch.setCycle(request.getCycle());
        batch.setStageConfig(stageConfigJson);
        batch.setStatus(SimulationBatchStatus.PENDING.name());
        batch.setTotalCount(0);
        batch.setCompletedCount(0);
        batch.setFailedCount(0);
        batch.setRemark(request.getRemark());
        batchMapper.insert(batch);

        String emailDomain = EMAIL_WORDS[random.nextInt(EMAIL_WORDS.length)];
        String batchTail = batch.getBatchNo().substring(batch.getBatchNo().length() - 8);
        LocalDateTime now = LocalDateTime.now();
        for (int i = 1; i <= request.getUserCount(); i++) {
            String plainPassword = generatePassword();
            String encrypted;
            try {
                encrypted = AesUtil.encrypt(plainPassword, passwordSecret);
            } catch (Exception e) {
                throw new BusinessException(AdminSimulationErrorCode.PARAM_INVALID.getCode(), "密码加密失败");
            }
            SimulationRobot robot = new SimulationRobot();
            robot.setBatchId(batch.getId());
            robot.setSeq(i);
            robot.setEmail(String.format("bot%s%04d@%s.simrobot.com", batchTail, i, emailDomain));
            robot.setPasswordEncrypted(encrypted);
            robot.setStatus(SimulationRobotStatus.WAITING.name());
            robot.setCurrentStage(SimulationStage.REGISTER.name());
            robot.setNextRunAt(now);
            robotMapper.insert(robot);
        }

        batch.setTotalCount(request.getUserCount());
        batchMapper.updateById(batch);
        log.info("模拟批次创建成功 batchId={} batchNo={} userCount={}", batch.getId(), batch.getBatchNo(), request.getUserCount());
        return batch.getId();
    }

    @Override
    public PageResult<SimulationBatchVO> list(SimulationBatchQueryRequest request) {
        LambdaQueryWrapper<SimulationBatch> wrapper = new LambdaQueryWrapper<SimulationBatch>()
                .eq(request.getStatus() != null && !request.getStatus().isBlank(), SimulationBatch::getStatus, request.getStatus())
                .orderByDesc(SimulationBatch::getId);
        Page<SimulationBatch> page = batchMapper.selectPage(new Page<>(request.getPage(), request.getSize()), wrapper);
        List<SimulationBatchVO> items = page.getRecords().stream().map(this::toBatchVO).toList();
        return new PageResult<>(items, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public SimulationBatchDetailVO detail(Long id) {
        SimulationBatch batch = getBatch(id);
        List<SimulationRobot> robots = robotMapper.selectList(new LambdaQueryWrapper<SimulationRobot>()
                .eq(SimulationRobot::getBatchId, id)
                .orderByAsc(SimulationRobot::getSeq));
        SimulationBatchDetailVO detail = new SimulationBatchDetailVO();
        detail.setBatch(toBatchVO(batch));
        List<SimulationRobotVO> robotVOs = new ArrayList<>();
        for (SimulationRobot robot : robots) {
            robotVOs.add(toRobotVO(robot));
        }
        detail.setRobots(robotVOs);
        return detail;
    }

    @Override
    public PageResult<SimulationRobotLogVO> logs(Long batchId, Long robotId, String stage, Long page, Long size) {
        getBatch(batchId);
        LambdaQueryWrapper<SimulationRobotLog> wrapper = new LambdaQueryWrapper<SimulationRobotLog>()
                .eq(SimulationRobotLog::getBatchId, batchId)
                .eq(robotId != null, SimulationRobotLog::getRobotId, robotId)
                .eq(stage != null && !stage.isBlank(), SimulationRobotLog::getStage, stage)
                .orderByDesc(SimulationRobotLog::getId);
        Page<SimulationRobotLog> result = robotLogMapper.selectPage(new Page<>(page, size), wrapper);
        List<SimulationRobotLogVO> items = result.getRecords().stream().map(this::toLogVO).toList();
        return new PageResult<>(items, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public void cancel(Long id) {
        SimulationBatch batch = getBatch(id);
        String status = batch.getStatus();
        if (!SimulationBatchStatus.PENDING.name().equals(status)
                && !SimulationBatchStatus.RUNNING.name().equals(status)) {
            throw new BusinessException(AdminSimulationErrorCode.BATCH_STATUS_INVALID);
        }
        LocalDateTime now = LocalDateTime.now();
        batch.setStatus(SimulationBatchStatus.CANCELED.name());
        batch.setFinishedAt(now);
        batchMapper.updateById(batch);
        robotMapper.cancelOpenRobots(id, now);
        log.info("模拟批次已取消 batchId={}", id);
    }

    private void validate(SimulationBatchCreateRequest request) {
        if (request.getUserCount() == null || request.getUserCount() < 1 || request.getUserCount() > 500) {
            throw new BusinessException(AdminSimulationErrorCode.PARAM_INVALID.getCode(), "机器人数必须在1-500之间");
        }
        if (request.getUserIntervalMin() > request.getUserIntervalMax()) {
            throw new BusinessException(AdminSimulationErrorCode.PARAM_INVALID.getCode(), "用户间隔最小值不能大于最大值");
        }
        if (request.getStageIntervalMin() > request.getStageIntervalMax()) {
            throw new BusinessException(AdminSimulationErrorCode.PARAM_INVALID.getCode(), "阶段间隔最小值不能大于最大值");
        }
    }

    private String nextBatchNo() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String prefix = "SIM" + date;
        Long todayCount = batchMapper.selectCount(new LambdaQueryWrapper<SimulationBatch>()
                .likeRight(SimulationBatch::getBatchNo, prefix));
        return String.format("%s%03d", prefix, todayCount + 1);
    }

    private String generatePassword() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    private SimulationBatch getBatch(Long id) {
        SimulationBatch batch = batchMapper.selectById(id);
        if (batch == null) {
            throw new BusinessException(AdminSimulationErrorCode.BATCH_NOT_FOUND);
        }
        return batch;
    }

    private SimulationBatchVO toBatchVO(SimulationBatch batch) {
        SimulationBatchVO vo = new SimulationBatchVO();
        vo.setId(batch.getId());
        vo.setBatchNo(batch.getBatchNo());
        vo.setUserCount(batch.getUserCount());
        vo.setPlanKey(batch.getPlanKey());
        vo.setPlanName(batch.getPlanName());
        vo.setCycle(batch.getCycle());
        vo.setStageConfig(batch.getStageConfig());
        vo.setStatus(batch.getStatus());
        vo.setTotalCount(batch.getTotalCount());
        vo.setCompletedCount(batch.getCompletedCount());
        vo.setFailedCount(batch.getFailedCount());
        vo.setRemark(batch.getRemark());
        vo.setCreatedAt(batch.getCreatedAt());
        vo.setStartedAt(batch.getStartedAt());
        vo.setFinishedAt(batch.getFinishedAt());
        return vo;
    }

    private SimulationRobotVO toRobotVO(SimulationRobot robot) {
        SimulationRobotVO vo = new SimulationRobotVO();
        vo.setId(robot.getId());
        vo.setBatchId(robot.getBatchId());
        vo.setSeq(robot.getSeq());
        vo.setEmail(robot.getEmail());
        vo.setUserId(robot.getUserId());
        vo.setInviteCode(robot.getInviteCode());
        vo.setStatus(robot.getStatus());
        vo.setCurrentStage(robot.getCurrentStage());
        vo.setNextRunAt(robot.getNextRunAt());
        vo.setFailStage(robot.getFailStage());
        vo.setFailReason(robot.getFailReason());
        vo.setStartedAt(robot.getStartedAt());
        vo.setFinishedAt(robot.getFinishedAt());
        return vo;
    }

    private SimulationRobotLogVO toLogVO(SimulationRobotLog log) {
        SimulationRobotLogVO vo = new SimulationRobotLogVO();
        vo.setId(log.getId());
        vo.setRobotId(log.getRobotId());
        vo.setBatchId(log.getBatchId());
        vo.setStage(log.getStage());
        vo.setStatus(log.getStatus());
        vo.setDetail(log.getDetail());
        vo.setErrorMsg(log.getErrorMsg());
        vo.setCreatedAt(log.getCreatedAt());
        return vo;
    }
}
