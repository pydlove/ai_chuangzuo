-- 管理端-用户管理：u_user 增加会员套餐字段

ALTER TABLE u_user
    ADD COLUMN membership_plan VARCHAR(32) NULL COMMENT '会员套餐：monthly/quarterly/yearly 等，NULL=无套餐' AFTER membership_expire_at;

CREATE INDEX idx_user_membership_plan ON u_user (membership_plan);