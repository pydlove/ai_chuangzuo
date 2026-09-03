ALTER TABLE u_experience_token
    ADD COLUMN used_by_user_name VARCHAR(64) DEFAULT NULL COMMENT '使用人昵称',
    ADD COLUMN used_by_user_email VARCHAR(128) DEFAULT NULL COMMENT '使用人邮箱',
    ADD COLUMN used_by_user_phone VARCHAR(20) DEFAULT NULL COMMENT '使用人手机号';

-- 回溯已消费的令牌，补全使用者信息
UPDATE u_experience_token t
    LEFT JOIN u_user u ON t.used_by_user_id = u.id AND u.is_deleted = 0
SET t.used_by_user_name = u.nickname,
    t.used_by_user_email = u.email,
    t.used_by_user_phone = u.phone
WHERE t.status = 1
  AND t.used_by_user_id IS NOT NULL
  AND (t.used_by_user_name IS NULL OR t.used_by_user_email IS NULL OR t.used_by_user_phone IS NULL);
