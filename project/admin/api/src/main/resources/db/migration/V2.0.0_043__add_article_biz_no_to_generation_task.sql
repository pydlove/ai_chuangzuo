-- 创作任务表增加 article_biz_no，用于管理端关联 u_article 做预览/下载
ALTER TABLE a_generation_task
    ADD COLUMN article_biz_no VARCHAR(64) DEFAULT NULL COMMENT '完成后关联的 u_article.biz_no' AFTER completed_at;

CREATE INDEX idx_a_gt_article_biz_no ON a_generation_task(article_biz_no);
