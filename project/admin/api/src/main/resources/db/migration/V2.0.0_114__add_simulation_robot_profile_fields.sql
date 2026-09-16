-- 模拟运营-机器人资料去重：记录已生成的昵称与头像编号，保证跨批次不重复
ALTER TABLE a_simulation_robot
    ADD COLUMN nickname  VARCHAR(64) DEFAULT NULL COMMENT 'LLM 生成的昵称（机器人内不重复）' AFTER invite_code,
    ADD COLUMN avatar_img INT         DEFAULT NULL COMMENT 'randomuser 头像编号 1-200（机器人内不重复）' AFTER nickname;
