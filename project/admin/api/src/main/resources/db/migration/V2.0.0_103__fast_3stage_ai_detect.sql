SET NAMES utf8mb4;

-- 极速 3 阶段模板 stage 14：启用 AI 率自评节点。
-- 元数据来自 PipelineStage.AI_DETECT，此处 ai_prompt 可留空（走 enum 默认值）。

INSERT INTO t_prompt_template_stage (
    template_id, stage_index, stage_type, stage_key, ai_prompt, rule_config, model_params, enabled,
    tenant_id, is_deleted, created_by, updated_by
) VALUES (
    2, 14, 'ai_prompt', 'ai_detect', NULL, NULL, NULL, 1,
    0, 0, 0, 0
)
ON DUPLICATE KEY UPDATE
    stage_type  = VALUES(stage_type),
    stage_key   = VALUES(stage_key),
    ai_prompt   = VALUES(ai_prompt),
    rule_config = VALUES(rule_config),
    model_params = VALUES(model_params),
    enabled     = VALUES(enabled),
    updated_at  = CURRENT_TIMESTAMP(3);

UPDATE t_prompt_template
   SET updated_at = CURRENT_TIMESTAMP(3)
 WHERE id = 2;
