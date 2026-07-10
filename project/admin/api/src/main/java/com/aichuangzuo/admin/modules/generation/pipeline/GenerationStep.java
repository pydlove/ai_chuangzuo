package com.aichuangzuo.admin.modules.generation.pipeline;

/**
 * 12 阶段流水线的一个步骤。
 *
 * <p>实现类应该是无状态的（Spring 单例 bean），所有可变状态走 {@link GenerationContext}。
 */
public interface GenerationStep {

    /**
     * 阶段序号 1-12。pipeline 按序号升序调用。
     * PersistArticleStep 等非 12 阶段用 {@code > 12}（如 100）。
     */
    int stageIndex();

    /** 步骤名（日志用）。 */
    String name();

    /**
     * 是否启用：默认 true；可读 ctx 里的字段做条件启用。
     */
    default boolean enabled(GenerationContext ctx) {
        return true;
    }

    /**
     * 步骤主体。返回 {@link StepResult#CONTINUE} 让 pipeline 继续；
     * 抛任何异常都视为 {@link StepResult#FAIL}，pipeline 把它抛给 worker 走 retry / 退币。
     */
    StepResult process(GenerationContext ctx);
}
