CREATE TABLE a_wechat_official_account_config (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键，固定为 1',
    app_id        VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '公众号 AppID',
    app_secret    VARCHAR(255) NOT NULL DEFAULT '' COMMENT '公众号 AppSecret（Jasypt 加密）',
    token         VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '服务器配置 Token',
    plaintext_mode TINYINT      NOT NULL DEFAULT 1 COMMENT '是否明文模式：0-否 1-是',
    enabled       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否启用：0-否 1-是',
    is_deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by    BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人',
    updated_by    BIGINT       NOT NULL DEFAULT 0 COMMENT '更新人'
) COMMENT='微信公众号配置';

INSERT INTO a_wechat_official_account_config (id, app_id, app_secret, token, plaintext_mode, enabled, created_by, updated_by)
VALUES (1, '', '', '', 1, 0, 0, 0);
