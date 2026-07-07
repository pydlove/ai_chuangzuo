SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS a_model_config (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    provider_type VARCHAR(64) NOT NULL COMMENT '厂商类型：kimi / minimax',
    base_url VARCHAR(512) NOT NULL COMMENT 'API 基础地址',
    api_key_encrypted VARCHAR(512) NOT NULL COMMENT '加密后的 API Key',
    model_code VARCHAR(128) NOT NULL COMMENT '模型编码',
    model_name VARCHAR(128) DEFAULT NULL COMMENT '模型显示名',
    is_active TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否启用：0-否，1-是（全局唯一）',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_a_model_config_provider_type (provider_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 模型配置表';
