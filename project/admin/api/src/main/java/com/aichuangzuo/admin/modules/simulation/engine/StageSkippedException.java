package com.aichuangzuo.admin.modules.simulation.engine;

/**
 * 阶段前置不满足（如没有进行中的抽奖活动），调度器捕获后记 SKIPPED 并正常推进。
 */
public class StageSkippedException extends RuntimeException {

    public StageSkippedException(String message) {
        super(message);
    }
}
