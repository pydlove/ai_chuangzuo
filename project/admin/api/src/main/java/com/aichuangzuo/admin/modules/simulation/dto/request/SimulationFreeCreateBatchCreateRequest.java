package com.aichuangzuo.admin.modules.simulation.dto.request;

import com.aichuangzuo.admin.modules.simulation.enums.PromptScope;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 模拟生成文章批次创建请求：随机抽取存量真实用户，随机使用市场提示词产生作者收益。
 */
@Data
public class SimulationFreeCreateBatchCreateRequest {

    /** 抽取的真实用户数。 */
    @NotNull
    @Min(1)
    @Max(500)
    private Integer userCount;

    /** 提示词发布者范围：REAL 真实用户 / ROBOT 机器人 / ALL 全部。 */
    @NotNull
    private PromptScope promptScope = PromptScope.REAL;

    /** 用户之间的执行间隔（秒，闭区间随机）。 */
    @Min(0)
    private int userIntervalMin = 10;
    @Min(0)
    private int userIntervalMax = 30;

    @Size(max = 200)
    private String remark;
}
