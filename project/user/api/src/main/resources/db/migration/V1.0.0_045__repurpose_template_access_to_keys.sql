SET NAMES utf8mb4;

-- =============================================================
-- 2026-07-30: template_access 权益从枚举改为逗号分隔的 template_key 列表。
--   - 由管理端「套餐管理 · 文章模板」多选框写入
--   - 用户端 GET /export-templates 根据当前套餐的 template_access 标记 accessible
--   - 定价页面对比表渲染 "导出模板 共 X 个"（按逗号个数）
--
-- 字段长度：原 VARCHAR(128) 不足以放下 30 个模板键的逗号串，扩展为 VARCHAR(2048)。
-- 兼容：旧值（basic_8/all_20/all_custom）不再被识别，会被下方三个 UPDATE 覆盖。
-- =============================================================

ALTER TABLE u_plan_benefit
    MODIFY COLUMN benefit_value VARCHAR(2048) NOT NULL
    COMMENT '权益值：boolean 存 true/false，quota 存数字，tier 存等级标识；template_access 存逗号分隔的 template_key';

-- 1) 清掉 template_access 旧的 value_label_json：选项现在由管理端从 a_export_template 动态加载
UPDATE u_benefit
SET value_label_json = NULL
WHERE code = 'template_access';

-- 2) 写入三档套餐的初始默认模板范围
-- basic 8 款（与原 V2.0.0_059 tier=basic 一致）
UPDATE u_plan_benefit
SET benefit_value = 'wechat,business,marketing,academic,toutiao,xiaohongshu,baijiahao,story'
WHERE benefit_code = 'template_access' AND plan_key = 'basic';

-- pro 20 款（basic + 12 款 pro）
UPDATE u_plan_benefit
SET benefit_value = 'wechat,business,marketing,academic,toutiao,xiaohongshu,baijiahao,story,magazine,card,checklist,dark,wechat-minimal,wechat-dialogue,wechat-brand,wechat-infographic,xiaohongshu-list,xiaohongshu-review,xiaohongshu-tutorial,toutiao-news'
WHERE benefit_code = 'template_access' AND plan_key = 'pro';

-- flagship 30 款（pro + 10 款旗舰）
UPDATE u_plan_benefit
SET benefit_value = 'wechat,business,marketing,academic,toutiao,xiaohongshu,baijiahao,story,magazine,card,checklist,dark,wechat-minimal,wechat-dialogue,wechat-brand,wechat-infographic,xiaohongshu-list,xiaohongshu-review,xiaohongshu-tutorial,toutiao-news,xiaohongshu-emotion,toutiao-depth,toutiao-hot,toutiao-qa,baijiahao-science,baijiahao-history,baijiahao-guide,douyin-graphic,douyin-quote,zhihu-answer'
WHERE benefit_code = 'template_access' AND plan_key = 'flagship';
