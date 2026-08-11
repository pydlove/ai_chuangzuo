CREATE TABLE IF NOT EXISTS u_user_coupon (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    coupon_code VARCHAR(64) NOT NULL COMMENT '券码',
    coupon_type VARCHAR(16) NOT NULL COMMENT '类型：percent/fixed_amount',
    discount_value DECIMAL(10,4) NOT NULL COMMENT '折扣值',
    applicable_cycle VARCHAR(16) NOT NULL DEFAULT 'all' COMMENT '适用周期',
    applicable_plan VARCHAR(16) NOT NULL DEFAULT 'all' COMMENT '适用套餐',
    status VARCHAR(16) NOT NULL DEFAULT 'unused' COMMENT '状态：unused/used/expired',
    valid_start DATETIME(3) NOT NULL COMMENT '有效期开始',
    valid_end DATETIME(3) NOT NULL COMMENT '有效期结束',
    used_order_id BIGINT UNSIGNED DEFAULT NULL COMMENT '使用订单ID',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_coupon_code (coupon_code),
    KEY idx_user_coupon_user_status (user_id, status, valid_end),
    KEY idx_user_coupon_order (used_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户优惠券表';

CREATE TABLE IF NOT EXISTS u_user_membership_pending (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    plan_key VARCHAR(32) NOT NULL COMMENT '套餐：basic/pro/flagship',
    days INT UNSIGNED NOT NULL COMMENT '天数',
    planned_start_at DATE NOT NULL COMMENT '计划生效日期',
    status VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT '状态：pending/activated/expired',
    source_code_id BIGINT UNSIGNED DEFAULT NULL COMMENT '来源兑换码ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    activated_at DATETIME(3) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_user_membership_pending_user_status (user_id, status, planned_start_at),
    KEY idx_user_membership_pending_start (planned_start_at, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='待生效会员表';
