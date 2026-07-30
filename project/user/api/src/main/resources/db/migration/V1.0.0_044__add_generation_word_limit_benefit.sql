SET NAMES utf8mb4;

-- =============================================================
-- 2026-07-29: 增加「单次生成字数上限」权益，按套餐限制生成字数。
--   - 基础版：500 字
--   - 专业版：1500 字
--   - 旗舰版：3000 字
-- =============================================================

-- 1) 权益定义
INSERT INTO u_benefit (code, name, type, description, sort_order, status, display_label, card_value_tpl, value_label_json)
VALUES ('generation_word_limit', '单次生成字数上限', 'quota', '单次生成任务允许设置的最大字数', 18, 1, '单次生成字数', '最多 {value} 字', NULL)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    type = VALUES(type),
    description = VALUES(description),
    sort_order = VALUES(sort_order),
    status = VALUES(status),
    display_label = VALUES(display_label),
    card_value_tpl = VALUES(card_value_tpl),
    value_label_json = VALUES(value_label_json);

-- 2) 各套餐权益值
INSERT INTO u_plan_benefit (plan_key, benefit_code, benefit_value) VALUES
('basic', 'generation_word_limit', '500'),
('pro', 'generation_word_limit', '1500'),
('flagship', 'generation_word_limit', '3000')
ON DUPLICATE KEY UPDATE benefit_value = VALUES(benefit_value);
