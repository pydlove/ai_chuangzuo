-- 登录限流配置表，由管理端安全设置菜单维护，用户端登录拦截器读取
CREATE TABLE IF NOT EXISTS a_rate_limit_config (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    is_login_rate_limit_enabled TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否启用登录限流：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID',
    deleted_at DATETIME(3) DEFAULT NULL COMMENT '删除时间，NULL表示未删除',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录限流配置';

-- 初始化单行默认配置：开启登录限流
INSERT INTO a_rate_limit_config (id, is_login_rate_limit_enabled) VALUES (1, 1)
ON DUPLICATE KEY UPDATE is_login_rate_limit_enabled = VALUES(is_login_rate_limit_enabled);
