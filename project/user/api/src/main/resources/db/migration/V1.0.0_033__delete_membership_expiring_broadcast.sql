-- 删除演示用的 membership.expiring 广播消息
-- 避免新用户/非会员用户错误收到"会员即将到期"提醒
DELETE FROM u_message
WHERE msg_type = 'membership'
  AND scope = 1
  AND sub_type = 'expiring'
  AND target_user_id IS NULL;
