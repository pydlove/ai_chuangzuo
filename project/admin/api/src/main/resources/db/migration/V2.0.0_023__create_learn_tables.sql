-- 创作学院分类表（admin 管理、user 公共读取）
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS t_article_category (
    id          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    parent_id   BIGINT UNSIGNED  NULL,
    name        VARCHAR(64)      NOT NULL,
    sort        INT              NOT NULL DEFAULT 0,
    is_deleted  TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    created_at  DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at  DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by  BIGINT UNSIGNED  NOT NULL DEFAULT 0,
    updated_by  BIGINT UNSIGNED  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_parent_deleted (parent_id, is_deleted),
    INDEX idx_sort (sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='创作学院分类';

-- 创作学院文章表
CREATE TABLE IF NOT EXISTS t_article (
    id            BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    category_id   BIGINT UNSIGNED  NOT NULL,
    title         VARCHAR(128)     NOT NULL,
    summary       VARCHAR(255)     NULL,
    content_type  VARCHAR(16)      NOT NULL COMMENT 'markdown 或 rich_text',
    content       LONGTEXT         NOT NULL,
    status        VARCHAR(16)      NOT NULL COMMENT 'draft 或 published',
    sort          INT              NOT NULL DEFAULT 0,
    author_id     BIGINT UNSIGNED  NULL,
    published_at  DATETIME(3)      NULL,
    is_deleted    TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    created_at    DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by    BIGINT UNSIGNED  NOT NULL DEFAULT 0,
    updated_by    BIGINT UNSIGNED  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_cat_status_deleted (category_id, status, is_deleted),
    INDEX idx_sort (sort),
    INDEX idx_status_deleted (status, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='创作学院文章';
