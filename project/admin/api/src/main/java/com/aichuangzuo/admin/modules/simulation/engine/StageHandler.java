package com.aichuangzuo.admin.modules.simulation.engine;

import com.aichuangzuo.admin.modules.simulation.enums.SimulationStage;

/**
 * 模拟运营-机器人旅程阶段处理器。
 */
public interface StageHandler {

    SimulationStage stage();

    /** 执行一个阶段；前置不满足时抛 {@link StageSkippedException}（记 SKIPPED 并正常推进）。 */
    void execute(RobotContext ctx);
}
