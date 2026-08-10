-- 修复管理端用户列表会员到期/套餐显示不正确的问题
-- 用户端订阅只写 u_user_membership，未同步 u_user 缓存列，导致管理端查询为空。
-- 这里把当前有效会员信息从 u_user_membership + 最新已支付订单回写到 u_user。

-- 1) 同步有效会员的到期时间和套餐周期（month/quarter/year -> monthly/quarterly/yearly）
UPDATE u_user u
    JOIN u_user_membership m ON u.id = m.user_id
    LEFT JOIN (
        SELECT user_id, cycle
        FROM (
            SELECT user_id,
                   cycle,
                   ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY paid_at DESC, id DESC) AS rn
            FROM u_order
            WHERE status = 1
        ) t
        WHERE rn = 1
    ) latest_order ON m.user_id = latest_order.user_id
SET u.membership_expire_at = CONCAT(m.expires_at, ' 23:59:59'),
    u.membership_plan      = CASE latest_order.cycle
                                WHEN 'month' THEN 'monthly'
                                WHEN 'quarter' THEN 'quarterly'
                                WHEN 'year' THEN 'yearly'
                                ELSE latest_order.cycle
                             END
WHERE m.expires_at >= CURDATE()
  AND (u.membership_expire_at IS NULL OR u.membership_expire_at < CONCAT(m.expires_at, ' 23:59:59'));

-- 2) 清理已过期会员在 u_user 中的残留缓存字段
UPDATE u_user u
SET u.membership_expire_at = NULL,
    u.membership_plan      = NULL
WHERE u.membership_expire_at IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM u_user_membership m
      WHERE m.user_id = u.id AND m.expires_at >= CURDATE()
  );
