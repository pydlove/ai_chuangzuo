SET NAMES utf8mb4;

-- 发布计划提示词增加约束，避免模型在 JSON 字符串值中生成未转义的双引号，导致解析失败。
UPDATE c_ai_prompt
SET user_prompt = REPLACE(user_prompt,
    '18. 第一个字符必须是 {，最后一个字符必须是 }。',
    '18. 第一个字符必须是 {，最后一个字符必须是 }。\n19. JSON 中所有字符串值禁止包含未转义的双引号 "，如需引用请使用单引号 '' 或中文引号「」，或直接去掉引号。'),
    updated_at = NOW()
WHERE prompt_code = 'publish_plan_guide_v1';
