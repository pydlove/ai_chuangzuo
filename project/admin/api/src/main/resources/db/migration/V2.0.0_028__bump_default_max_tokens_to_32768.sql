-- 再调大 default_max_tokens 16384 -> 32768
-- 原因：stage 9 (rhythm-polish) 在 16k 时 reasoning + 全文 rewrite 合计超预算，
-- AI 输出被截断在 JSON 字段中间，Jackson 报
-- "Unexpected end-of-input: was expecting closing quote for a string value"。
-- 32k 给 reasoning + 全文 rewrite 留足余量。

-- 修改列默认值（影响后续新建行）
ALTER TABLE a_generation_config
    MODIFY COLUMN default_max_tokens INT UNSIGNED NOT NULL DEFAULT 32768
        COMMENT 'AI max_tokens 默认值（1-128000；MiniMax-M3 推理模型 reasoning 也吃此预算，润色类 stage 至少给到 32k）';

-- 同步更新现有配置行（若仍是 8192 或 16384）
UPDATE a_generation_config
SET default_max_tokens = 32768
WHERE id = 1 AND default_max_tokens IN (8192, 16384);
