SET NAMES utf8mb4;

-- 移除收益记录的结算状态概念：用户端不再区分已结算/未结算，统一为实时到账。
SET @db_name = DATABASE();
SET @table_name = 'u_earnings_record';

-- status
SELECT COUNT(*) INTO @col_status_exists
FROM information_schema.columns
WHERE table_schema = @db_name
  AND table_name = @table_name
  AND column_name = 'status';

SET @sql = IF(@col_status_exists = 1,
    'ALTER TABLE u_earnings_record DROP COLUMN status',
    'SELECT 1');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- settled_at
SELECT COUNT(*) INTO @col_settled_at_exists
FROM information_schema.columns
WHERE table_schema = @db_name
  AND table_name = @table_name
  AND column_name = 'settled_at';

SET @sql2 = IF(@col_settled_at_exists = 1,
    'ALTER TABLE u_earnings_record DROP COLUMN settled_at',
    'SELECT 1');

PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- 移除结算状态查询索引
SELECT COUNT(*) INTO @idx_exists
FROM information_schema.statistics
WHERE table_schema = @db_name
  AND table_name = @table_name
  AND index_name = 'idx_user_status_month';

SET @sql3 = IF(@idx_exists = 1,
    'DROP INDEX idx_user_status_month ON u_earnings_record',
    'SELECT 1');

PREPARE stmt3 FROM @sql3;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;

-- 新增按归属月份查询索引
SELECT COUNT(*) INTO @idx_month_exists
FROM information_schema.statistics
WHERE table_schema = @db_name
  AND table_name = @table_name
  AND index_name = 'idx_user_settlement_month';

SET @sql4 = IF(@idx_month_exists = 0,
    'CREATE INDEX idx_user_settlement_month ON u_earnings_record(user_id, settlement_month)',
    'SELECT 1');

PREPARE stmt4 FROM @sql4;
EXECUTE stmt4;
DEALLOCATE PREPARE stmt4;
