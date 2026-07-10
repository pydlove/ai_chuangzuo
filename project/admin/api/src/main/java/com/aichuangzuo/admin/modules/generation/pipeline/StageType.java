package com.aichuangzuo.admin.modules.generation.pipeline;

/**
 * 流水线阶段类型。
 *
 * <p>对应设计文档 2026-07-09-de-ai-flavor-writing-pipeline-design.md：
 * <ul>
 *   <li>{@link #AI_PROMPT} — 调 AI 完成的阶段，可编辑 prompt</li>
 *   <li>{@link #RULE_CONFIG} — 程序化规则阶段，无 AI 调用，可配置阈值</li>
 *   <li>{@link #PASSTHROUGH} — 纯组装/无副作用阶段，不可配置</li>
 * </ul>
 */
public enum StageType {
    /** AI 提示词阶段（8 个：2, 3, 4, 6, 7, 8, 9, 11）。 */
    AI_PROMPT("ai_prompt", "AI"),
    /** 规则配置阶段（3 个：5, 10, 12）。 */
    RULE_CONFIG("rule_config", "规则"),
    /** 透传阶段（1 个：1）。 */
    PASSTHROUGH("passthrough", "系统");

    public final String code;
    public final String label;

    StageType(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
