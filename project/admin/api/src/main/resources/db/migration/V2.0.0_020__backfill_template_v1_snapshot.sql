-- 阶段 3 补充：为阶段 2 已发布但未生成版本快照的模板补 v1 快照。
--
-- 背景：V2.0.0_018 因 MySQL JSON_ARRAYAGG 内 ORDER BY 语法不兼容导致 INSERT 快照失败，
--       列和表已创建成功。本迁移仅执行缺失的快照回填，幂等（已存在 v1 则跳过）。
--
-- 设计文档：docs/superpowers/specs/2026-07-09-de-ai-flavor-writing-pipeline-design.md §5.14

SET NAMES utf8mb4;

INSERT INTO t_prompt_template_version
    (template_id, version, version_status, config_json, change_note,
     published_at, published_by, tenant_id, is_deleted, created_by, updated_by)
SELECT
    t.id,
    1,
    1,
    JSON_OBJECT(
        'stages', (
            SELECT JSON_ARRAYAGG(
                JSON_OBJECT(
                    'index', s.stage_index,
                    'stageKey', s.stage_key,
                    'stageType', s.stage_type,
                    'aiPrompt', s.ai_prompt,
                    'ruleConfig', s.rule_config,
                    'enabled', s.enabled
                )
            )
            FROM t_prompt_template_stage s
            WHERE s.template_id = t.id
        )
    ),
    'V2.0.0_020 补充：阶段 2 初始快照回填',
    COALESCE(t.updated_at, CURRENT_TIMESTAMP(3)),
    0,
    0,
    0,
    0,
    0
FROM t_prompt_template t
WHERE t.enabled = 1
  AND t.is_deleted = 0
  AND t.latest_published_version = 1
  AND (SELECT COUNT(*) FROM t_prompt_template_stage s WHERE s.template_id = t.id) = 12
  AND NOT EXISTS (
      SELECT 1 FROM t_prompt_template_version v
      WHERE v.template_id = t.id AND v.version = 1
  );
