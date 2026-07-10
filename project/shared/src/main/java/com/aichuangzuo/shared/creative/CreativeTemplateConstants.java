package com.aichuangzuo.shared.creative;

/**
 * 默认创作模板（default-v1）相关常量。
 *
 * <p>设计文档：docs/superpowers/specs/2026-07-09-de-ai-flavor-writing-pipeline-design.md §5.10
 *
 * <p>Stage 1 起，内置模板由 Flyway 迁移 V2.0.0_017 写入 t_prompt_template(id=1, enabled=1)；
 * 12 阶段配置由 {@code PipelineTemplateResolver} 在 DB 未命中时回落到
 * {@code PipelineStage} enum 默认值兜底。
 */
public final class CreativeTemplateConstants {

    private CreativeTemplateConstants() {}

    /** 内置默认模板固定 ID，admin 不要删除，可在基础上复制派生。 */
    public static final long DEFAULT_TEMPLATE_ID = 1L;

    /** 默认模板名称（与迁移里 name 字段字符级一致）。 */
    public static final String DEFAULT_TEMPLATE_NAME = "默认去 AI 味模板";

    /** 12 阶段数量，与设计文档 5.10 锁定的结构一致。 */
    public static final int STAGE_COUNT = 12;

    /** classpath 资源路径，用于设计文档参考 / 未来配置导入。 */
    public static final String DEFAULT_TEMPLATE_JSON = "creative-template/default-v1.json";
}