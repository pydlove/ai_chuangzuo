SET NAMES utf8mb4;

-- =============================================================
-- 2026-08-05: 校准「AI 提示词学习」权益定义及各套餐额度。
--
-- 问题现象：专业版用户点击「学习新提示词」提示「当前套餐不支持」。
-- 根因：部分环境 V1.0.0_041（style -> skill 重命名）未执行，
--       u_plan_benefit 中仍保留旧编码 style_learn_analyze，
--       而代码已改用 skill_learn_analyze 查询，导致查不到记录。
--
-- 修复内容：
--   1. 若存在旧编码 style_learn_analyze，先重命名为 skill_learn_analyze；
--   2. 确保 u_benefit 中存在 skill_learn_analyze 权益定义；
--   3. 确保 u_plan_benefit 中 basic=0、pro=1、flagship=2。
-- =============================================================

-- 1. 将旧编码重命名为新编码（兼容 V1.0.0_041 未执行的环境）
UPDATE u_plan_benefit SET benefit_code = 'skill_learn_analyze' WHERE benefit_code = 'style_learn_analyze';
UPDATE u_benefit SET code = 'skill_learn_analyze' WHERE code = 'style_learn_analyze';

-- 2. 确保权益定义存在
INSERT INTO u_benefit (code, name, type, description, sort_order, status, display_label, card_value_tpl)
VALUES ('skill_learn_analyze', '学习我的提示词', 'quota', '每月可使用 AI 提示词学习/分析的次数', 17, 1, '学习我的提示词', '每月可学习 {value} 次 AI 提示词分析')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    type = VALUES(type),
    description = VALUES(description),
    sort_order = VALUES(sort_order),
    status = VALUES(status),
    display_label = VALUES(display_label),
    card_value_tpl = VALUES(card_value_tpl);

-- 3. 确保套餐额度存在
INSERT INTO u_plan_benefit (plan_key, benefit_code, benefit_value) VALUES
('basic', 'skill_learn_analyze', '0'),
('pro', 'skill_learn_analyze', '1'),
('flagship', 'skill_learn_analyze', '2')
ON DUPLICATE KEY UPDATE benefit_value = VALUES(benefit_value);
