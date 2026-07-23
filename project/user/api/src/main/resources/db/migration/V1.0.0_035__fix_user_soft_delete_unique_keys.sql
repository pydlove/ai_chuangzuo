-- 修复软删除用户重新注册时触发邮箱唯一键冲突的问题。
-- 将 u_user 的 email / invite_code 唯一索引改为“仅对未删除行生效”，
-- 已删除行的 email/invite_code 不再阻塞新注册。
-- 使用函数索引：未删除行生成 (email, 0)，已删除行生成 (email, NULL)，
-- NULL 在唯一索引中不参与等值比较，因此允许已删除行存在重复。

ALTER TABLE u_user
    DROP INDEX uk_u_user_email,
    ADD UNIQUE KEY uk_u_user_email (email, (CASE WHEN is_deleted = 0 THEN 0 ELSE NULL END)),
    DROP INDEX uk_u_user_invite_code,
    ADD UNIQUE KEY uk_u_user_invite_code (invite_code, (CASE WHEN is_deleted = 0 THEN 0 ELSE NULL END));
