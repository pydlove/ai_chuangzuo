SET NAMES utf8mb4;

ALTER TABLE u_user_style
    ADD COLUMN audit_status TINYINT UNSIGNED NOT NULL DEFAULT 1
        COMMENT '审核状态：0-待审核，1-已通过，2-已拒绝'
        AFTER updated_at,
    ADD COLUMN audited_by BIGINT UNSIGNED DEFAULT NULL
        COMMENT '审核管理员ID' AFTER audit_status,
    ADD COLUMN audited_at DATETIME(3) DEFAULT NULL
        COMMENT '审核时间' AFTER audited_by,
    ADD COLUMN reject_reason VARCHAR(256) DEFAULT NULL
        COMMENT '打回原因' AFTER audited_at;

ALTER TABLE u_user_style
    ADD INDEX idx_u_user_style_audit_status (audit_status);