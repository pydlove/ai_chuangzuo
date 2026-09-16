package com.aichuangzuo.admin.modules.simulation.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SimulationBatchVO {

    private Long id;
    private String batchNo;
    private Integer userCount;
    private String planKey;
    private String planName;
    private String cycle;

    /** 批次类型：ROBOT_JOURNEY / FREE_CREATE。 */
    private String batchType;

    /** 阶段配置 JSON（原样透出）。 */
    private String stageConfig;

    private String status;
    private Integer totalCount;
    private Integer completedCount;
    private Integer failedCount;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
