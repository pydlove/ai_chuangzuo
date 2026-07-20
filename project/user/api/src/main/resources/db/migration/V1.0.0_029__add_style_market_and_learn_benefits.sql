SET NAMES utf8mb4;

-- 新增「发布到风格市场」与「学习我的风格」两项月度额度权益。
-- basic=0, pro=1, flagship=2；basic=0 时 BENEFIT_NOT_SUPPORTED / QUOTA_EXHAUSTED 阻断对应动作。
INSERT INTO u_benefit (code, name, type, description, sort_order) VALUES
('style_market_publish', '发布到风格市场', 'quota', '每月可发布到风格市场的风格数', 16),
('style_learn_analyze', '学习我的风格', 'quota', '每月可使用 AI 风格分析的次数', 17);

INSERT INTO u_plan_benefit (plan_key, benefit_code, benefit_value) VALUES
('basic', 'style_market_publish', '0'),
('pro', 'style_market_publish', '1'),
('flagship', 'style_market_publish', '2'),
('basic', 'style_learn_analyze', '0'),
('pro', 'style_learn_analyze', '1'),
('flagship', 'style_learn_analyze', '2');