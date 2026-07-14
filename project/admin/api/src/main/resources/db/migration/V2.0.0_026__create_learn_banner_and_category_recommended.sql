-- 创作学院 banner 表 + 分类推荐字段
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS t_learn_banner (
    id            BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    image_url     VARCHAR(512)     NOT NULL                COMMENT '图片 URL',
    link_url      VARCHAR(512)     NOT NULL DEFAULT ''     COMMENT '点击跳转链接',
    sort          INT              NOT NULL DEFAULT 0      COMMENT '排序权重，小在前',
    is_deleted    TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '是否删除：0-否，1-是',
    created_at    DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by    BIGINT UNSIGNED  NOT NULL DEFAULT 0,
    updated_by    BIGINT UNSIGNED  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_sort (sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='创作学院 banner';

ALTER TABLE t_article_category
    ADD COLUMN is_recommended TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否推荐 0=否 1=是' AFTER sort;
