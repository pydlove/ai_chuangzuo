package com.aichuangzuo.admin.modules.simulation.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 模拟运营-批次类型。
 */
@Getter
@RequiredArgsConstructor
public enum SimulationBatchType {

    /** 机器人旅程：注册新机器人并按阶段走完整旅程。 */
    ROBOT_JOURNEY("机器人旅程"),
    /** 模拟生成文章：随机抽取存量真实用户，随机使用市场提示词产生作者收益（不真正生成文章）。 */
    FREE_CREATE("模拟生成文章");

    private final String description;
}
