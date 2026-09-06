SET NAMES utf8mb4;

-- 新增权益：运营方案库（专业版及以上可用）
INSERT INTO u_benefit (code, name, type, description, sort_order, status, display_label)
VALUES ('plan_gallery', '运营方案库', 'boolean', '匿名浏览全平台用户的自媒体运营方案', 18, 1, '运营方案库')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    type = VALUES(type),
    description = VALUES(description),
    sort_order = VALUES(sort_order),
    status = VALUES(status),
    display_label = VALUES(display_label);

-- 各套餐默认值：基础版不可用，专业版/旗舰版可用
INSERT INTO u_plan_benefit (plan_key, benefit_code, benefit_value) VALUES
('basic', 'plan_gallery', 'false'),
('pro', 'plan_gallery', 'true'),
('flagship', 'plan_gallery', 'true')
ON DUPLICATE KEY UPDATE benefit_value = VALUES(benefit_value);
