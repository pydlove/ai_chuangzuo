SET NAMES utf8mb4;

-- 极速 3 阶段模板 stage 4：禁止 AI 在文章末尾输出 “- 完 -”、“（完）”、“END” 等结束标记。
-- 只更新 template_id=2 的 stage 4 ai_prompt。

UPDATE t_prompt_template_stage
   SET ai_prompt = '用以下风格写：{{userSkillPrompt}}\n\n根据下面的文章意图和风格，直接一次性写出完整的文章初稿，不再等待后续润色。\n\n[user_context_block]\n\n目标字数：约 {{targetWordCount}} 字（允许 ±10%）。\n\n要求：\n- 直接输出完整文章，不要只写大纲或片段。\n- 文章结构自然，包含开头吸引、中间论证/展开、结尾收束。\n- 每段只承担一个职责，段落之间有过渡。\n- 使用具体例子、场景或细节支撑论点，禁用空话、套话。\n- 必须严格遵循上方给出的风格要求。\n- 不要编造无法确认的数据、人名、案例；不知道的素材直接跳过，不要硬凑。\n- 句子长短交替，避免连续使用相同句首词。\n- 去掉 AI 味：禁用“在当今社会”“综上所述”“值得深思”“首先…其次…最后”等套路表达。\n- 同时生成一段 80-120 字的发布描述（description）和 4-6 个推荐标签（tags）。\n- description 不要以“本文介绍了”开头，要概括文章核心价值并勾起点击欲。\n- tags 为 2-8 字的简短名词或动宾短语，不重复、不互相包含。\n\n输出格式（严格 JSON）：\n{\n  "draft": [\n    {\n      "paragraph_index": 1,\n      "responsibility": "建立好奇",\n      "content": "这里是第 1 段的正文，只写自然叙述文本，不要出现序号、小标题、责任标签或结束标记。"\n    }\n  ],\n  "description": "80-120 字的发布描述，口语化，概括核心价值",\n  "tags": ["标签1", "标签2", "标签3", "标签4"]\n}\n\n正文绝对禁止（这些只应出现在 responsibility 元数据里，绝不能出现在 content 中）：\n- 段落序号：如 “(1)”“(2)”“1.”“①” 等任何形式的编号。\n- 责任标签：如 “建立好奇”“点明身份”“给出方法” 等。\n- markdown 标题：如 “##”“###” 或加粗标题行。\n- bullet list、编号列表、小标题。\n- 结束标记：如 “— 完 —”“- 完 -”“（完）”“END”“The End”“未完待续” 等任何形式的结尾标识。\n\n约束：\n- content 必须是自然段落正文，不是 bullet list，也不是标题，更不是结束标记。\n- 每段 content 控制在 2-8 句话。\n- 全文段落数根据目标字数合理安排。\n- 文章结尾就用最后一段自然收束，不要额外添加 “- 完 -” 之类的标记。\n- description 必须基于正文实际内容，不夸大、不虚构数据。\n- 只输出 JSON，不要任何解释或 markdown 围栏。',
       updated_at = CURRENT_TIMESTAMP(3)
 WHERE template_id = 2
   AND stage_index = 4;
