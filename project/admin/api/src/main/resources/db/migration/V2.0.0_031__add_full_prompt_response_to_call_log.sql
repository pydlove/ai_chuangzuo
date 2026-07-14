-- 创作 AI 调用日志：把截断 preview 列升级为全文列，存储实际发给 AI 的 userMsg 和 AI 完整返回
ALTER TABLE a_generation_call_log
    ADD COLUMN user_msg TEXT NULL COMMENT '本次尝试完整 userMsg（变量已替换）' AFTER error,
    ADD COLUMN response_content TEXT NULL COMMENT 'AI 完整返回（成功时）' AFTER user_msg,
    DROP COLUMN user_msg_preview,
    DROP COLUMN response_preview;