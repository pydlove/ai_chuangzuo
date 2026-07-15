-- 创作 AI 调用日志加 token 消耗字段
-- 每次成功调用记录 usage；失败时三个字段都为 null

ALTER TABLE a_generation_call_log
    ADD COLUMN prompt_tokens INT UNSIGNED DEFAULT NULL COMMENT 'prompt tokens（成功时有值）' AFTER duration_ms,
    ADD COLUMN completion_tokens INT UNSIGNED DEFAULT NULL COMMENT 'completion tokens（成功时有值）' AFTER prompt_tokens,
    ADD COLUMN total_tokens INT UNSIGNED DEFAULT NULL COMMENT '总 tokens = prompt + completion' AFTER completion_tokens;
