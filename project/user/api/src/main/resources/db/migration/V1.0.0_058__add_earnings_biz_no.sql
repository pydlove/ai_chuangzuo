SET NAMES utf8mb4;

-- 收益记录新增业务流水号，用于前端账单详情展示；已有记录自动回填。
SET @db_name = DATABASE();
SET @table_name = 'u_earnings_record';

-- 1. 添加 biz_no 列
SELECT COUNT(*) INTO @col_exists
FROM information_schema.columns
WHERE table_schema = @db_name
  AND table_name = @table_name
  AND column_name = 'biz_no';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE u_earnings_record ADD COLUMN biz_no VARCHAR(64) DEFAULT NULL COMMENT ''收益流水号（业务唯一编号）''',
    'SELECT 1');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 回填已有记录（每次调用 UUID() 生成不同值）
UPDATE u_earnings_record
SET biz_no = CONCAT('ER', UPPER(REPLACE(UUID(), '-', '')))
WHERE biz_no IS NULL OR biz_no = '';

-- 3. 添加唯一索引
SELECT COUNT(*) INTO @idx_exists
FROM information_schema.statistics
WHERE table_schema = @db_name
  AND table_name = @table_name
  AND index_name = 'uk_u_earnings_record_biz_no';

SET @sql2 = IF(@idx_exists = 0,
    'CREATE UNIQUE INDEX uk_u_earnings_record_biz_no ON u_earnings_record(biz_no)',
    'SELECT 1');

PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- 4. 改为 NOT NULL，确保后续写入必须生成流水号
SELECT COUNT(*) INTO @col_nullable
FROM information_schema.columns
WHERE table_schema = @db_name
  AND table_name = @table_name
  AND column_name = 'biz_no'
  AND is_nullable = 'YES';

SET @sql3 = IF(@col_nullable = 1,
    'ALTER TABLE u_earnings_record MODIFY COLUMN biz_no VARCHAR(64) NOT NULL COMMENT ''收益流水号（业务唯一编号）''',
    'SELECT 1');

PREPARE stmt3 FROM @sql3;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;
