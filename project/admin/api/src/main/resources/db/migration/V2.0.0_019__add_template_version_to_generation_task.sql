-- 阶段 3 落地：生成任务锁定模板版本号
-- 设计文档：docs/superpowers/specs/2026-07-09-de-ai-flavor-writing-pipeline-design.md §5.15.6 / 5.16.1
-- 实施 plan：docs/superpowers/plans/2026-07-09-creative-template-stage3-user-selection.md
--
-- 老任务（无 version）保持 NULL，runtime 走 fallback（PipelineTemplateResolver
-- 检测到 NULL 时用 enabled=1 的最新版），与阶段 1/2 完全兼容。

SET NAMES utf8mb4;

ALTER TABLE a_generation_task
    ADD COLUMN prompt_template_version INT UNSIGNED DEFAULT NULL
        COMMENT '任务创建时锁定的模板版本号；NULL=fallback 到 enabled=1 的最新版';