SET NAMES utf8mb4;

-- 极速 3 阶段模板 stage 5：内容后处理增加角引号替换规则。
-- 将 content / description 中的日文角引号 「 / 」 替换为中文双引号 “ / ”。

UPDATE t_prompt_template_stage
   SET rule_config = '{"singleQuoteToChineseQuotes": true, "cornerBracketToChineseQuotes": true}',
       updated_at  = CURRENT_TIMESTAMP(3)
 WHERE template_id = 2
   AND stage_index = 5
   AND stage_key   = 'content_post_process';
