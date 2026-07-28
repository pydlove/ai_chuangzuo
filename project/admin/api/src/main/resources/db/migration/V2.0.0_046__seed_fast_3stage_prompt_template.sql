SET NAMES utf8mb4;

-- 极速 3 阶段提示词模板：只跑意图锚定 → 一次性成稿 → 导出渲染，目标是把生成时间压到 3 分钟以内。
-- 默认以草稿状态插入，admin 在管理端-创作提示词里发布后即可生效。

INSERT INTO t_prompt_template
    (id, name, base_content, template_status, latest_published_version, remark,
     tenant_id, is_deleted, created_by, updated_by)
VALUES
    (2,
     '极速 3 阶段（3 分钟硬指标）',
     '',
     0,
     NULL,
     '仅启用 stage 1（意图锚定）、stage 4（一次性成稿）、stage 12（导出渲染）。其余阶段全部禁用，用于测试 3 分钟生成硬指标。',
     0, 0, 0, 0)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    template_status = 0,
    remark = VALUES(remark),
    updated_at = CURRENT_TIMESTAMP(3);

-- 幂等重入：先清掉旧 stage 再重新写入
DELETE FROM t_prompt_template_stage WHERE template_id = 2;

INSERT INTO t_prompt_template_stage
    (template_id, stage_index, stage_type, stage_key, ai_prompt, rule_config, model_params, enabled,
     tenant_id, is_deleted, created_by, updated_by)
VALUES
    (2, 1, 'passthrough', 'intent_anchor', NULL, NULL, NULL, 1, 0, 0, 0, 0),
    (2, 2, 'ai_prompt', 'outline',
     '（本阶段在“极速 3 阶段”模板中已禁用，如需使用请切换回默认模板或重新编辑提示词。）',
     NULL, NULL, 0, 0, 0, 0, 0),
    (2, 3, 'ai_prompt', 'material_list',
     '（本阶段在“极速 3 阶段”模板中已禁用，如需使用请切换回默认模板或重新编辑提示词。）',
     NULL, NULL, 0, 0, 0, 0, 0),
    (2, 4, 'ai_prompt', 'draft',
     '你是一位熟悉新媒体写作的资深作者。请根据下面的文章意图和风格，直接一次性写出完整的文章初稿，不再等待后续润色。\n\n[user_context_block]\n\n目标字数：约 {{targetWordCount}} 字（允许 ±10%）。\n\n要求：\n- 直接输出完整文章，不要只写大纲或片段。\n- 文章结构自然，包含开头吸引、中间论证/展开、结尾收束。\n- 每段只承担一个职责，段落之间有过渡。\n- 使用具体例子、场景或细节支撑论点，禁用空话、套话。\n- 必须严格遵循上方给出的风格要求。\n- 不要编造无法确认的数据、人名、案例；不知道的素材直接跳过，不要硬凑。\n- 句子长短交替，避免连续使用相同句首词。\n- 去掉 AI 味：禁用“在当今社会”“综上所述”“值得深思”“首先…其次…最后”等套路表达。\n- 同时生成一段 80-120 字的发布描述（description）和 4-6 个推荐标签（tags）。\n- description 不要以“本文介绍了”开头，要概括文章核心价值并勾起点击欲。\n- tags 为 2-8 字的简短名词或动宾短语，不重复、不互相包含。\n\n输出格式（严格 JSON）：\n{\n  "draft": [\n    {\n      "paragraph_index": 1,\n      "responsibility": "建立好奇",\n      "content": "第 1 段完整文本"\n    }\n  ],\n  "description": "80-120 字的发布描述，口语化，概括核心价值",\n  "tags": ["标签1", "标签2", "标签3", "标签4"]\n}\n\n约束：\n- content 是自然段落，不是 bullet list。\n- 每段 content 控制在 2-8 句话。\n- 全文段落数根据目标字数合理安排。\n- description 必须基于正文实际内容，不夸大、不虚构数据。\n- 只输出 JSON，不要任何解释或 markdown 围栏。',
     NULL, '{"max_tokens": 12288, "temperature": 0.7}', 1, 0, 0, 0, 0),
    (2, 5, 'rule_config', 'rhythm_detect', NULL,
     '{"uniformLengthDelta": 5, "breathMaxChars": 35, "monotonousStartCount": 3}',
     NULL, 0, 0, 0, 0, 0),
    (2, 6, 'ai_prompt', 'rhythm_rewrite',
     '（本阶段在“极速 3 阶段”模板中已禁用，如需使用请切换回默认模板或重新编辑提示词。）',
     NULL, NULL, 0, 0, 0, 0, 0),
    (2, 7, 'ai_prompt', 'external_review',
     '（本阶段在“极速 3 阶段”模板中已禁用，如需使用请切换回默认模板或重新编辑提示词。）',
     NULL, NULL, 0, 0, 0, 0, 0),
    (2, 8, 'ai_prompt', 'targeted_rewrite',
     '（本阶段在“极速 3 阶段”模板中已禁用，如需使用请切换回默认模板或重新编辑提示词。）',
     NULL, NULL, 0, 0, 0, 0, 0),
    (2, 9, 'ai_prompt', 'rhythm_polish',
     '（本阶段在“极速 3 阶段”模板中已禁用，如需使用请切换回默认模板或重新编辑提示词。）',
     NULL, NULL, 0, 0, 0, 0, 0),
    (2, 10, 'rule_config', 'word_count', NULL,
     '{"excludePunctuation": true, "excludeSpaces": true}',
     NULL, 0, 0, 0, 0, 0),
    (2, 11, 'ai_prompt', 'word_adjust',
     '（本阶段在“极速 3 阶段”模板中已禁用，如需使用请切换回默认模板或重新编辑提示词。）',
     NULL, NULL, 0, 0, 0, 0, 0),
    (2, 12, 'rule_config', 'export_render', NULL,
     '{"templateId": "wechat_default", "fallbackToPlainText": true}',
     NULL, 1, 0, 0, 0, 0),
    (2, 13, 'ai_prompt', 'publish_meta',
     '（本阶段在“极速 3 阶段”模板中已禁用，如需使用请切换回默认模板或重新编辑提示词。）',
     NULL, NULL, 0, 0, 0, 0, 0);
