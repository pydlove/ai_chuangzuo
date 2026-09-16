package com.aichuangzuo.admin.modules.simulation.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SimulationRobotLogVO {

    private Long id;
    private Long robotId;
    private Long batchId;
    private String stage;
    private String status;
    private String detail;
    private String errorMsg;
    private LocalDateTime createdAt;
}
