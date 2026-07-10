-- 创作运行时配置：AI 失败重试参数（按设计文档 §4 失败重试）

ALTER TABLE a_generation_config
    ADD COLUMN llm_retry_max_attempts     INT UNSIGNED NOT NULL DEFAULT 3
        COMMENT '单 stage AI 调用最大尝试次数（含首次；1-10）',
    ADD COLUMN llm_retry_base_delay_ms    INT UNSIGNED NOT NULL DEFAULT 500
        COMMENT '首次重试前等待 ms（100-10000）',
    ADD COLUMN llm_retry_backoff_multiplier INT UNSIGNED NOT NULL DEFAULT 2
        COMMENT '指数退避倍数（1-5：第 N 次重试睡 baseDelay * multiplier^(N-1)）';
