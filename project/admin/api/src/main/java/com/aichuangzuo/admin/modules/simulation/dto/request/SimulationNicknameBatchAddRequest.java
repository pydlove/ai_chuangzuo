package com.aichuangzuo.admin.modules.simulation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 昵称库批量添加请求：一行一个昵称，服务端去重去空白。
 */
@Data
public class SimulationNicknameBatchAddRequest {

    @NotNull
    @Size(min = 1, max = 2000)
    private List<String> nicknames;
}
