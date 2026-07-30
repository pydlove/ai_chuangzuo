SET NAMES utf8mb4;

-- 调整各套餐同时在队列中的最大任务数：基础版 1、专业版 3、旗舰版 5
UPDATE u_plan_benefit
SET benefit_value = '1'
WHERE plan_key = 'basic' AND benefit_code = 'queue_max_tasks';

UPDATE u_plan_benefit
SET benefit_value = '3'
WHERE plan_key = 'pro' AND benefit_code = 'queue_max_tasks';

UPDATE u_plan_benefit
SET benefit_value = '5'
WHERE plan_key = 'flagship' AND benefit_code = 'queue_max_tasks';
