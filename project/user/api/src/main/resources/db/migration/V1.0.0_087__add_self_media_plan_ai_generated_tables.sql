SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS u_self_media_plan_question (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '所属用户ID',
    platform_key VARCHAR(64) NOT NULL COMMENT '平台key，如 xiaohongshu',
    prompt_code VARCHAR(128) NOT NULL COMMENT '生成问题时使用的 prompt code',
    question_key VARCHAR(64) NOT NULL COMMENT '问题标识',
    question_text VARCHAR(512) NOT NULL COMMENT '问题文本',
    options_json JSON NOT NULL COMMENT '选项列表 [{"key":"...","label":"..."}]',
    is_required TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否必填：0-否，1-是',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_platform_question (user_id, platform_key, question_key),
    KEY idx_user_platform (user_id, platform_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自媒体方案AI生成问题表';

CREATE TABLE IF NOT EXISTS u_self_media_plan_niche (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '所属用户ID',
    platform_key VARCHAR(64) NOT NULL COMMENT '平台key，如 xiaohongshu',
    answer_snapshot_hash VARCHAR(64) NOT NULL COMMENT '答案快照 SHA256',
    answer_snapshot_json JSON NOT NULL COMMENT '答案快照 JSON',
    niche_key VARCHAR(64) NOT NULL COMMENT '赛道标识',
    name VARCHAR(128) NOT NULL COMMENT '赛道名称',
    audience VARCHAR(128) DEFAULT NULL COMMENT '目标人群',
    monetization VARCHAR(128) DEFAULT NULL COMMENT '变现方式',
    risk_label VARCHAR(32) DEFAULT NULL COMMENT '风险标签',
    risk_color VARCHAR(32) DEFAULT NULL COMMENT '风险颜色',
    case_count INT NOT NULL DEFAULT 0 COMMENT '案例数',
    reason TEXT DEFAULT NULL COMMENT '推荐理由',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_platform_hash_niche (user_id, platform_key, answer_snapshot_hash, niche_key),
    KEY idx_user_platform_hash (user_id, platform_key, answer_snapshot_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自媒体方案AI生成赛道表';

CREATE TABLE IF NOT EXISTS u_self_media_plan_persona (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '所属用户ID',
    platform_key VARCHAR(64) NOT NULL COMMENT '平台key，如 xiaohongshu',
    answer_snapshot_hash VARCHAR(64) NOT NULL COMMENT '答案快照 SHA256',
    niche_key VARCHAR(64) NOT NULL COMMENT '赛道标识',
    persona_key VARCHAR(64) NOT NULL COMMENT '人设标识',
    name VARCHAR(128) NOT NULL COMMENT '人设名称',
    description TEXT DEFAULT NULL COMMENT '人设描述',
    default_pillars_json JSON DEFAULT NULL COMMENT '默认内容支柱 [{"name":"...","percent":60}]',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_platform_hash_niche_persona (user_id, platform_key, answer_snapshot_hash, niche_key, persona_key),
    KEY idx_user_platform_hash_niche (user_id, platform_key, answer_snapshot_hash, niche_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自媒体方案AI生成人设表';

ALTER TABLE u_self_media_plan
    ADD COLUMN answers_json JSON DEFAULT NULL COMMENT '用户对平台问题的答案 [{"questionKey":"...","answer":"..."}]' AFTER recommendation_context_json,
    ADD COLUMN question_prompt_code VARCHAR(128) DEFAULT NULL COMMENT '生成问题时使用的 prompt code' AFTER answers_json;
