-- 创作学院文章增加封面图 URL 字段
ALTER TABLE t_article
    ADD COLUMN cover_image_url VARCHAR(512) NOT NULL DEFAULT '' COMMENT '封面图 URL' AFTER summary;
