-- 取消约稿任务「已流局」状态：把历史 status=3 数据归并为 status=2（已完成）
SET NAMES utf8mb4;

UPDATE u_commission_task
SET status = 2,
    completed_at = COALESCE(completed_at, NOW(3)),
    updated_at = NOW(3)
WHERE status = 3
  AND is_deleted = 0;