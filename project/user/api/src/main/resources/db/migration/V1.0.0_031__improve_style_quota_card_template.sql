SET NAMES utf8mb4;

-- 把"1 个/月"、"1 次/月"换成更明确的"每月可发布 X 个"、"每月可学习 X 次"。
-- "30 篇/月"、"5 张/月"、"队列最多 1 个任务"等量词清晰的保持不动。
UPDATE u_benefit SET card_value_tpl = '每月可发布 {value} 个' WHERE code = 'style_market_publish';
UPDATE u_benefit SET card_value_tpl = '每月可学习 {value} 次' WHERE code = 'style_learn_analyze';