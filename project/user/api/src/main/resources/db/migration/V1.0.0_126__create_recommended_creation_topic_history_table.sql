CREATE TABLE IF NOT EXISTS u_recommended_creation_topic_history (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '所属用户ID',
    title VARCHAR(128) NOT NULL COMMENT '已推荐选题标题',
    session_id BIGINT UNSIGNED NOT NULL COMMENT '关联的推荐创作会话ID',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID',
    PRIMARY KEY (id),
    KEY idx_u_recommended_creation_topic_history_user_created (user_id, created_at),
    KEY idx_u_recommended_creation_topic_history_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小爱推荐创作选题历史记录';
