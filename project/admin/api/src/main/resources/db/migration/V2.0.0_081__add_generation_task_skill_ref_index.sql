-- 为 a_generation_task 增加 input_param.skillRef 的虚拟列与索引，便于提示词市场统计与使用记录查询。
ALTER TABLE a_generation_task
    ADD COLUMN skill_ref VARCHAR(64) AS (JSON_UNQUOTE(JSON_EXTRACT(input_param, '$.skillRef'))) VIRTUAL NULL
        COMMENT 'input_param.skillRef 虚拟列，用于索引与统计';

CREATE INDEX idx_a_generation_task_skill_ref
    ON a_generation_task(skill_ref);

CREATE INDEX idx_a_generation_task_skill_ref_completed
    ON a_generation_task(skill_ref, completed_at DESC);
