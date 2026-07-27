SET NAMES utf8mb4;

-- 历史流水允许同一业务引用重复写入；唯一索引建立前保留最早一条，
-- 避免重复的历史入账记录阻塞迁移。用户余额不在此处回算。
DELETE duplicate_record
FROM u_user_coin_record duplicate_record
JOIN u_user_coin_record retained_record
  ON retained_record.biz_type = duplicate_record.biz_type
 AND retained_record.ref_id = duplicate_record.ref_id
 AND retained_record.id < duplicate_record.id
WHERE duplicate_record.ref_id IS NOT NULL
  AND duplicate_record.ref_id <> '';

ALTER TABLE u_user_coin_record
    ADD UNIQUE KEY uk_u_user_coin_record_type_ref (biz_type, ref_id);
