package com.aichuangzuo.admin.modules.simulation.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 模拟运营-自由创作提示词发布者范围。
 */
@Getter
@RequiredArgsConstructor
public enum PromptScope {

    /** 仅机器人发布的提示词。 */
    ROBOT(0),
    /** 所有人（不过滤）。 */
    ALL(null),
    /** 仅真实用户发布的提示词。 */
    REAL(1);

    /** 对应用户端 market-skills publisherType 参数；ALL 为 null（不过滤）。 */
    private final Integer userType;
}
