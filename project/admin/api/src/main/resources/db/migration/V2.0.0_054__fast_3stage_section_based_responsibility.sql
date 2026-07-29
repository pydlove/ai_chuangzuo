SET NAMES utf8mb4;

-- 极速 3 阶段模板 stage 4：调整 responsibility 的使用方式，避免一段一个小标题。
-- 让 AI 按 2-4 个主要 section 组织文章，只有 section 开头段写 responsibility，
-- section 内部的普通段落 responsibility 留空。

UPDATE t_prompt_template_stage
   SET ai_prompt = '用以下风格写：{{userSkillPrompt}}

根据下面的文章意图和风格，直接一次性写出完整的文章初稿，不再等待后续润色。

[user_context_block]

目标字数：约 {{targetWordCount}} 字（允许 ±10%）。

文章结构要求（重要）：
- 全文用 2-4 个主要 section 组织，每个 section 围绕一个观点或视角展开。
- 每个 section 内部可以有多个自然段落，段落之间有过渡，不要生硬切换。
- 只有每个 section 的第一段才需要填写 responsibility（作为该 section 的小标题）。
- section 内部的普通段落 responsibility 必须留空，不要每个段落都写 responsibility，否则会变成满篇小标题。
- 正常文章只有 2-4 个小标题，不要出现十几个小标题。

写作要求：
- 直接输出完整文章，不要只写大纲或片段。
- 每个 section 内部具体例子、场景或细节支撑论点，禁用空话、套话。
- 必须严格遵循上方给出的风格要求。
- 不要编造无法确认的数据、人名、案例；不知道的素材直接跳过，不要硬凑。
- 句子长短交替，避免连续使用相同句首词。
- 去掉 AI 味：禁用“在当今社会”“综上所述”“值得深思”“首先…其次…最后”等套路表达。
- 去 AI 味 / 增加人味：
  - 文章必须有一个具体的人在叙述，而不是一个全知视角在总结。
  - 不要解释主题，要让主题从细节和感受里自然浮现。
  - 多写“我当时觉得……”“我后来才意识到……”这类带个人时间感的句子。
  - 允许叙述者前后矛盾、允许没有结论、允许停在某个情绪或画面里。
  - 段落长度要有变化。
- 同时生成一段 80-120 字的发布描述（description）和 4-6 个推荐标签（tags）。
- description 不要以“本文介绍了”开头，要概括文章核心价值并勾起点击欲。
- tags 为 2-8 字的简短名词或动宾短语，不重复、不互相包含。

输出格式（严格 JSON）：
{
  "draft": [
    {
      "paragraph_index": 1,
      "responsibility": "建立好奇",
      "content": "这里是 section 1 第 1 段的正文。"
    },
    {
      "paragraph_index": 2,
      "responsibility": "",
      "content": "这里是 section 1 第 2 段的正文，同一个 section 下的段落 responsibility 留空。"
    },
    {
      "paragraph_index": 3,
      "responsibility": "给出核心观点",
      "content": "这里是 section 2 第 1 段的正文。"
    }
  ],
  "description": "80-120 字的发布描述",
  "tags": ["标签1", "标签2", "标签3", "标签4"]
}

正文绝对禁止（这些只应出现在 responsibility 元数据里，绝不能出现在 content 中）：
- 段落序号：如 “(1)”“(2)”“1.”“①” 等任何形式的编号。
- 责任标签：如 “建立好奇”“点明身份”“给出方法” 等。
- markdown 标题：如 “##”“###” 或加粗标题行。
- bullet list、编号列表、小标题。
- 结束标记：如 “— 完 —”“- 完 -”“（完）”“END”“The End”“未完待续” 等任何形式的结尾标识。

约束：
- content 必须是自然段落正文，不是 bullet list，也不是标题，更不是结束标记。
- 每段 content 控制在 2-8 句话。
- 全文段落数根据目标字数合理安排。
- section 数量控制在 2-4 个，普通段落 responsibility 必须为空字符串。
- 文章结尾就用最后一段自然收束，不要额外添加 “- 完 -” 之类的标记。
- description 必须基于正文实际内容，不夸大、不虚构数据。
- 只输出 JSON，不要任何解释或 markdown 围栏。',
       updated_at = CURRENT_TIMESTAMP(3)
 WHERE template_id = 2
   AND stage_index = 4;
