-- 作品表加发布描述 + 推荐标签（pipeline 第 13 阶段 AI 生成）
SET NAMES utf8mb4;

ALTER TABLE u_article
    ADD COLUMN description VARCHAR(512) NULL COMMENT '发布描述（AI 生成摘要）' AFTER body,
    ADD COLUMN tags_json   JSON         NULL COMMENT '推荐标签 JSON 数组' AFTER description;
