CREATE TABLE u_user_wechat_bind (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    openid          VARCHAR(64)  NOT NULL COMMENT '微信openid',
    unionid         VARCHAR(64)  DEFAULT NULL COMMENT '微信unionid',
    app_id          VARCHAR(64)  NOT NULL COMMENT '微信公众号appid',
    nickname        VARCHAR(128) DEFAULT NULL COMMENT '微信昵称',
    avatar_url      VARCHAR(512) DEFAULT NULL COMMENT '微信头像',
    bind_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_openid_app (openid, app_id),
    UNIQUE KEY uk_user_app (user_id, app_id)
) COMMENT='用户微信绑定关系表';

CREATE TABLE u_wechat_bind_qr_session (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    scene_str   VARCHAR(64)  NOT NULL COMMENT '二维码场景值',
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0待扫描 1已扫描 2已绑定',
    openid      VARCHAR(64)  DEFAULT NULL COMMENT '扫描用户openid',
    expire_time DATETIME     NOT NULL COMMENT '过期时间',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_scene_str (scene_str),
    KEY idx_user_id (user_id),
    KEY idx_expire_time (expire_time)
) COMMENT='微信绑定二维码会话';
