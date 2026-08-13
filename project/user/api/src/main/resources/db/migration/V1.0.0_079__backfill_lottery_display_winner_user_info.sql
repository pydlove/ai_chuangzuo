-- 补全中奖展示墙真实用户的昵称和头像
-- 修复上线后已有记录因未写入 nickname 而前端回退显示“幸运用户”的问题
UPDATE u_lottery_display_winner w
    INNER JOIN u_user u ON w.user_id = u.id
SET w.nickname   = u.nickname,
    w.avatar_url = u.avatar_url
WHERE w.user_id IS NOT NULL
  AND w.is_real = 1
  AND (w.nickname IS NULL OR w.nickname = '');
