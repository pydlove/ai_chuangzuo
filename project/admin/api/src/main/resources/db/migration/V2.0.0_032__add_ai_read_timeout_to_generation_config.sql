-- 创作运行时配置增加 AI 读取超时（秒），默认 180
ALTER TABLE a_generation_config
    ADD COLUMN ai_read_timeout_seconds INT UNSIGNED NOT NULL DEFAULT 180 COMMENT 'AI 调用读取超时（秒）' AFTER default_top_p;