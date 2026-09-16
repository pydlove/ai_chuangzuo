package com.aichuangzuo.admin.modules.simulation.config;

import com.aichuangzuo.admin.modules.simulation.enums.PromptScope;
import lombok.Data;

/**
 * 模拟运营-批次阶段配置（Jackson 序列化为 a_simulation_batch.stage_config）。
 */
@Data
public class SimulationStageConfig {

    private boolean lotteryEnabled = true;
    private int lotteryProbability = 100;
    private boolean membershipEnabled = true;
    private int membershipProbability = 100;
    private boolean createEnabled = true;
    private int createProbability = 100;
    private boolean commissionEnabled = true;
    private int commissionProbability = 50;

    /** 自由创作提示词发布者范围。 */
    private PromptScope promptScope = PromptScope.ALL;

    /** 机器人之间的执行间隔（秒，闭区间随机）。 */
    private int userIntervalMin = 10;
    private int userIntervalMax = 30;

    /** 同一机器人各阶段之间的间隔（秒，闭区间随机）。 */
    private int stageIntervalMin = 3;
    private int stageIntervalMax = 8;
}
