SET NAMES utf8mb4;

-- 优化“极速 3 阶段”模板第 4 阶段（分块初稿 / 一次性成稿）的提示词。
-- 主要改进：
-- 1. 去掉与 user_context_block 重复的风格注入；
-- 2. 明确 responsibility 应填写 section 小标题，而不是阶段职责名；
-- 3. 软化第一人称叙事要求，避免与用户自定义风格冲突；
-- 4. 精简 JSON 转义说明，正文少用直双引号；
-- 5. section 数量按目标字数弹性调整。
-- 不修改历史迁移文件，通过新迁移更新现有模板 stage 行。

UPDATE t_prompt_template_stage
SET ai_prompt = '请根据下面的文章意图和风格，直接一次性写出完整的文章初稿。本阶段没有后续润色，因此正文、发布描述和推荐标签都需要在本阶段一次性输出。\n\n[user_context_block]\n\n目标字数：约 {{targetWordCount}} 字（允许 ±10%）。\n\n文章结构：\n- 根据目标字数决定 section 数量：短篇（≤500 字）2 个 section，中篇 3 个，长文最多 4 个。\n- 每个 section 围绕一个观点或视角展开，内部可以有多个自然段落，段落之间有过渡。\n- 只有每个 section 的第一段需要填写 responsibility，值应为该 section 的人话小标题（如“我为什么会开始关注这件事”“那个被忽略的角度”）。\n- section 内部其他段落的 responsibility 必须留空，不要每个段落都写小标题。\n\n写作要求：\n- 直接输出完整正文，不要只写大纲或片段。\n- 每个 section 用具体例子、场景或细节支撑论点，禁用空话、套话。\n- 严格遵循 [user_context_block] 中“风格”一行的全部要求；在不与风格冲突的前提下，可优先采用第一人称叙事，加入个人时间感（如“我当时觉得……”“我后来才意识到……”），让观点从细节里自然浮现。\n- 不要编造无法确认的数据、人名、案例；不知道的素材直接跳过，不要硬凑。\n- 句子长短交替，段落长度有变化，避免连续使用相同句首词。\n- 去掉 AI 味：禁用“在当今社会”“综上所述”“值得深思”“首先…其次…最后”等套路表达。\n- 段落中不要出现段落序号、markdown 标题、bullet list、编号列表、结束标记。\n- 结尾自然收束，不要加“完”“END”等结束标记。\n\n同时生成：\n- 一段 80-120 字的发布描述（description），概括文章核心价值并勾起点击欲，不要以“本文介绍了”开头。\n- 4-6 个推荐标签（tags），2-8 字，简短名词或动宾短语，不重复、不互相包含。\n\n输出格式（严格 JSON，绝对不要 markdown 围栏）：\n{\n  "draft": [\n    {"paragraph_index": 1, "responsibility": "Section 1 小标题", "content": "正文"},\n    {"paragraph_index": 2, "responsibility": "", "content": "同 section 下第二段正文"},\n    {"paragraph_index": 3, "responsibility": "Section 2 小标题", "content": "正文"}\n  ],\n  "description": "80-120 字的发布描述",\n  "tags": ["标签1", "标签2", "标签3", "标签4"]\n}\n\n约束：\n- content 必须是自然段落正文，不是 bullet list、编号列表、markdown 标题或结束标记。\n- 每段 content 控制在 2-8 句话。\n- 正文里尽量少用直双引号 "，可用单引号或书名号代替直接引语，降低 JSON 转义失败概率。\n- responsibility 中也不要出现未转义的双引号。\n- description 必须基于正文实际内容，不夸大、不虚构数据。\n- 只输出合法 JSON，不要任何解释或 markdown 围栏。'
WHERE template_id = 2
  AND stage_index = 4;

UPDATE t_prompt_template
SET updated_at = CURRENT_TIMESTAMP(3)
WHERE id = 2;
