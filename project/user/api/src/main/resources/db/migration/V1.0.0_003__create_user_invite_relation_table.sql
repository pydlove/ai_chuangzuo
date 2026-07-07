SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS u_user_invite_relation (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    inviter_id BIGINT UNSIGNED NOT NULL COMMENT '邀请人ID',
    invitee_id BIGINT UNSIGNED NOT NULL COMMENT '被邀请人ID',
    invite_code VARCHAR(16) NOT NULL COMMENT '邀请码',
    source_type TINYINT UNSIGNED NOT NULL DEFAULT 2 COMMENT '来源：1-链接，2-手动填写',
    effective_status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态：0-待验证，1-有效，2-无效',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_user_invite_relation_invitee_id (invitee_id),
    KEY idx_u_user_invite_relation_inviter_id (inviter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邀请关系表';
