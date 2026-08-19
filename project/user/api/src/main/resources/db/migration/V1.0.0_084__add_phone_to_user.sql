
-- 用户表增加手机号字段，支持邮箱/手机二选一注册与短信验证
ALTER TABLE u_user MODIFY email VARCHAR(128) NULL COMMENT '邮箱';
ALTER TABLE u_user ADD COLUMN phone VARCHAR(32) NULL COMMENT '手机号' AFTER email;
ALTER TABLE u_user ADD COLUMN phone_verified TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '手机是否验证：0-否，1-是' AFTER phone;
ALTER TABLE u_user ADD UNIQUE KEY uk_u_user_phone (phone);
