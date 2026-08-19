SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS u_self_media_plan (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '所属用户ID',
    platform_key VARCHAR(64) NOT NULL COMMENT '主攻平台key，如 xiaohongshu',
    platform_name VARCHAR(128) NOT NULL COMMENT '主攻平台显示名，如 小红书',
    goal VARCHAR(128) NOT NULL COMMENT '用户选择的核心目标',
    background VARCHAR(128) DEFAULT NULL COMMENT '用户职业/经验领域',
    has_product TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否有可变现产品/服务：0-否，1-是',
    product_desc VARCHAR(512) DEFAULT NULL COMMENT '可变现产品/服务描述',
    niche_key VARCHAR(64) NOT NULL COMMENT '细分赛道key',
    niche_name VARCHAR(128) NOT NULL COMMENT '细分赛道显示名',
    persona_key VARCHAR(64) NOT NULL COMMENT '人设key',
    persona_name VARCHAR(128) NOT NULL COMMENT '人设显示名',
    content_pillars_json JSON NOT NULL COMMENT '内容支柱比例 [{"name":"...","percent":60},...]',
    recommendation_context_json JSON DEFAULT NULL COMMENT 'AI推荐问卷上下文',
    is_recommended_by_ai TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '平台是否由AI推荐：0-否，1-是',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_self_media_plan_user_id (user_id),
    KEY idx_u_self_media_plan_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户自媒体运营方案表';
