SET NAMES utf8mb4;

-- 移除「批量导出」套餐权益：管理端套餐管理不再展示，用户端权益页也不再显示。
DELETE FROM u_plan_benefit WHERE benefit_code = 'batch_export';
DELETE FROM u_benefit WHERE code = 'batch_export';
