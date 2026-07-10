-- 创作提示词模板 12 阶段配置（对应设计文档 2026-07-09-de-ai-flavor-writing-pipeline-design.md）
-- 一个模板对应 12 行 stage（index 1-12），每行存对应阶段的 prompt 或 rule config
-- 老模板（V2.0.0_011 创建的 t_prompt_template 行）不会自动建 stage，
-- admin 在编辑页点「初始化 12 阶段」才会补齐（见 POST /prompt-templates/{id}/init-stages）

CREATE TABLE IF NOT EXISTS t_prompt_template_stage (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    template_id BIGINT UNSIGNED NOT NULL COMMENT '所属模板 ID（t_prompt_template.id）',
    stage_index TINYINT UNSIGNED NOT NULL COMMENT '阶段序号 1-12（设计文档固定 12 阶段）',
    stage_type VARCHAR(16) NOT NULL COMMENT 'ai_prompt / rule_config / passthrough',
    stage_key VARCHAR(32) NOT NULL COMMENT '阶段稳定标识符：outline / material_list / draft / rhythm_detect / ...',
    ai_prompt MEDIUMTEXT COMMENT '仅 stage_type=ai_prompt 有值：可编辑的 AI 提示词模板',
    rule_config JSON COMMENT '仅 stage_type=rule_config 有值：规则参数（JSON）',
    enabled TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '该 stage 是否启用（admin 可单 stage 关停）',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID（=0）',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_template_stage (template_id, stage_index),
    KEY idx_stage_template (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='创作提示词模板 12 阶段配置';
