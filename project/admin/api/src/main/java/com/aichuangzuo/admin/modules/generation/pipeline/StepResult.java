package com.aichuangzuo.admin.modules.generation.pipeline;

/**
 * 流水线 step 执行结果。
 *
 * <ul>
 *   <li>{@link #CONTINUE} — 成功，继续下一个 step</li>
 *   <li>{@link #STOP} — 成功但要求 pipeline 提前结束（目前用不到，预留）</li>
 *   <li>{@link #FAIL} — 失败，pipeline 会抛异常让 worker 走 retry / 退币</li>
 * </ul>
 */
public enum StepResult {
    CONTINUE,
    STOP,
    FAIL
}
