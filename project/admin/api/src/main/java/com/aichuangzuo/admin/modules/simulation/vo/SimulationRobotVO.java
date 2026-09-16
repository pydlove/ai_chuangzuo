package com.aichuangzuo.admin.modules.simulation.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SimulationRobotVO {

    private Long id;
    private Long batchId;
    private Integer seq;
    private String email;
    private Long userId;
    private String inviteCode;
    private String status;
    private String currentStage;
    private LocalDateTime nextRunAt;
    private String failStage;
    private String failReason;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
