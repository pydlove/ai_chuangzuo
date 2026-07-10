-- 阶段 2 落地：创作模板状态机 + 版本快照表
-- 设计文档：docs/superpowers/specs/2026-07-09-de-ai-flavor-writing-pipeline-design.md §5.14
-- 实施 plan：docs/superpowers/plans/2026-07-09-creative-template-stage2-lifecycle.md
--
-- 本次升级：
--   1. t_prompt_template 加 template_status（0-草稿 / 1-已发布 / 2-已下线）+ latest_published_version
--   2. 历史 enabled=1 的模板自动映射为「已发布 v1」
--   3. 新建 t_prompt_template_version 表：每次发布把当前 12 阶段配置快照成新版本
--   4. 把已发布模板的 12 阶段现状快照为 v1，方便回溯
--
-- 注意：stage 表里的 12 阶段行可能不存在（阶段 1 靠 PipelineTemplateResolver 兜底）。
--      回填 v1 快照时只对 stage 行已存在的模板执行，否则 snapshot 为空 stages JSON。

SET NAMES utf8mb4;

-- ===== 1. 加状态字段 =====

ALTER TABLE t_prompt_template
    ADD COLUMN template_status TINYINT UNSIGNED NOT NULL DEFAULT 0
        COMMENT '模板状态：0-草稿，1-已发布，2-已下线',
    ADD COLUMN latest_published_version INT UNSIGNED DEFAULT NULL
        COMMENT '当前最新已发布版本号，未发布则为 NULL';

-- ===== 2. 历史数据回填 =====

UPDATE t_prompt_template
SET template_status = 1,
    latest_published_version = 1
WHERE enabled = 1 AND is_deleted = 0;

-- ===== 3. 版本快照表 =====

CREATE TABLE IF NOT EXISTS t_prompt_template_version (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    template_id BIGINT UNSIGNED NOT NULL COMMENT '所属模板ID',
    version INT UNSIGNED NOT NULL COMMENT '版本号，从 1 开始自增',
    version_status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '版本状态：0-草稿，1-已发布，2-已下线',
    config_json JSON NOT NULL COMMENT '12 阶段配置完整快照',
    change_note VARCHAR(512) DEFAULT NULL COMMENT '本次发布变更说明',
    published_at DATETIME(3) DEFAULT NULL COMMENT '发布时间',
    published_by BIGINT UNSIGNED DEFAULT NULL COMMENT '发布人ID',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_t_pt_version (template_id, version),
    KEY idx_t_pt_version_status (version_status),
    KEY idx_t_pt_version_template (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='创作模板版本快照';

-- ===== 4. 把已发布模板的当前 stage 行快照为 v1 =====
-- 仅当 stage 行齐全（=12）才回填；不齐全的留空，靠 PipelineTemplateResolver 在运行时兜底
INSERT INTO t_prompt_template_version
    (template_id, version, version_status, config_json, change_note,
     published_at, published_by, tenant_id, is_deleted, created_by, updated_by)
SELECT
    t.id,
    1,
    1,
    JSON_OBJECT(
        'stages', JSON_ARRAYAGG(
            JSON_OBJECT(
                'index', s.stage_index,
                'stageKey', s.stage_key,
                'stageType', s.stage_type,
                'aiPrompt', s.ai_prompt,
                'ruleConfig', s.rule_config,
                'enabled', s.enabled
            ) ORDER BY s.stage_index ASC
        )
    ),
    'V2.0.0_018 回填：阶段 2 上线时的初始快照',
    COALESCE(t.updated_at, CURRENT_TIMESTAMP(3)),
    0,
    0,
    0,
    0,
    0
FROM t_prompt_template t
JOIN t_prompt_template_stage s ON s.template_id = t.id
WHERE t.enabled = 1 AND t.is_deleted = 0
GROUP BY t.id
HAVING COUNT(s.id) = 12;