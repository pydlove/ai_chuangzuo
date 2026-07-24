SET NAMES utf8mb4;

-- ─────────────────────────────────────────────────────────────
-- 收益流水表扩展：邀请奖励佣金明细
-- ─────────────────────────────────────────────────────────────
ALTER TABLE u_earnings_record
    ADD COLUMN plan_key VARCHAR(32) DEFAULT NULL COMMENT '套餐 key：basic/pro/flagship（邀请奖励适用）' AFTER source_id,
    ADD COLUMN plan_name VARCHAR(64) DEFAULT NULL COMMENT '套餐显示名（邀请奖励适用）' AFTER plan_key,
    ADD COLUMN cycle VARCHAR(16) DEFAULT NULL COMMENT '周期：month/quarter/year（邀请奖励适用）' AFTER plan_name,
    ADD COLUMN order_amount DECIMAL(10,2) DEFAULT NULL COMMENT '被邀请人订单金额（邀请奖励适用）' AFTER cycle,
    ADD COLUMN commission_rate DECIMAL(5,4) DEFAULT NULL COMMENT '返佣比例：0.1000=10%, 0.0500=5%（邀请奖励适用）' AFTER order_amount,
    ADD COLUMN is_first_purchase TINYINT DEFAULT NULL COMMENT '是否首购：0-续费，1-首购（邀请奖励适用）' AFTER commission_rate;
