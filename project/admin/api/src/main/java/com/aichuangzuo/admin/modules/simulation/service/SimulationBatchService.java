package com.aichuangzuo.admin.modules.simulation.service;

import com.aichuangzuo.admin.modules.earnings.vo.PageResult;
import com.aichuangzuo.admin.modules.simulation.dto.request.SimulationBatchCreateRequest;
import com.aichuangzuo.admin.modules.simulation.dto.request.SimulationBatchQueryRequest;
import com.aichuangzuo.admin.modules.simulation.vo.SimulationBatchDetailVO;
import com.aichuangzuo.admin.modules.simulation.vo.SimulationBatchVO;
import com.aichuangzuo.admin.modules.simulation.vo.SimulationRobotLogVO;

public interface SimulationBatchService {

    Long create(SimulationBatchCreateRequest request);

    PageResult<SimulationBatchVO> list(SimulationBatchQueryRequest request);

    SimulationBatchDetailVO detail(Long id);

    PageResult<SimulationRobotLogVO> logs(Long batchId, Long robotId, String stage, Long page, Long size);

    void cancel(Long id);
}
