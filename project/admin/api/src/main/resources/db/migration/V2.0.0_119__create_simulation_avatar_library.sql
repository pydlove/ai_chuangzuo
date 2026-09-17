-- 模拟运营-头像库：管理员上传（服务端压缩为 JPEG），模拟机器人资料阶段随机取用，用后即删
CREATE TABLE a_simulation_avatar (
    id          BIGINT     NOT NULL AUTO_INCREMENT COMMENT '主键',
    avatar      MEDIUMBLOB NOT NULL COMMENT '头像二进制（压缩后的 JPEG）',
    created_at  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '模拟运营-头像库';
