CREATE TABLE u_share_config
(
    id          BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键ID',
    scene_key   VARCHAR(32)  NOT NULL COMMENT '分享场景：lottery-抽奖活动，invite-邀请有礼',
    title       VARCHAR(128) NOT NULL DEFAULT '' COMMENT '配置标题（管理端展示用）',
    content     TEXT         NOT NULL COMMENT '分享文案，支持占位符',
    enabled     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    is_deleted  TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by  BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人ID',
    updated_by  BIGINT       NOT NULL DEFAULT 0 COMMENT '更新人ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_scene_key (scene_key)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='分享配置表';

INSERT INTO u_share_config (scene_key, title, content, enabled, sort_order, created_by, updated_by)
VALUES ('lottery', '抽奖活动分享文案',
        '🎁 {title}来啦！我在爱创作发现了超多好礼，会员、创作币、折扣券等你来抽～\n快来一起参与：{url}',
        1, 0, 0, 0);

INSERT INTO u_share_config (scene_key, title, content, enabled, sort_order, created_by, updated_by)
VALUES ('invite', '邀请有礼分享文案',
        '🎁 快来加入爱创作！使用我的邀请码注册，立得 50 创作币，还能享受会员返利～\n立即注册：{url}\n邀请码：{code}',
        1, 0, 0, 0);
