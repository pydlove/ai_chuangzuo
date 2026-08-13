SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS a_provider_model (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    provider_type VARCHAR(64) NOT NULL COMMENT '厂商类型：kimi / minimax',
    model_code VARCHAR(128) NOT NULL COMMENT '模型编码',
    model_name VARCHAR(128) DEFAULT NULL COMMENT '模型显示名',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_a_provider_model_provider_type_model_code (provider_type, model_code),
    KEY idx_a_provider_model_provider_type (provider_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='厂商可用模型表';
