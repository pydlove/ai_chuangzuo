-- 去掉公示期，约稿任务简化为三态：0=投递中，1=评选中，2=已完成
-- 旧4态：2=公示中，3=已完成 → 统一为新3态 2=已完成，并回填 completed_at
SET NAMES utf8mb4;

UPDATE u_commission_task
SET status = 2,
    completed_at = COALESCE(completed_at, NOW(3)),
    updated_at = NOW(3)
WHERE status IN (2, 3)
  AND is_deleted = 0;
