SET NAMES utf8mb4;

-- 选题标题库：管理端 AI 批量生成入库，用户端随机拉取（按用户隔离已用）
CREATE TABLE IF NOT EXISTS u_topic_title (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    title VARCHAR(128) NOT NULL COMMENT '标题',
    summary VARCHAR(512) NOT NULL COMMENT '标题概要（写作方向）',
    direction VARCHAR(256) NOT NULL DEFAULT '' COMMENT '生成时用的方向提示词（追溯用）',
    use_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '全站累计使用次数',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID，0表示系统',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID，0表示系统',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (id),
    KEY idx_u_topic_title_deleted_id (is_deleted, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='选题标题库';

-- 用户使用记录：同一用户对同一标题只记一次（唯一键是 use 幂等的基石）
CREATE TABLE IF NOT EXISTS u_topic_title_usage (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    title_id BIGINT UNSIGNED NOT NULL COMMENT '标题ID，关联 u_topic_title.id',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID，0表示系统',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID，0表示系统',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_topic_usage_user_title (user_id, title_id),
    KEY idx_u_topic_usage_title (title_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户标题使用记录';
