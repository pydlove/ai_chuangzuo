-- 约稿中心三阶段重构：增加评选截止时间字段，重映射状态值
-- 新状态语义：0=投递中，1=评选中，2=公示中，3=已完成
SET NAMES utf8mb4;

ALTER TABLE u_commission_task
    ADD COLUMN selection_deadline_at DATETIME(3) NULL COMMENT '评选截止时间（NULL=待回填）' AFTER deadline_at;

-- 历史任务回填：selection_deadline_at = deadline_at + 7 天
UPDATE u_commission_task
SET selection_deadline_at = DATE_ADD(deadline_at, INTERVAL 7 DAY),
    updated_at = NOW(3)
WHERE selection_deadline_at IS NULL;

-- 历史已完成任务（status=2 在旧模型表示"已完成"）→ 新模型 status=3
UPDATE u_commission_task
SET status = 3,
    updated_at = NOW(3)
WHERE status = 2
  AND is_deleted = 0;

-- 调整为 NOT NULL（历史已回填，新任务创建时必填）
ALTER TABLE u_commission_task
    MODIFY COLUMN selection_deadline_at DATETIME(3) NOT NULL COMMENT '评选截止时间';