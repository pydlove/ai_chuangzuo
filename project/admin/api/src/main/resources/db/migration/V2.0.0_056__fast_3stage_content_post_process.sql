SET NAMES utf8mb4;

-- 极速 3 阶段模板 stage 5：改造成可扩展的内容后处理阶段。
-- 当前启用规则：将 content / description 中的成对单引号替换为中文双引号。
-- 后续可通过修改 rule_config 追加更多后处理规则，而无需调整流水线序号。

UPDATE t_prompt_template_stage
   SET stage_key   = 'content_post_process',
       stage_type  = 'rule_config',
       ai_prompt   = NULL,
       rule_config = '{"singleQuoteToChineseQuotes": true}',
       enabled     = 1,
       updated_at  = CURRENT_TIMESTAMP(3)
 WHERE template_id = 2
   AND stage_index = 5;
