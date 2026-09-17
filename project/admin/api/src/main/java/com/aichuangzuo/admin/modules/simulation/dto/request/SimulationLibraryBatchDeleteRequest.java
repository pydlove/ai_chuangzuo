package com.aichuangzuo.admin.modules.simulation.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 模拟运营素材库（昵称/头像）批量删除请求。
 */
@Data
public class SimulationLibraryBatchDeleteRequest {

    @NotEmpty(message = "请选择要删除的记录")
    private List<Long> ids;
}
