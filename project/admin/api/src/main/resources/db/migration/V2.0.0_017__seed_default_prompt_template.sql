-- 阶段 1 落地：内置默认创作模板 default-v1
-- 设计文档：docs/superpowers/specs/2026-07-09-de-ai-flavor-writing-pipeline-design.md §5.10 / 5.16.1
-- 实施 plan：docs/superpowers/plans/2026-07-09-creative-template-stage1-default-seed.md
--
-- 启动后 worker 可立即从 (id=1, enabled=1) 加载，避免抛 PROMPT_TEMPLATE_NO_ENABLED。
-- 12 阶段配置不在此处展开，由 PipelineTemplateResolver 在 DB 未命中时
-- 回落到 PipelineStage enum 默认值兜底（admin 在编辑页点「初始化 12 阶段」后会落库）。
--
-- 幂等：ON DUPLICATE KEY UPDATE，重复执行安全。

SET NAMES utf8mb4;

INSERT INTO t_prompt_template
    (id, name, base_content, enabled, remark, tenant_id, is_deleted, created_by, updated_by)
VALUES
    (1,
     '默认去 AI 味模板',
     '',
     1,
     '标准 12 阶段去 AI 味写作流水线，admin 不要删除，可在基础上复制派生',
     0, 0, 0, 0)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    enabled = 1,
    remark = VALUES(remark),
    updated_at = CURRENT_TIMESTAMP(3);