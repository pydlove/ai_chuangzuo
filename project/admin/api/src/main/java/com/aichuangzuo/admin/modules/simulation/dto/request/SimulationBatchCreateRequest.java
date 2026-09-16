package com.aichuangzuo.admin.modules.simulation.dto.request;

import com.aichuangzuo.admin.modules.simulation.enums.PromptScope;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SimulationBatchCreateRequest {

    @NotNull
    @Min(1)
    @Max(500)
    private Integer userCount;

    /** 会员套餐 key：basic/pro/flagship。 */
    @NotBlank
    private String planKey;

    /** 会员套餐名称快照。 */
    @NotBlank
    private String planName;

    /** 订阅周期：month/quarter/year。 */
    @NotBlank
    private String cycle;

    @NotNull
    private PromptScope promptScope = PromptScope.ALL;

    private boolean lotteryEnabled = true;
    @Min(0)
    @Max(100)
    private int lotteryProbability = 100;

    private boolean membershipEnabled = true;
    @Min(0)
    @Max(100)
    private int membershipProbability = 100;

    private boolean createEnabled = true;
    @Min(0)
    @Max(100)
    private int createProbability = 100;

    private boolean commissionEnabled = true;
    @Min(0)
    @Max(100)
    private int commissionProbability = 50;

    /** 机器人之间的执行间隔（秒，闭区间随机）。 */
    @Min(0)
    private int userIntervalMin = 10;
    @Min(0)
    private int userIntervalMax = 30;

    /** 同一机器人各阶段之间的间隔（秒，闭区间随机）。 */
    @Min(0)
    private int stageIntervalMin = 3;
    @Min(0)
    private int stageIntervalMax = 8;

    @Size(max = 200)
    private String remark;
}
