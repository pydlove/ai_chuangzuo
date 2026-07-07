SET @db_name = DATABASE();
SET @table_name = 'u_earnings_record';
SET @column_name = 'is_deleted';

SELECT COUNT(*) INTO @col_exists
FROM information_schema.columns
WHERE table_schema = @db_name
  AND table_name = @table_name
  AND column_name = @column_name;

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE u_earnings_record ADD COLUMN is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0-未删除，1-已删除''',
    'SELECT 1');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @idx_exists
FROM information_schema.statistics
WHERE table_schema = @db_name
  AND table_name = @table_name
  AND index_name = 'idx_u_earnings_record_is_deleted';

SET @sql2 = IF(@idx_exists = 0,
    'CREATE INDEX idx_u_earnings_record_is_deleted ON u_earnings_record(is_deleted)',
    'SELECT 1');

PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;
