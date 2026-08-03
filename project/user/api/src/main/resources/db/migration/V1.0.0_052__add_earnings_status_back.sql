SET NAMES utf8mb4;

-- 提示词市场月度结算依赖收益记录的结算状态，V1.0.0_051 移除后业务代码无法编译。
-- 以新迁移恢复这两个字段，不修改已发布的 V1.0.0_051。
SET @db_name = DATABASE();
SET @table_name = 'u_earnings_record';

-- status
SELECT COUNT(*) INTO @col_status_exists
FROM information_schema.columns
WHERE table_schema = @db_name
  AND table_name = @table_name
  AND column_name = 'status';

SET @sql = IF(@col_status_exists = 0,
    'ALTER TABLE u_earnings_record ADD COLUMN status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT ''结算状态：0-未结算，1-已结算''',
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

SET @sql2 = IF(@col_settled_at_exists = 0,
    'ALTER TABLE u_earnings_record ADD COLUMN settled_at DATETIME(3) DEFAULT NULL COMMENT ''结算时间''',
    'SELECT 1');

PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- 恢复结算状态查询索引
SELECT COUNT(*) INTO @idx_exists
FROM information_schema.statistics
WHERE table_schema = @db_name
  AND table_name = @table_name
  AND index_name = 'idx_user_status_month';

SET @sql3 = IF(@idx_exists = 0,
    'CREATE INDEX idx_user_status_month ON u_earnings_record(user_id, status, settlement_month)',
    'SELECT 1');

PREPARE stmt3 FROM @sql3;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;
