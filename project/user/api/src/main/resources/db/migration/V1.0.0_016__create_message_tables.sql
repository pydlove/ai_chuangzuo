CREATE TABLE IF NOT EXISTS u_message (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    msg_type VARCHAR(32) NOT NULL COMMENT '消息类型：announcement-公告 / feature-新功能 / promotion-优惠活动 / generation-生成完成 / membership-会员提醒',
    scope TINYINT UNSIGNED NOT NULL COMMENT '范围：1-广播（全体），2-个人',
    target_user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '目标用户ID：个人消息填写，广播为NULL',
    title VARCHAR(128) NOT NULL COMMENT '标题',
    summary VARCHAR(512) NOT NULL COMMENT '摘要',
    link_url VARCHAR(256) DEFAULT NULL COMMENT '点击跳转路由，空则前端按类型默认跳转',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_message_scope_type (scope, msg_type),
    KEY idx_message_target (target_user_id),
    KEY idx_message_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息内容表';

CREATE TABLE IF NOT EXISTS u_message_read (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    message_id BIGINT UNSIGNED NOT NULL COMMENT '消息ID',
    read_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '已读时间',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_message_read_user_msg (user_id, message_id),
    KEY idx_message_read_message (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户消息已读记录表';

-- 广播消息种子数据（scope=1，target_user_id=NULL），供消息中心 UI 展示
INSERT INTO u_message (msg_type, scope, target_user_id, title, summary, link_url, created_at) VALUES
('announcement', 1, NULL, '系统维护通知', '爱创作将于本月 30 日 22:00-23:00 进行系统维护，届时部分功能暂停使用，敬请谅解。', NULL, DATE_SUB(CURRENT_TIMESTAMP(3), INTERVAL 5 MINUTE)),
('feature', 1, NULL, '新功能上线：标题优化器', '预览页新增 AI 标题优化，一键生成多平台爆款标题，快去试试吧。', NULL, DATE_SUB(CURRENT_TIMESTAMP(3), INTERVAL 10 MINUTE)),
('promotion', 1, NULL, '限时优惠：年会员 7 折', '即日起至月底，年会员低至 199 元，点击了解详情。', '/pricing', DATE_SUB(CURRENT_TIMESTAMP(3), INTERVAL 30 MINUTE));
