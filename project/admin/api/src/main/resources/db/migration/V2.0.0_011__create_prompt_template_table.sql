-- 创作提示词模板表
-- 模板 = base_content + user_style_guidance + system_prompt_json
-- 多模板共存，runtime 仅 1 个 enabled（事务级约束，迁移里不做唯一索引）

CREATE TABLE IF NOT EXISTS t_prompt_template (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    name VARCHAR(64) NOT NULL COMMENT '模板名称（管理后台显示）',
    base_content MEDIUMTEXT COMMENT '① 基础内容（去 AI 味）',
    user_style_guidance MEDIUMTEXT COMMENT '② 用户风格引导（{{name}} 占位符）',
    system_prompt_json MEDIUMTEXT COMMENT '③ 系统提示词（规定 JSON 输出格式）',
    enabled TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否启用（runtime 唯一 1，事务约束）',
    remark VARCHAR(256) DEFAULT NULL COMMENT '备注',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    -- 启用中的模板扫表用
    KEY idx_t_pt_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='创作提示词模板';
