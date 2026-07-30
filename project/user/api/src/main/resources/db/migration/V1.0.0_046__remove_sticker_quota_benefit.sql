-- 生成贴图功能已下线，清理对应权益数据
DELETE FROM u_plan_benefit WHERE benefit_code = 'sticker_quota';
DELETE FROM u_benefit WHERE code = 'sticker_quota';
