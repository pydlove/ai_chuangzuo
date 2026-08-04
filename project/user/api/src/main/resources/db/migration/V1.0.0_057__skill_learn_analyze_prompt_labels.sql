SET NAMES utf8mb4;

-- =============================================================
-- 2026-08-03: 套餐管理中「学习我的风格」权益文案统一为「提示词」。
-- 与 V1.0.0_055 中 skill_market_publish 的「提示词」语义保持一致。
-- =============================================================

UPDATE u_benefit
SET name = '学习我的提示词',
    description = '每月可使用 AI 提示词学习/分析的次数',
    display_label = '学习我的提示词',
    card_value_tpl = '每月可学习 {value} 次 AI 提示词分析'
WHERE code = 'skill_learn_analyze';
