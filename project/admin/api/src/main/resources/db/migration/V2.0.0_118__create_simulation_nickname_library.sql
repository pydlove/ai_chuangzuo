-- 模拟运营-昵称库：管理员上传，模拟机器人资料阶段随机取用，用后即删
CREATE TABLE a_simulation_nickname (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    nickname    VARCHAR(64) NOT NULL COMMENT '昵称',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_simulation_nickname (nickname)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '模拟运营-昵称库';
