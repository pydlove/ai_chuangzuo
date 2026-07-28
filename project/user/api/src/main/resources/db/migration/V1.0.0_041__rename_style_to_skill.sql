SET NAMES utf8mb4;

-- =============================================================
-- 2026-07-27: 用户端写作预设相关代码 / 数据库统一为 skills / skill。
-- 中文显示文案保持「风格」不变。
-- 1) 重命名风格相关表及字段；
-- 2) 同步权益编码（code），显示文案 (display_label / card_value_tpl) 保持「风格」。
-- =============================================================

-- 1. 重命名 u_user_style -> u_user_skill，并将 style_name 改为 skill_name
ALTER TABLE u_user_style
    RENAME TO u_user_skill,
    CHANGE COLUMN style_name skill_name VARCHAR(64) NOT NULL COMMENT '风格名称；同一用户下唯一',
    RENAME INDEX uk_u_user_style_user_id_name TO uk_u_user_skill_user_id_name;

-- 2. 重命名 u_style_market -> u_skill_market，并将 style_name 改为 skill_name
ALTER TABLE u_style_market
    RENAME TO u_skill_market,
    CHANGE COLUMN style_name skill_name VARCHAR(64) NOT NULL COMMENT '风格名称',
    RENAME INDEX uk_style_market_biz_no TO uk_skill_market_biz_no,
    RENAME INDEX idx_style_market_publisher TO idx_skill_market_publisher,
    RENAME INDEX idx_style_market_enable_deleted TO idx_skill_market_enable_deleted,
    RENAME INDEX idx_style_market_audit TO idx_skill_market_audit,
    RENAME INDEX idx_style_market_name TO idx_skill_market_name;

-- 3. 文章/草稿/约稿任务表中记录「所选风格」的 style 字段改为 skill（style_overrides 是编辑器内联样式，保持不动）
ALTER TABLE u_article
    CHANGE COLUMN style skill VARCHAR(64) DEFAULT NULL COMMENT '写作风格名称';

ALTER TABLE u_draft
    CHANGE COLUMN style skill VARCHAR(64) DEFAULT NULL COMMENT '写作风格名称';

ALTER TABLE u_commission_task
    CHANGE COLUMN style_hint skill_hint VARCHAR(128) DEFAULT NULL COMMENT '期望风格提示';

-- 4. 同步权益编码（code），显示文案保持「风格」
UPDATE u_benefit
SET code = 'skill_custom',
    name = '我的风格数量',
    description = '可同时保存的自定义风格数量上限',
    display_label = '我的风格',
    card_value_tpl = '{value} 个'
WHERE code = 'style_custom';

UPDATE u_benefit
SET code = 'skill_market_publish',
    name = '发布到风格市场',
    description = '每月可发布到风格市场的风格数（单位：个风格）',
    card_value_tpl = '每月可发布 {value} 个风格'
WHERE code = 'style_market_publish';

UPDATE u_benefit
SET code = 'skill_learn_analyze',
    name = '学习我的风格',
    description = '每月可使用 AI 风格学习/分析的次数',
    card_value_tpl = '每月可学习 {value} 次 AI 风格分析'
WHERE code = 'style_learn_analyze';

-- 5. 同步套餐权益关联表中的 benefit_code
UPDATE u_plan_benefit SET benefit_code = 'skill_custom' WHERE benefit_code = 'style_custom';
UPDATE u_plan_benefit SET benefit_code = 'skill_market_publish' WHERE benefit_code = 'style_market_publish';
UPDATE u_plan_benefit SET benefit_code = 'skill_learn_analyze' WHERE benefit_code = 'style_learn_analyze';