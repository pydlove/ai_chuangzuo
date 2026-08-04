SET NAMES utf8mb4;

-- 文章增加推荐标记
ALTER TABLE t_article
    ADD COLUMN is_recommended TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否推荐 0=否 1=是' AFTER sort,
    ADD INDEX idx_recommended_status_deleted (is_recommended, status, is_deleted);

-- 分类去掉推荐标记
ALTER TABLE t_article_category
    DROP COLUMN is_recommended;
