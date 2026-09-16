package com.aichuangzuo.admin.modules.simulation.engine;

import com.aichuangzuo.admin.modules.simulation.config.SimulationStageConfig;
import com.aichuangzuo.admin.modules.simulation.entity.SimulationBatch;
import com.aichuangzuo.admin.modules.simulation.entity.SimulationRobot;
import com.aichuangzuo.admin.modules.simulation.enums.PromptScope;
import com.aichuangzuo.admin.modules.simulation.enums.SimulationStage;
import com.aichuangzuo.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SimulationStageDispatcherTest {

    private final StageHandler registerHandler = mockHandler(SimulationStage.REGISTER);
    private final StageHandler loginHandler = mockHandler(SimulationStage.LOGIN);
    private final StageHandler lotteryHandler = mockHandler(SimulationStage.LOTTERY);
    private final StageHandler commissionHandler = mockHandler(SimulationStage.COMMISSION);

    private final SimulationStageDispatcher dispatcher = new SimulationStageDispatcher(
            List.of(registerHandler, loginHandler, lotteryHandler, commissionHandler));

    @Test
    void registerAlwaysExecutes() {
        RobotContext ctx = ctx(SimulationStage.REGISTER, configWithLottery(0));

        dispatcher.dispatch(ctx);

        verify(registerHandler).execute(ctx);
    }

    @Test
    void lotterySkippedWhenProbabilityZero() {
        RobotContext ctx = ctx(SimulationStage.LOTTERY, configWithLottery(0));

        assertThrows(StageSkippedException.class, () -> dispatcher.dispatch(ctx));
        verify(lotteryHandler, never()).execute(ctx);
    }

    @Test
    void lotteryDispatchedWhenProbabilityHundred() {
        RobotContext ctx = ctx(SimulationStage.LOTTERY, configWithLottery(100));

        dispatcher.dispatch(ctx);

        verify(lotteryHandler).execute(ctx);
    }

    @Test
    void commissionSkippedWhenDisabled() {
        SimulationStageConfig config = new SimulationStageConfig();
        config.setCommissionEnabled(false);
        config.setCommissionProbability(100);
        RobotContext ctx = ctx(SimulationStage.COMMISSION, config);

        assertThrows(StageSkippedException.class, () -> dispatcher.dispatch(ctx));
        verify(commissionHandler, never()).execute(ctx);
    }

    @Test
    void unknownStageWithoutHandlerThrowsBusinessException() {
        RobotContext ctx = ctx(SimulationStage.PROFILE, new SimulationStageConfig());

        BusinessException e = assertThrows(BusinessException.class, () -> dispatcher.dispatch(ctx));
        assertEquals(500, e.getCode());
    }

    private static StageHandler mockHandler(SimulationStage stage) {
        StageHandler handler = org.mockito.Mockito.mock(StageHandler.class);
        when(handler.stage()).thenReturn(stage);
        return handler;
    }

    private static SimulationStageConfig configWithLottery(int probability) {
        SimulationStageConfig config = new SimulationStageConfig();
        config.setLotteryEnabled(true);
        config.setLotteryProbability(probability);
        config.setPromptScope(PromptScope.ALL);
        return config;
    }

    private static RobotContext ctx(SimulationStage stage, SimulationStageConfig config) {
        SimulationRobot robot = new SimulationRobot();
        robot.setId(1L);
        robot.setCurrentStage(stage.name());
        SimulationBatch batch = new SimulationBatch();
        batch.setId(1L);
        return new RobotContext(batch, config, robot, "password",
                null, null, null, null);
    }
}
