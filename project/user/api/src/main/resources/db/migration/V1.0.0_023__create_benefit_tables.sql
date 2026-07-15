SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS u_benefit (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    code VARCHAR(64) NOT NULL COMMENT '权益编码，如 ai_article_quota',
    name VARCHAR(64) NOT NULL COMMENT '权益名称',
    type VARCHAR(16) NOT NULL COMMENT '类型：boolean/quota/tier',
    description VARCHAR(256) DEFAULT NULL COMMENT '权益描述',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-启用',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_benefit_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权益定义表';

CREATE TABLE IF NOT EXISTS u_plan_benefit (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    plan_key VARCHAR(32) NOT NULL COMMENT '套餐：basic/pro/flagship',
    benefit_code VARCHAR(64) NOT NULL COMMENT '权益编码，关联 u_benefit.code',
    benefit_value VARCHAR(128) NOT NULL COMMENT '权益值：boolean 存 true/false，quota 存数字，tier 存等级标识',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_plan_benefit_plan_code (plan_key, benefit_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='套餐权益值表';

CREATE TABLE IF NOT EXISTS u_benefit_usage (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    benefit_code VARCHAR(64) NOT NULL COMMENT '权益编码',
    period VARCHAR(16) NOT NULL COMMENT '周期标识，月度格式 yyyy-MM',
    used_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已用量',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_benefit_usage_user_code_period (user_id, benefit_code, period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权益用量表';

INSERT INTO u_benefit (code, name, type, description, sort_order) VALUES
('ai_article_quota', 'AI 文章生成', 'quota', '每月可生成的文章篇数', 1),
('export_word', '导出 Word', 'boolean', '导出 Word 文档', 2),
('copy_text', '复制正文', 'boolean', '复制文章正文', 3),
('ai_topic', 'AI 选题灵感', 'boolean', 'AI 选题灵感推荐', 4),
('ai_title_optimize', 'AI 标题优化', 'boolean', 'AI 标题优化建议', 5),
('online_edit', '在线编辑', 'boolean', '文章在线编辑器', 6),
('style_custom', '写作风格定制', 'tier', 'none-不可用/preset-预设风格/custom-自定义风格', 7),
('seo_keywords', 'SEO 关键词建议', 'boolean', 'SEO 关键词优化建议', 8),
('template_access', '文章模板', 'tier', 'basic_8-基础 8 款/all_20-全部 20 款/all_custom-全部+自定义', 9),
('sticker_quota', '贴图生成', 'quota', '每月可生成的贴图数量', 10),
('batch_generate', '批量生成/改写', 'boolean', '批量生成与批量改写', 11),
('batch_export', '批量导出', 'boolean', '批量导出文章', 12),
('history_days', '历史记录', 'quota', '历史记录保留天数，-1 表示永久', 13),
('queue_priority', '生成队列优先级', 'tier', 'standard-标准/priority-优先/express-极速', 14),
('queue_max_tasks', '队列任务数', 'quota', '同时在队列中的最大任务数', 15);

INSERT INTO u_plan_benefit (plan_key, benefit_code, benefit_value) VALUES
('basic', 'ai_article_quota', '30'), ('pro', 'ai_article_quota', '100'), ('flagship', 'ai_article_quota', '300'),
('basic', 'export_word', 'true'), ('pro', 'export_word', 'true'), ('flagship', 'export_word', 'true'),
('basic', 'copy_text', 'true'), ('pro', 'copy_text', 'true'), ('flagship', 'copy_text', 'true'),
('basic', 'ai_topic', 'true'), ('pro', 'ai_topic', 'true'), ('flagship', 'ai_topic', 'true'),
('basic', 'ai_title_optimize', 'false'), ('pro', 'ai_title_optimize', 'true'), ('flagship', 'ai_title_optimize', 'true'),
('basic', 'online_edit', 'false'), ('pro', 'online_edit', 'true'), ('flagship', 'online_edit', 'true'),
('basic', 'style_custom', 'none'), ('pro', 'style_custom', 'preset'), ('flagship', 'style_custom', 'custom'),
('basic', 'seo_keywords', 'false'), ('pro', 'seo_keywords', 'false'), ('flagship', 'seo_keywords', 'true'),
('basic', 'template_access', 'basic_8'), ('pro', 'template_access', 'all_20'), ('flagship', 'template_access', 'all_custom'),
('basic', 'sticker_quota', '5'), ('pro', 'sticker_quota', '30'), ('flagship', 'sticker_quota', '100'),
('basic', 'batch_generate', 'false'), ('pro', 'batch_generate', 'false'), ('flagship', 'batch_generate', 'true'),
('basic', 'batch_export', 'false'), ('pro', 'batch_export', 'false'), ('flagship', 'batch_export', 'true'),
('basic', 'history_days', '30'), ('pro', 'history_days', '-1'), ('flagship', 'history_days', '-1'),
('basic', 'queue_priority', 'standard'), ('pro', 'queue_priority', 'priority'), ('flagship', 'queue_priority', 'express'),
('basic', 'queue_max_tasks', '1'), ('pro', 'queue_max_tasks', '5'), ('flagship', 'queue_max_tasks', '10');
