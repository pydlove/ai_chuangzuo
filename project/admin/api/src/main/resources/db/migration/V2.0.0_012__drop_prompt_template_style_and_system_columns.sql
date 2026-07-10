-- 创作提示词模板重构：删除 ② 用户风格引导 / ③ 系统提示词 两列
-- ② 改由 u_user_style.prompt 在用户提交任务时快照到 input_param 提供
-- ③ 改为 Java 端 PromptConstants.SYSTEM_PROMPT_JSON 硬编码常量

ALTER TABLE t_prompt_template
    DROP COLUMN user_style_guidance,
    DROP COLUMN system_prompt_json;
