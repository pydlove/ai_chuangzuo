SET NAMES utf8mb4;

-- 新增权益：运营方案调整次数
INSERT INTO u_benefit (code, name, type, description, sort_order, status, display_label, card_value_tpl)
VALUES ('plan_adjust_quota', '运营方案调整', 'quota', '每月可调整运营方案的次数', 16, 1, '运营方案调整', '{value} 次/月')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    type = VALUES(type),
    description = VALUES(description),
    sort_order = VALUES(sort_order),
    status = VALUES(status),
    display_label = VALUES(display_label),
    card_value_tpl = VALUES(card_value_tpl);

-- 各套餐默认值：基础版 1 次/月，专业版 2 次/月，旗舰版 3 次/月
INSERT INTO u_plan_benefit (plan_key, benefit_code, benefit_value) VALUES
('basic', 'plan_adjust_quota', '1'),
('pro', 'plan_adjust_quota', '2'),
('flagship', 'plan_adjust_quota', '3')
ON DUPLICATE KEY UPDATE benefit_value = VALUES(benefit_value);

-- 新增权益：一文多发建议（专业版及以上可用）
INSERT INTO u_benefit (code, name, type, description, sort_order, status, display_label)
VALUES ('repost_plan', '一文多发建议', 'boolean', '基于已生成文章生成多平台发布计划', 17, 1, '一文多发建议')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    type = VALUES(type),
    description = VALUES(description),
    sort_order = VALUES(sort_order),
    status = VALUES(status),
    display_label = VALUES(display_label);

-- 各套餐默认值：基础版不可用，专业版/旗舰版可用
INSERT INTO u_plan_benefit (plan_key, benefit_code, benefit_value) VALUES
('basic', 'repost_plan', 'false'),
('pro', 'repost_plan', 'true'),
('flagship', 'repost_plan', 'true')
ON DUPLICATE KEY UPDATE benefit_value = VALUES(benefit_value);
