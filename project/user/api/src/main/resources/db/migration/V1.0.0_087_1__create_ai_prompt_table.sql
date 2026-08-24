SET NAMES utf8mb4;

-- AI 提示词配置表：管理后台统一维护，用户端/管理端运行时读取
-- 用户端也持有建表语句，确保在独立 Flyway schema history 场景下先于种子迁移创建表
CREATE TABLE IF NOT EXISTS c_ai_prompt (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    prompt_code VARCHAR(64) NOT NULL COMMENT '唯一编码',
    prompt_name VARCHAR(128) NOT NULL COMMENT '显示名称',
    module VARCHAR(32) NOT NULL COMMENT '归属端：admin / user',
    category VARCHAR(64) DEFAULT NULL COMMENT '业务分类',
    system_role MEDIUMTEXT COMMENT '系统角色 / AI 身份设定',
    user_prompt MEDIUMTEXT NOT NULL COMMENT '用户提示词主体',
    variable_schema JSON DEFAULT NULL COMMENT '变量元数据：[{name, required, description, example}]',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-启用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    description VARCHAR(500) DEFAULT NULL COMMENT '备注说明',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_c_ai_prompt_code (prompt_code),
    KEY idx_module_category (module, category),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 提示词配置表';
