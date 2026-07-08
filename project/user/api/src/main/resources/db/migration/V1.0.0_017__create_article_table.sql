CREATE TABLE IF NOT EXISTS u_article (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    biz_no VARCHAR(64) NOT NULL
        COMMENT '业务唯一编号，对外暴露',
    user_id BIGINT UNSIGNED NOT NULL
        COMMENT '所属用户ID（u_user.id）',
    title VARCHAR(256) NOT NULL
        COMMENT '作品标题',
    body MEDIUMTEXT NOT NULL
        COMMENT '作品正文',
    style_overrides JSON DEFAULT NULL
        COMMENT '编辑器内联样式覆盖，用于 Preview/Edit 还原',
    platform VARCHAR(32) DEFAULT NULL
        COMMENT '发布平台 key：wechat / xiaohongshu / toutiao / baijiahao / douyin / zhihu / general',
    style VARCHAR(64) DEFAULT NULL
        COMMENT '写作风格名称',
    template VARCHAR(64) DEFAULT NULL
        COMMENT '导出模板名称',
    word_count INT UNSIGNED NOT NULL DEFAULT 0
        COMMENT '正文字数（去空白字符）',
    completed_at DATETIME(3) DEFAULT NULL
        COMMENT '生成完成时间',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0
        COMMENT '逻辑删除标记：0-未删除，1-已删除',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                          ON UPDATE CURRENT_TIMESTAMP(3)
        COMMENT '更新时间',
    UNIQUE KEY uk_u_article_biz_no (biz_no),
    KEY idx_u_article_user_deleted (user_id, is_deleted),
    KEY idx_u_article_completed (user_id, completed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='用户已生成作品表';