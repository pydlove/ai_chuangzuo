-- 移除任务级自动重试：max_retry 字段不再有消费方
-- markFailed 已改为始终置 FAILED；stage 内 AI 调用重试由 llmRetryMaxAttempts 控制
-- 任务需要重跑由 admin 在「创作队列」点「重试」按钮（manualRetry）

ALTER TABLE a_generation_config
    DROP COLUMN max_retry;
