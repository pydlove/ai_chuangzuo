-- 修复历史会员到期时间缓存列的存储口径。
-- 旧代码把到期日 23:59:59 存进了 u_user.membership_expire_at，
-- 与管理端“到期日次日 00:00”的约定不一致，导致到期提醒列表显示“剩余 0 天”。
-- 这里把 23:59:59 结尾的记录统一修正为次日 00:00:00。

UPDATE u_user
SET membership_expire_at = DATE_ADD(DATE(membership_expire_at), INTERVAL 1 DAY)
WHERE membership_expire_at IS NOT NULL
  AND TIME(membership_expire_at) >= '23:59:59';
