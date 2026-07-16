-- 方向提示词放宽到 1024 字符（256 不够描述受众+风格+赛道要求）
SET NAMES utf8mb4;

ALTER TABLE u_topic_title
    MODIFY COLUMN direction VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '生成时用的方向提示词（追溯用）';
