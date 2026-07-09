-- 反馈表移除 contact 字段：用户已用邮箱注册，admin 通过 u_user.email 联系，无需重复填写联系方式
ALTER TABLE u_feedback DROP COLUMN contact;