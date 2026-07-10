-- 12 阶段流水线运行时增强（设计文档 docs/superpowers/specs/2026-07-10-12-stage-pipeline-execution-design.md §4.1）
-- worker 每阶段结束后把进度百分比写回 a_generation_task.progress_pct，user 端轮询可见。

SET NAMES utf8mb4;

ALTER TABLE a_generation_task
    ADD COLUMN progress_pct TINYINT UNSIGNED NOT NULL DEFAULT 0
        COMMENT '任务进度 0-100；worker 每阶段结束后写回；user 端轮询可见';