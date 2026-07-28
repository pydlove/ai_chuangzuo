-- 历史归档表增加 article_biz_no，保留任务与 u_article 的关联
ALTER TABLE a_generation_history
    ADD COLUMN article_biz_no VARCHAR(64) DEFAULT NULL COMMENT '完成后关联的 u_article.biz_no' AFTER completed_at;

CREATE INDEX idx_a_gh_article_biz_no ON a_generation_history(article_biz_no);
