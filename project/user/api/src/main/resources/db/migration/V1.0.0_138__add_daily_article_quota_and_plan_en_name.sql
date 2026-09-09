SET NAMES utf8mb4;

-- 套餐英文显示名（定价页与套餐卡展示）
ALTER TABLE u_plan
    ADD COLUMN display_name_en VARCHAR(64) DEFAULT NULL COMMENT '套餐英文显示名' AFTER display_name;

UPDATE u_plan SET display_name_en = 'Plus' WHERE plan_key = 'basic';
UPDATE u_plan SET display_name_en = 'Max' WHERE plan_key = 'pro';
UPDATE u_plan SET display_name_en = 'Ultra' WHERE plan_key = 'flagship';

-- 权益额度周期：month-自然月 / day-自然日 / lifetime-永久（默认 month，兼容存量权益）
ALTER TABLE u_benefit
    ADD COLUMN period_type VARCHAR(16) NOT NULL DEFAULT 'month' COMMENT '额度周期：month-自然月/day-自然日/lifetime-永久' AFTER type;

-- 让「每日创作上限」紧跟「AI 文章生成」展示（原 sort_order 2~15 顺移一位）
UPDATE u_benefit SET sort_order = sort_order + 1 WHERE sort_order BETWEEN 2 AND 15;

-- 新增权益：每日创作上限
INSERT INTO u_benefit (code, name, type, period_type, description, sort_order, status, display_label, card_value_tpl)
VALUES ('daily_article_quota', '每日创作上限', 'quota', 'day', '每天可创作的文章篇数', 2, 1, '每日创作上限', '每日 {value} 篇')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    type = VALUES(type),
    period_type = VALUES(period_type),
    description = VALUES(description),
    sort_order = VALUES(sort_order),
    status = VALUES(status),
    display_label = VALUES(display_label),
    card_value_tpl = VALUES(card_value_tpl);

-- 各套餐默认值：基础版 5 篇/天，专业版 10 篇/天，旗舰版 20 篇/天
INSERT INTO u_plan_benefit (plan_key, benefit_code, benefit_value) VALUES
('basic', 'daily_article_quota', '5'),
('pro', 'daily_article_quota', '10'),
('flagship', 'daily_article_quota', '20')
ON DUPLICATE KEY UPDATE benefit_value = VALUES(benefit_value);
