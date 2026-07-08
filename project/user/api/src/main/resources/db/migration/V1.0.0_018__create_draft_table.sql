CREATE TABLE IF NOT EXISTS u_draft (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    biz_no VARCHAR(64) NOT NULL
        COMMENT '业务唯一编号，对外暴露',
    user_id BIGINT UNSIGNED NOT NULL
        COMMENT '所属用户ID（u_user.id）',
    custom_title VARCHAR(256) DEFAULT NULL
        COMMENT '草稿标题（用户自定义）',
    custom_requirement TEXT DEFAULT NULL
        COMMENT '草稿需求描述（用户自定义）',
    platform VARCHAR(32) DEFAULT NULL
        COMMENT '发布平台 key',
    word_count INT UNSIGNED NOT NULL DEFAULT 0
        COMMENT '目标字数',
    style VARCHAR(64) DEFAULT NULL
        COMMENT '写作风格名称',
    template VARCHAR(64) DEFAULT NULL
        COMMENT '导出模板名称',
    saved_at DATETIME(3) NOT NULL
        COMMENT '草稿保存时间（用户最近一次编辑的时间）',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0
        COMMENT '逻辑删除标记：0-未删除，1-已删除',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                          ON UPDATE CURRENT_TIMESTAMP(3)
        COMMENT '更新时间',
    UNIQUE KEY uk_u_draft_biz_no (biz_no),
    KEY idx_u_draft_user_deleted (user_id, is_deleted),
    KEY idx_u_draft_saved (user_id, saved_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='用户创作草稿表';