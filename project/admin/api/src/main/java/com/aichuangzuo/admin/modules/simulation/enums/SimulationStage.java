package com.aichuangzuo.admin.modules.simulation.enums;

import java.util.List;

/**
 * 模拟运营-机器人旅程阶段（按序执行）。
 *
 * <p>FREE_CREATE 为「模拟生成文章」批次的独立单阶段，不属于旅程顺序。
 */
public enum SimulationStage {

    REGISTER, LOGIN, PROFILE, LOTTERY, MEMBERSHIP, CREATE, COMMISSION, FREE_CREATE;

    private static final List<SimulationStage> ORDER = List.of(
            REGISTER, LOGIN, PROFILE, LOTTERY, MEMBERSHIP, CREATE, COMMISSION);

    /**
     * 下一个阶段；已是最后阶段或不属于旅程（FREE_CREATE）返回 null。
     */
    public SimulationStage next() {
        int index = ORDER.indexOf(this) + 1;
        return index > 0 && index < ORDER.size() ? ORDER.get(index) : null;
    }
}
