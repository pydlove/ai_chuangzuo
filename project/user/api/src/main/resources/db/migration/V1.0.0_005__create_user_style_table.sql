SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS u_user_style (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    biz_no VARCHAR(64) NOT NULL COMMENT '业务唯一编号，对外暴露',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '所属用户ID',
    style_name VARCHAR(64) NOT NULL COMMENT '风格名称',
    prompt TEXT NOT NULL COMMENT '风格提示词，写入生成请求',
    scope VARCHAR(256) DEFAULT NULL COMMENT '适用范围标签，逗号分隔',
    source_type TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '来源类型：1-自定义，2-学习',
    use_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '累计使用次数',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_user_style_biz_no (biz_no),
    UNIQUE KEY uk_u_user_style_user_id_name (user_id, style_name),
    KEY idx_u_user_style_user_id_source (user_id, source_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户风格表';
