SET NAMES utf8mb4;

-- =============================================================
-- 2026-07-30: 导出模板按套餐分层（basic / pro / flagship）。
--   - basic 基础版：仅可访问 8 款基础模板
--   - pro 专业版：基础 + 12 款专业模板（共 20 款）
--   - flagship 旗舰版：全部 30 款，含 10 款旗舰模板
-- =============================================================

-- 1) 加 tier 列（默认 basic，新增模板保持向后兼容）
ALTER TABLE a_export_template
    ADD COLUMN tier VARCHAR(16) NOT NULL DEFAULT 'basic' COMMENT '所需套餐：basic/pro/flagship' AFTER status;

-- 2) 按现有 30 条种子数据的 sort_order 划分 tier
UPDATE a_export_template SET tier = 'pro' WHERE template_key IN (
    'magazine', 'card', 'checklist', 'dark',
    'wechat-minimal', 'wechat-dialogue', 'wechat-brand', 'wechat-infographic',
    'xiaohongshu-list', 'xiaohongshu-review', 'xiaohongshu-tutorial',
    'toutiao-news'
);

UPDATE a_export_template SET tier = 'flagship' WHERE template_key IN (
    'xiaohongshu-emotion',
    'toutiao-depth', 'toutiao-hot', 'toutiao-qa',
    'baijiahao-science', 'baijiahao-history', 'baijiahao-guide',
    'douyin-graphic', 'douyin-quote',
    'zhihu-answer'
);
