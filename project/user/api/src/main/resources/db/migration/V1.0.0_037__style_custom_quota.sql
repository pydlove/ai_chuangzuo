SET NAMES utf8mb4;

-- =============================================================
-- 2026-07-22: 「我的风格」权益从 tier 改为 quota，所有套餐均可创建，
-- 只是数量上限不同：基础版 1 个 / 专业版 2 个 / 旗舰版 4 个。
-- =============================================================

-- 1) 权益定义：类型改为 quota，描述/模板同步更新
UPDATE u_benefit
SET type = 'quota',
    name = '我的风格数量',
    description = '可同时保存的自定义风格数量上限',
    display_label = '我的风格',
    card_value_tpl = '{value} 个',
    value_label_json = NULL
WHERE code = 'style_custom';

-- 2) 各套餐权益值
INSERT INTO u_plan_benefit (plan_key, benefit_code, benefit_value) VALUES
('basic', 'style_custom', '1'),
('pro', 'style_custom', '2'),
('flagship', 'style_custom', '4')
ON DUPLICATE KEY UPDATE benefit_value = VALUES(benefit_value);
