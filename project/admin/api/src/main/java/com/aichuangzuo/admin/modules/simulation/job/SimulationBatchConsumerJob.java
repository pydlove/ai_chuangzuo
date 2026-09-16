package com.aichuangzuo.admin.modules.simulation.job;

import com.aichuangzuo.admin.modules.scheduler.annotation.ScheduledTask;
import com.aichuangzuo.admin.modules.scheduler.executor.ScheduledTaskExecutor;
import com.aichuangzuo.admin.modules.simulation.service.SimulationRobotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimulationBatchConsumerJob {

    private final SimulationRobotService robotService;
    private final ScheduledTaskExecutor scheduledTaskExecutor;

    @ScheduledTask(key = "simulation_batch_consumer", name = "模拟批次消费器",
            description = "每5秒推进一个模拟机器人的一个阶段", triggerType = "fixed_delay",
            expression = "5000", sortOrder = 40)
    @Scheduled(fixedDelay = 5000)
    public void run() {
        scheduledTaskExecutor.executeAuto("simulation_batch_consumer", robotService::tick);
    }
}
