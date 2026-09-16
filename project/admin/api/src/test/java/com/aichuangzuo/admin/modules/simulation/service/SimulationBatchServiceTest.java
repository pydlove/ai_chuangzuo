package com.aichuangzuo.admin.modules.simulation.service;

import com.aichuangzuo.admin.modules.simulation.dto.request.SimulationBatchCreateRequest;
import com.aichuangzuo.admin.modules.simulation.entity.SimulationBatch;
import com.aichuangzuo.admin.modules.simulation.entity.SimulationRobot;
import com.aichuangzuo.admin.modules.simulation.enums.PromptScope;
import com.aichuangzuo.admin.modules.simulation.enums.SimulationRobotStatus;
import com.aichuangzuo.admin.modules.simulation.enums.SimulationStage;
import com.aichuangzuo.admin.modules.simulation.mapper.SimulationBatchMapper;
import com.aichuangzuo.admin.modules.simulation.mapper.SimulationRobotLogMapper;
import com.aichuangzuo.admin.modules.simulation.mapper.SimulationRobotMapper;
import com.aichuangzuo.admin.modules.simulation.service.impl.SimulationBatchServiceImpl;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.utils.AesUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimulationBatchServiceTest {

    private static final String SECRET = "0123456789abcdef";

    @Mock
    private SimulationBatchMapper batchMapper;
    @Mock
    private SimulationRobotMapper robotMapper;
    @Mock
    private SimulationRobotLogMapper robotLogMapper;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private SimulationBatchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SimulationBatchServiceImpl(batchMapper, robotMapper, robotLogMapper, objectMapper, SECRET);
    }

    @Test
    void createThrowsWhenUserIntervalMinGreaterThanMax() {
        SimulationBatchCreateRequest request = validRequest();
        request.setUserIntervalMin(30);
        request.setUserIntervalMax(10);

        assertThrows(BusinessException.class, () -> service.create(request));
    }

    @Test
    void createThrowsWhenStageIntervalMinGreaterThanMax() {
        SimulationBatchCreateRequest request = validRequest();
        request.setStageIntervalMin(8);
        request.setStageIntervalMax(3);

        assertThrows(BusinessException.class, () -> service.create(request));
    }

    @Test
    void createThrowsWhenUserCountExceeded() {
        SimulationBatchCreateRequest request = validRequest();
        request.setUserCount(501);

        assertThrows(BusinessException.class, () -> service.create(request));
    }

    @Test
    void createInsertsBatchAndRobots() throws Exception {
        SimulationBatchCreateRequest request = validRequest();
        request.setUserCount(3);
        when(batchMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(batchMapper.insert(any(SimulationBatch.class))).thenAnswer(inv -> {
            SimulationBatch b = inv.getArgument(0);
            if (b.getId() == null) {
                b.setId(100L);
            }
            return 1;
        });

        Long batchId = service.create(request);

        assertNotNull(batchId);
        ArgumentCaptor<SimulationBatch> batchCaptor = ArgumentCaptor.forClass(SimulationBatch.class);
        verify(batchMapper, times(1)).insert(batchCaptor.capture());
        verify(batchMapper).updateById(any(SimulationBatch.class));
        SimulationBatch batch = batchCaptor.getValue();
        assertEquals("SIM", batch.getBatchNo().substring(0, 3));
        assertEquals("PENDING", batch.getStatus());

        ArgumentCaptor<SimulationRobot> robotCaptor = ArgumentCaptor.forClass(SimulationRobot.class);
        verify(robotMapper, times(3)).insert(robotCaptor.capture());
        List<SimulationRobot> robots = robotCaptor.getAllValues();
        for (int i = 0; i < robots.size(); i++) {
            SimulationRobot robot = robots.get(i);
            assertEquals(i + 1, robot.getSeq());
            assertTrue(robot.getEmail().startsWith("bot"));
            assertTrue(robot.getEmail().endsWith(".simrobot.com"));
            assertFalse(robot.getEmail().contains(" "));
            assertEquals(SimulationRobotStatus.WAITING.name(), robot.getStatus());
            assertEquals(SimulationStage.REGISTER.name(), robot.getCurrentStage());
            assertNotNull(robot.getNextRunAt());
            // 密码必须 AES 加密（非明文 12 位字符），且可解密
            String plain = AesUtil.decrypt(robot.getPasswordEncrypted(), SECRET);
            assertEquals(12, plain.length());
        }
        assertEquals(3, batch.getTotalCount());
    }

    private SimulationBatchCreateRequest validRequest() {
        SimulationBatchCreateRequest request = new SimulationBatchCreateRequest();
        request.setUserCount(2);
        request.setPlanKey("pro");
        request.setPlanName("专业版");
        request.setCycle("month");
        request.setPromptScope(PromptScope.ALL);
        request.setLotteryEnabled(true);
        request.setLotteryProbability(100);
        request.setMembershipEnabled(true);
        request.setMembershipProbability(100);
        request.setCreateEnabled(true);
        request.setCreateProbability(100);
        request.setCommissionEnabled(true);
        request.setCommissionProbability(50);
        request.setUserIntervalMin(10);
        request.setUserIntervalMax(30);
        request.setStageIntervalMin(3);
        request.setStageIntervalMax(8);
        return request;
    }
}
