-- 模拟生成文章批次面向存量真实用户，无初始密码
ALTER TABLE a_simulation_robot
    MODIFY COLUMN password_encrypted VARCHAR(255) NULL COMMENT 'AES 加密后的初始密码（模拟生成文章批次为空）';
