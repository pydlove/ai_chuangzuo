-- 12 阶段流水线运行时增强 §4.2：AI 阶段可配参数（temperature / max_tokens / top_p）
-- NULL = 用 GenerationAiService 内置默认值。

SET NAMES utf8mb4;

ALTER TABLE t_prompt_template_stage
    ADD COLUMN model_params JSON DEFAULT NULL
        COMMENT 'AI 阶段可配参数：temperature / max_tokens / top_p 等；NULL=用全局默认';