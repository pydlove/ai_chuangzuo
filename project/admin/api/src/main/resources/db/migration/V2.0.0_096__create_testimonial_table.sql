-- 首页用户评价表
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS a_testimonial (
    id           BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    avatar_url   VARCHAR(512)     NOT NULL DEFAULT ''     COMMENT '头像 URL',
    name         VARCHAR(64)      NOT NULL                COMMENT '评价人姓名',
    title        VARCHAR(128)     NOT NULL DEFAULT ''     COMMENT '评价人身份/职位',
    star_rating  TINYINT UNSIGNED NOT NULL DEFAULT 5      COMMENT '星级：1-5',
    review_text  VARCHAR(2048)    NOT NULL                COMMENT '评价内容',
    sort         INT              NOT NULL DEFAULT 0      COMMENT '排序权重，小在前',
    is_enabled   TINYINT UNSIGNED NOT NULL DEFAULT 1      COMMENT '是否启用：0-禁用，1-启用',
    is_deleted   TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '是否删除：0-否，1-是',
    created_at   DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at   DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by   BIGINT UNSIGNED  NOT NULL DEFAULT 0      COMMENT '创建人 ID',
    updated_by   BIGINT UNSIGNED  NOT NULL DEFAULT 0      COMMENT '更新人 ID',
    PRIMARY KEY (id),
    INDEX idx_a_testimonial_sort (sort),
    INDEX idx_a_testimonial_is_enabled (is_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页用户评价';
