-- 统一约稿任务表风格提示列名：代码中使用 skillHint，数据库列应为 skill_hint
SET NAMES utf8mb4;

-- 仅当旧列名 style_hint 存在时才重命名，避免已修正的数据库报错
SET @rename_sql = (
    SELECT CASE
        WHEN COUNT(*) > 0 THEN 'ALTER TABLE u_commission_task CHANGE COLUMN style_hint skill_hint VARCHAR(128) NULL COMMENT \'风格提示\''
        ELSE 'SELECT 1'
    END
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'u_commission_task'
      AND COLUMN_NAME = 'style_hint'
);

PREPARE stmt FROM @rename_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
