CREATE TABLE u_wechat_bind_code (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    bind_code   VARCHAR(16)  NOT NULL COMMENT '绑定码',
    openid      VARCHAR(64)  DEFAULT NULL COMMENT '微信openid',
    app_id      VARCHAR(64)  NOT NULL COMMENT '微信公众号appid',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0待绑定 1已激活 2已绑定',
    expire_time DATETIME     NOT NULL COMMENT '过期时间',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_bind_code (bind_code),
    KEY idx_user_id (user_id),
    KEY idx_expire_time (expire_time)
) COMMENT='微信绑定码表';
