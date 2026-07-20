SET NAMES utf8mb4;

-- 把"{value} 个" / "{value} 次"后面接不到名词的话术补全：
--   风格市场发布 → 每个是「风格」
--   风格学习    → 每次都是「AI 风格分析」
UPDATE u_benefit SET card_value_tpl = '每月可发布 {value} 个风格' WHERE code = 'style_market_publish';
UPDATE u_benefit SET card_value_tpl = '每月可学习 {value} 次 AI 风格分析' WHERE code = 'style_learn_analyze';

UPDATE u_benefit SET description = '每月可发布到风格市场的风格数（单位：个风格）' WHERE code = 'style_market_publish';
UPDATE u_benefit SET description = '每月可使用 AI 风格学习/分析的次数（单位：次分析）' WHERE code = 'style_learn_analyze';
