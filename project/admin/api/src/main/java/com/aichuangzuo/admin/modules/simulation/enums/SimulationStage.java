package com.aichuangzuo.admin.modules.simulation.enums;

import java.util.List;

/**
 * 模拟运营-机器人旅程阶段（按序执行）。
 */
public enum SimulationStage {

    REGISTER, LOGIN, PROFILE, LOTTERY, MEMBERSHIP, CREATE, COMMISSION;

    private static final List<SimulationStage> ORDER = List.of(values());

    /**
     * 下一个阶段；已是最后阶段返回 null。
     */
    public SimulationStage next() {
        int index = ORDER.indexOf(this) + 1;
        return index < ORDER.size() ? ORDER.get(index) : null;
    }
}
