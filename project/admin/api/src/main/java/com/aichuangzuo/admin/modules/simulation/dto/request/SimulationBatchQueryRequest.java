package com.aichuangzuo.admin.modules.simulation.dto.request;

import lombok.Data;

@Data
public class SimulationBatchQueryRequest {

    /** 状态过滤：PENDING/RUNNING/COMPLETED/CANCELED，空查全部。 */
    private String status;

    private Long page = 1L;
    private Long size = 20L;
}
