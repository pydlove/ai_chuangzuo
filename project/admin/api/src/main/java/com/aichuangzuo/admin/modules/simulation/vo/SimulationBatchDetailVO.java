package com.aichuangzuo.admin.modules.simulation.vo;

import lombok.Data;

import java.util.List;

@Data
public class SimulationBatchDetailVO {

    private SimulationBatchVO batch;
    private List<SimulationRobotVO> robots;
}
