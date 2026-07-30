SET NAMES utf8mb4;

-- =============================================================
-- 2026-07-30: 增加「AI 生成频率限制」权益，按套餐限制每分钟生成次数。
--   - 基础版：3 次/分钟
--   - 专业版：5 次/分钟
--   - 旗舰版：8 次/分钟
-- =============================================================

-- 1) 权益定义
INSERT INTO u_benefit (code, name, type, description, sort_order, status, display_label, card_value_tpl, value_label_json)
VALUES ('generation_rate_limit', 'AI 生成频率限制', 'quota', '每分钟允许提交的 AI 生成任务数', 19, 1, 'AI 生成频率', '{value} 次/分钟', NULL)
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
('basic', 'generation_rate_limit', '3'),
('pro', 'generation_rate_limit', '5'),
('flagship', 'generation_rate_limit', '8')
ON DUPLICATE KEY UPDATE benefit_value = VALUES(benefit_value);
