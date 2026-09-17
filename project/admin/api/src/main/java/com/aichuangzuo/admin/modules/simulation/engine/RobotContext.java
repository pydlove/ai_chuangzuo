package com.aichuangzuo.admin.modules.simulation.engine;

import com.aichuangzuo.admin.modules.simulation.client.SimulationUserApiClient;
import com.aichuangzuo.admin.modules.simulation.config.SimulationStageConfig;
import com.aichuangzuo.admin.modules.simulation.entity.SimulationBatch;
import com.aichuangzuo.admin.modules.simulation.entity.SimulationRobot;
import com.aichuangzuo.admin.modules.simulation.mapper.SimulationRobotMapper;
import com.aichuangzuo.admin.modules.simulation.service.SimulationLibraryService;
import com.aichuangzuo.admin.modules.simulation.service.SimulationProfileGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 单个机器人一次阶段执行所需的上下文。
 */
public class RobotContext {

    public final SimulationBatch batch;
    public final SimulationStageConfig config;
    public final SimulationRobot robot;
    public final String plainPassword;
    public final SimulationUserApiClient userApi;
    public final SimulationProfileGenerator profileGenerator;
    public final SimulationLibraryService libraryService;
    public final SimulationRobotMapper robotMapper;
    public final ObjectMapper objectMapper;

    public RobotContext(SimulationBatch batch,
                        SimulationStageConfig config,
                        SimulationRobot robot,
                        String plainPassword,
                        SimulationUserApiClient userApi,
                        SimulationProfileGenerator profileGenerator,
                        SimulationLibraryService libraryService,
                        SimulationRobotMapper robotMapper,
                        ObjectMapper objectMapper) {
        this.batch = batch;
        this.config = config;
        this.robot = robot;
        this.plainPassword = plainPassword;
        this.userApi = userApi;
        this.profileGenerator = profileGenerator;
        this.libraryService = libraryService;
        this.robotMapper = robotMapper;
        this.objectMapper = objectMapper;
    }
}
