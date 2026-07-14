-- 调大 AI max_tokens 默认值 8192 -> 16384
-- 原因：MiniMax-M3 是推理模型，max_tokens 与 reasoning_content 共享预算；
-- 8192 时整稿改写类 stage（如 rhythm-rewrite）会被 reasoning 吃光，
-- 导致 content 为空（finish_reason=length），AI 返回内容解析失败。

-- 修改列默认值（影响后续新建行）
ALTER TABLE a_generation_config
    MODIFY COLUMN default_max_tokens INT UNSIGNED NOT NULL DEFAULT 16384
        COMMENT 'AI max_tokens 默认值（1-128000；MiniMax-M3 等推理模型 reasoning 也吃此预算）';

-- 同步更新现有配置行（若仍是旧的 8192）
UPDATE a_generation_config
SET default_max_tokens = 16384
WHERE id = 1 AND default_max_tokens = 8192;
