-- 创作运行时配置：AI 调用默认参数（max_tokens / temperature / top_p）
-- stage 的 model_params 优先；未配置时回落到这里的全局默认

ALTER TABLE a_generation_config
    ADD COLUMN default_temperature DECIMAL(3,2) NOT NULL DEFAULT 0.70
        COMMENT 'AI temperature 默认值（0.00-2.00；stage model_params 可覆盖）',
    ADD COLUMN default_max_tokens INT UNSIGNED NOT NULL DEFAULT 8192
        COMMENT 'AI max_tokens 默认值（1-128000；MiniMax-M3 等推理模型 reasoning 也吃此预算）',
    ADD COLUMN default_top_p DECIMAL(3,2) NOT NULL DEFAULT 1.00
        COMMENT 'AI top_p 默认值（0.00-1.00；stage model_params 可覆盖）';
