-- 草稿记录创建模式（引导/熟手），恢复时能精确切回原模式
SET NAMES utf8mb4;

ALTER TABLE u_draft
    ADD COLUMN create_mode VARCHAR(16) NOT NULL DEFAULT 'guided'
        COMMENT '创建模式：guided-引导模式，minimal-熟手模式'
        AFTER template;