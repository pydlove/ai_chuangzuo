-- 创作任务队列增加套餐优先级，worker 拉取时高套餐优先
ALTER TABLE a_generation_task
    ADD COLUMN plan_priority TINYINT NOT NULL DEFAULT 0 COMMENT '任务提交时用户套餐优先级：0-免费/基础版，1-专业版，2-旗舰版';

-- 支撑 worker 按状态+优先级+时间拉取任务
CREATE INDEX idx_a_gt_status_priority_created ON a_generation_task(status, plan_priority, created_at);
