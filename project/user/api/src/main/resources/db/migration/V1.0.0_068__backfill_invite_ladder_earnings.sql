SET NAMES utf8mb4;

-- 补回已发放但未写入收益账本的邀请阶梯奖励。
-- biz_no 根据创作币流水号确定，迁移修复后重试时可安全忽略重复记录。
INSERT IGNORE INTO u_earnings_record (
    user_id,
    type,
    source_type,
    source_id,
    title,
    description,
    amount,
    settlement_month,
    biz_no,
    status,
    is_deleted,
    created_at,
    updated_at
)
SELECT
    c.user_id,
    'INVITE_REWARD',
    'invite_ladder',
    c.biz_no,
    '邀请阶梯奖励',
    COALESCE(c.remark, '邀请阶梯奖励'),
    c.amount,
    DATE_FORMAT(COALESCE(c.biz_time, c.created_at), '%Y-%m'),
    CONCAT('ER', UPPER(MD5(CONCAT('invite_ladder:', c.biz_no)))),
    0,
    0,
    COALESCE(c.created_at, CURRENT_TIMESTAMP),
    COALESCE(c.created_at, CURRENT_TIMESTAMP)
FROM u_user_coin_record c
WHERE c.biz_type = 'invite_ladder_reward'
  AND c.direction = 1
  AND c.is_deleted = 0;
