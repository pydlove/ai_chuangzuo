-- 移除任务级自动重试：a_generation_task.max_retry 不再有消费方
-- markFailed 已改为始终置 FAILED；retry_count 仅作审计计数
-- 任务需要重跑由 admin 在「创作队列」点「重试」按钮（manualRetry）

ALTER TABLE a_generation_task
    DROP COLUMN max_retry;
