SET NAMES utf8mb4;

-- 极速 3 阶段模板 stage 6：启用人类化改写节点。
-- 实际提示词存放于 c_ai_prompt（prompt_code = article_humanize_rewrite_v1），
-- 由 HumanizeRewriteStep 运行时读取；本处 ai_prompt 留空。

UPDATE t_prompt_template_stage
   SET stage_key   = 'humanize_rewrite',
       stage_type  = 'ai_prompt',
       ai_prompt   = NULL,
       rule_config = NULL,
       enabled     = 1,
       updated_at  = CURRENT_TIMESTAMP(3)
 WHERE template_id = 2
   AND stage_index = 6;

UPDATE t_prompt_template
   SET updated_at = CURRENT_TIMESTAMP(3)
 WHERE id = 2;
