SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS u_order (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(32) NOT NULL COMMENT '订单编号：SUB{yyMMdd}{6位随机}',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '下单用户ID',
    plan_key VARCHAR(32) NOT NULL COMMENT '套餐：basic/pro/flagship',
    cycle VARCHAR(16) NOT NULL COMMENT '周期：month/quarter/year',
    amount DECIMAL(19,4) NOT NULL COMMENT '订单金额',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态：0-待支付，1-已支付',
    paid_at DATETIME(3) DEFAULT NULL COMMENT '支付时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_order_order_no (order_no),
    KEY idx_u_order_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户订单表';

CREATE TABLE IF NOT EXISTS u_user_membership (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    level VARCHAR(32) NOT NULL COMMENT '当前等级：basic/pro/flagship',
    started_at DATE NOT NULL COMMENT '本次会员开始日期',
    expires_at DATE NOT NULL COMMENT '会员到期日期',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_user_membership_user_id (user_id),
    KEY idx_u_user_membership_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户会员状态表';
