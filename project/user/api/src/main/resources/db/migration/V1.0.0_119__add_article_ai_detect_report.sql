SET NAMES utf8mb4;

-- 为已生成作品增加 AI 检测报告字段，由 pipeline 第 14 阶段写入。
ALTER TABLE u_article
    ADD COLUMN ai_detect_report JSON DEFAULT NULL COMMENT 'AI 检测报告（人工/疑似/AI 三段占比与建议）'
    AFTER optimized_titles_json;
