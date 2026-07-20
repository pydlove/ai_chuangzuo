SET NAMES utf8mb4;

-- ─────────────────────────────────────────────────────────────
-- 套餐元数据表：价格、推荐位、邀请奖励、文章数展示
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS u_plan (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    plan_key VARCHAR(32) NOT NULL COMMENT '套餐 key：basic/pro/flagship',
    display_name VARCHAR(32) NOT NULL COMMENT '套餐显示名，如 基础版',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '前台展示顺序',
    recommended TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否推荐套餐：0-否，1-是',
    price_monthly DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '月度原价（无折扣时同步）',
    price_quarter DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '季度价',
    price_year DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '年度价',
    original_monthly DECIMAL(10,2) DEFAULT NULL COMMENT '月度划线价',
    original_quarter DECIMAL(10,2) DEFAULT NULL COMMENT '季度划线价',
    original_year DECIMAL(10,2) DEFAULT NULL COMMENT '年度划线价',
    articles_monthly VARCHAR(32) DEFAULT NULL COMMENT '月度文章数展示文案',
    articles_quarter VARCHAR(32) DEFAULT NULL COMMENT '季度文章数展示文案',
    articles_year VARCHAR(32) DEFAULT NULL COMMENT '年度文章数展示文案',
    savings_year DECIMAL(10,2) DEFAULT NULL COMMENT '年度立省金额',
    inviter_reward DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '邀请奖励（创作币）',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-启用',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_plan_key (plan_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='套餐元数据表';

-- ─────────────────────────────────────────────────────────────
-- u_benefit 扩展字段：对外展示用的标签与渲染模板
-- display_label   对比表行标签 & 默认名称
-- card_value_tpl  价格卡上 quota/tier 的拼接模板，含 {value} 占位
-- value_label_json JSON：value→显示文本覆盖（用于 -1→永久、tier 等）
-- ─────────────────────────────────────────────────────────────
ALTER TABLE u_benefit
    ADD COLUMN display_label VARCHAR(64) DEFAULT NULL COMMENT '对比表行标签/默认名称' AFTER description,
    ADD COLUMN card_value_tpl VARCHAR(64) DEFAULT NULL COMMENT '价格卡值模板，含 {value} 占位' AFTER display_label,
    ADD COLUMN value_label_json VARCHAR(256) DEFAULT NULL COMMENT 'value→显示文本 JSON 覆盖' AFTER card_value_tpl;

-- ─────────────────────────────────────────────────────────────
-- 迁移当前 Pricing.vue 的硬编码数据到 u_plan
-- ─────────────────────────────────────────────────────────────
INSERT INTO u_plan (plan_key, display_name, sort_order, recommended,
    price_monthly, price_quarter, price_year,
    original_monthly, original_quarter, original_year,
    articles_monthly, articles_quarter, articles_year,
    savings_year, inviter_reward) VALUES
('basic', '基础版', 1, 0,
    29.90, 80.70, 251.20,
    NULL, 89.70, 358.80,
    '30 篇 AI 文章/月', '90 篇 AI 文章/季', '360 篇 AI 文章/年',
    107.60, 3.00),
('pro', '专业版', 2, 1,
    59.90, 161.70, 503.20,
    NULL, 179.70, 718.80,
    '100 篇 AI 文章/月', '300 篇 AI 文章/季', '1200 篇 AI 文章/年',
    215.60, 5.00),
('flagship', '旗舰版', 3, 0,
    99.90, 269.70, 839.20,
    NULL, 299.70, 1198.80,
    '300 篇 AI 文章/月', '900 篇 AI 文章/季', '3600 篇 AI 文章/年',
    359.60, 10.00);

-- ─────────────────────────────────────────────────────────────
-- 迁移 u_benefit 展示标签：display_label / card_value_tpl / value_label_json
-- ─────────────────────────────────────────────────────────────
UPDATE u_benefit SET display_label='AI 文章生成', card_value_tpl='{value} 篇/月' WHERE code='ai_article_quota';
UPDATE u_benefit SET display_label='导出 Word' WHERE code='export_word';
UPDATE u_benefit SET display_label='复制正文' WHERE code='copy_text';
UPDATE u_benefit SET display_label='AI 选题灵感' WHERE code='ai_topic';
UPDATE u_benefit SET display_label='AI 标题优化' WHERE code='ai_title_optimize';
UPDATE u_benefit SET display_label='在线编辑' WHERE code='online_edit';
UPDATE u_benefit SET display_label='写作风格定制',
    value_label_json='{"none":"不可用","preset":"3 种预置","custom":"自定义 + 记忆"}' WHERE code='style_custom';
UPDATE u_benefit SET display_label='SEO 关键词建议' WHERE code='seo_keywords';
UPDATE u_benefit SET display_label='文章模板',
    value_label_json='{"basic_8":"8 款基础","all_20":"全部 20+","all_custom":"全部 + 自定义"}' WHERE code='template_access';
UPDATE u_benefit SET display_label='贴图生成', card_value_tpl='{value} 张/月' WHERE code='sticker_quota';
UPDATE u_benefit SET display_label='批量生成/改写' WHERE code='batch_generate';
UPDATE u_benefit SET display_label='批量导出' WHERE code='batch_export';
UPDATE u_benefit SET display_label='历史记录',
    value_label_json='{"30":"30 天","90":"90 天","180":"180 天","365":"365 天","-1":"永久"}' WHERE code='history_days';
UPDATE u_benefit SET display_label='生成队列优先级',
    value_label_json='{"standard":"标准","priority":"优先","express":"极速"}' WHERE code='queue_priority';
UPDATE u_benefit SET display_label='队列任务数', card_value_tpl='队列最多 {value} 个任务' WHERE code='queue_max_tasks';
UPDATE u_benefit SET display_label='发布到风格市场', card_value_tpl='{value} 个/月' WHERE code='style_market_publish';
UPDATE u_benefit SET display_label='学习我的风格', card_value_tpl='{value} 次/月' WHERE code='style_learn_analyze';