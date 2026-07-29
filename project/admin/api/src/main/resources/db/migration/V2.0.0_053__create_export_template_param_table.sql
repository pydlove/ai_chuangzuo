SET NAMES utf8mb4;

-- 导出模板「参数定义」表：把 visual_style_json 的字段拆成可命名的参数，
-- 让 admin 端能按字段类型（color/number/text/select/border）渲染表单控件，
-- 而不是手编 JSON 字符串。
--
-- 这是 schema 定义表（一次定义，30 个模板共享）；每个模板的具体参数值仍存在
-- a_export_template.visual_style_json 里。default_value 在「新建模板」时填进
-- visual_style_json 当初始值。
--
-- 参数从 V2.0.0_037 seed 30 条 visual_style_json 中出现过 key 全量抽出。

CREATE TABLE IF NOT EXISTS a_export_template_param (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    param_key       VARCHAR(64)      NOT NULL
                        COMMENT '参数 key，对应 visual_style_json 里的字段名（如 titleColor）',
    display_label   VARCHAR(64)      NOT NULL
                        COMMENT 'admin 端显示名（如「标题颜色」）',
    field_type      VARCHAR(16)      NOT NULL
                        COMMENT '控件类型：color/number/text/select/border',
    group_label     VARCHAR(32)      NOT NULL
                        COMMENT '分组：标题/Meta/正文/小标题/高亮块/整体/排版',
    default_value   VARCHAR(255)     NULL
                        COMMENT '新建模板时填入 visual_style_json 的默认值',
    options_json    VARCHAR(1024)    NULL
                        COMMENT 'select 类型的可选项 JSON 数组',
    sort_order      INT              NOT NULL DEFAULT 0,
    created_at      DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_export_template_param_key (param_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导出模板参数定义';

-- 种子：从现有 30 条模板 visual_style_json 抽出的字段全集
-- 分组：标题(title) / Meta(meta) / 正文(body) / 小标题(heading) / 高亮块(callout) / 整体(layout)
INSERT INTO a_export_template_param (param_key, display_label, field_type, group_label, default_value, options_json, sort_order) VALUES
-- 整体 / 排版
('bg',                       '背景色',         'color',  '整体',  '#ffffff',         NULL,                                                       10),
('font',                     '字体栈',         'text',   '整体',  '-apple-system, sans-serif', NULL,                                                       20),
-- 标题
('titleColor',               '标题颜色',       'color',  '标题',  '#1a1a1a',         NULL,                                                       10),
('titleSize',                '标题字号',       'text',   '标题',  '22px',            NULL,                                                       20),
('titleAlign',               '标题对齐',       'select', '标题',  'left',            '["left","center","right"]',                                30),
-- Meta 行（导出/分享时显示的元信息）
('metaColor',                'Meta 文字色',    'color',  'Meta',  '#8c8c8c',         NULL,                                                       10),
('metaBorder',               'Meta 分隔线色',  'color',  'Meta',  '#eeeeee',         NULL,                                                       20),
('metaAlign',                'Meta 对齐',      'select', 'Meta',  'left',            '["left","center","right"]',                                30),
-- 正文
('bodyColor',                '正文颜色',       'color',  '正文',  '#262626',         NULL,                                                       10),
('bodySize',                 '正文字号',       'text',   '正文',  '14px',            NULL,                                                       20),
('bodyLine',                 '正文行高倍数',   'number', '正文',  '1.85',            NULL,                                                       30),
('bodyAlign',                '正文对齐',       'select', '正文',  'left',            '["left","center","right","justify"]',                      40),
-- 小标题
('headingColor',             '小标题颜色',     'color',  '小标题','#1a1a1a',         NULL,                                                       10),
('headingSize',              '小标题字号',     'text',   '小标题','16px',            NULL,                                                       20),
('headingBorder',            '小标题左边线',   'text',   '小标题','none',            NULL,                                                       30),
('headingPl',                '小标题左边距(px)','number', '小标题',0,                 NULL,                                                       40),
('headingBorderBottom',      '小标题下边线',   'text',   '小标题',NULL,              NULL,                                                       50),
('headingAlign',             '小标题对齐',     'select', '小标题','left',            '["left","center","right"]',                                60),
('numbered',                 '小标题序号前缀', 'select', '小标题','0',               '["0","1"]',                                                70),
-- 高亮块（> 引述/要点）
('calloutBg',                '高亮块背景色',   'color',  '高亮块','#f6ffed',         NULL,                                                       10),
('calloutBorder',            '高亮块左边线',   'text',   '高亮块','4px solid #07c160',NULL,                                                       20),
('calloutColor',             '高亮块文字色',   'color',  '高亮块','#262626',         NULL,                                                       30),
('calloutVariant',           '高亮块样式',     'select', '高亮块',NULL,              '["pill","card","cta","checklist"]',                        40);