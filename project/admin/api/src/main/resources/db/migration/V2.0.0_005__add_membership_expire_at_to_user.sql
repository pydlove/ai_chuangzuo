-- 管理端-到期提醒：u_user 增加会员到期字段
-- 存"到期日次日 00:00"（如 7/10 24:00 结束 → 2026-07-11 00:00:00），NULL=非会员。

ALTER TABLE u_user
    ADD COLUMN membership_expire_at DATETIME NULL COMMENT '会员到期时刻（到期日次日00:00，NULL=非会员）' AFTER user_type;

CREATE INDEX idx_user_membership_expire_at ON u_user (membership_expire_at);