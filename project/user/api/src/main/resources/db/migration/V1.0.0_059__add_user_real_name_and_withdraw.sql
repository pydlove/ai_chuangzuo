SET NAMES utf8mb4;

-- 为用户表增加实名认证相关字段
ALTER TABLE u_user
    ADD COLUMN real_name VARCHAR(64) DEFAULT NULL COMMENT '真实姓名' AFTER avatar_url,
    ADD COLUMN id_card VARCHAR(18) DEFAULT NULL COMMENT '身份证号' AFTER real_name,
    ADD COLUMN real_name_verified TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否实名认证：0-否，1-是' AFTER id_card;

-- 提现申请表
CREATE TABLE IF NOT EXISTS u_withdraw_request (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    biz_no VARCHAR(64) NOT NULL COMMENT '业务唯一编号',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '申请人用户ID',
    amount DECIMAL(19,4) NOT NULL COMMENT '提现创作币数量',
    account VARCHAR(128) NOT NULL COMMENT '收款账号（支付宝）',
    name VARCHAR(64) NOT NULL COMMENT '收款人真实姓名',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：1-审核中，2-已通过，3-已拒绝',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_withdraw_request_biz_no (biz_no),
    KEY idx_u_withdraw_request_user_status (user_id, status),
    KEY idx_u_withdraw_request_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户提现申请表';
