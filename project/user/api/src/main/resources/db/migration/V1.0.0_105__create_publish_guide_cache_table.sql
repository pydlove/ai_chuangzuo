SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS u_self_media_plan_publish_guide (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '所属用户ID',
    plan_id BIGINT UNSIGNED NOT NULL COMMENT '自媒体方案ID',
    plan_updated_at DATETIME(3) NOT NULL COMMENT '生成时方案更新时间',
    article_title VARCHAR(512) NOT NULL COMMENT '文章标题',
    main_platform VARCHAR(128) NOT NULL COMMENT '主发平台',
    main_platform_json JSON NOT NULL COMMENT '主平台发布计划',
    reposts_json JSON NOT NULL COMMENT '一文多发计划列表',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_title_platform (user_id, article_title, main_platform),
    KEY idx_plan_id (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户自媒体发布计划缓存表';
