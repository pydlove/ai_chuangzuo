package com.aichuangzuo.admin.modules.generation.pipeline;

import java.util.List;

/**
 * 13 阶段流水线元信息（与设计文档 2026-07-09-de-ai-flavor-writing-pipeline-design.md 一一对应）。
 *
 * <p>每个 {@code PipelineStage} 持有：
 * <ul>
 *   <li>稳定 key（不随显示文案变化）</li>
 *   <li>类型（AI / 规则 / passthrough）</li>
 *   <li>默认值（AI prompt 文本 或 rule config JSON 字符串）</li>
 *   <li>该阶段 prompt 可用的占位符（仅 AI 阶段）</li>
 *   <li>该规则阶段的表单字段定义（仅规则阶段）</li>
 * </ul>
 *
 * <p>所有 13 个 stage 用 {@link #ALL} 数组持有，UI / API / 后续 executor 都从这一处读。
 */
public enum PipelineStage {

    // ===== 1. 意图锚定（passthrough）=====
    INTENT_ANCHOR(
            1, "intent_anchor", "意图锚定", StageType.PASSTHROUGH,
            "把用户 4 项输入（标题 / 核心观点 / 目标读者 / 风格）组装成下游 prompt 可嵌入的 user_context_block。",
            null,
            null,
            List.of(
                    new Placeholder("title", "用户标题"),
                    new Placeholder("coreViewpoint", "核心观点"),
                    new Placeholder("targetReader", "目标读者"),
                    new Placeholder("userStylePrompt", "用户写作风格")
            ),
            List.of(),
            3
    ),

    // ===== 2. 结构骨架（AI）=====
    OUTLINE(
            2, "outline", "结构骨架", StageType.AI_PROMPT,
            "职责式大纲：每段只写\"职责\"（建立好奇 / 打破常识 / 给新视角），不写主题。",
            """
            你是一位资深编辑。请根据以下文章意图，为这篇文章生成一份职责式大纲。

            [user_context_block]

            任务：
            - 把全文拆成若干段落
            - 每段只写"职责"，不写"主题"或具体内容
            - 职责要明确、不重复、不抽象
            - 风格提示词可间接影响结构选择

            输出格式（JSON）：
            {
              "paragraphs": [
                {"index": 1, "responsibility": "建立好奇：让读者想知道答案"},
                {"index": 2, "responsibility": "打破常识：指出读者原有认知的漏洞"},
                {"index": 3, "responsibility": "给出新视角：提供读者想不到的角度"}
              ]
            }

            约束：
            - 职责不能用"展开论述"这种抽象词
            - 相邻段落职责不能重复
            - 全文围绕核心观点服务，不要偏离""",
            null,
            List.of(
                    new Placeholder("title", "用户标题"),
                    new Placeholder("coreViewpoint", "核心观点"),
                    new Placeholder("targetReader", "目标读者"),
                    new Placeholder("userStylePrompt", "用户写作风格")
            ),
            List.of(),
            8
    ),

    // ===== 3. 素材清单（AI）=====
    MATERIAL_LIST(
            3, "material_list", "素材清单", StageType.AI_PROMPT,
            "诚实素材清单：每段需要的支撑素材，明确标注 已知 / 推断 / 未知 / 待补，不准编造。",
            """
            你是一位事实核查编辑。请根据以下文章意图和结构骨架，列出每段需要的支撑素材，并诚实标注来源可靠性。

            [user_context_block]

            结构骨架（职责式大纲）：
            {{outline}}

            任务：
            - 对每一段，判断它需要什么素材支撑（数据 / 案例 / 引用 / 个人观察 / 类比等）
            - 每项素材必须标注为以下之一：
              - 已知：用户已提供，或 AI 能 100% 确认真实的
              - 推断：AI 推测但不能 100% 确认的
              - 未知：AI 不知道的
              - 待补：需要用户补充的

            约束：
            - 严禁编造数据、案例、人名、时间
            - 不知道的素材必须标"未知"，不能标"已知"或"推断"来蒙混
            - 找不到真实素材支撑的论点，标注"建议降级"（改用不依赖数据的论证方式）

            输出格式（JSON）：
            {
              "materials": [
                {
                  "paragraph_index": 1,
                  "responsibility": "建立好奇",
                  "items": [
                    {
                      "type": "数据",
                      "description": "小红书日活用户数",
                      "reliability": "未知",
                      "source_or_reason": "需要用户补充可靠来源"
                    }
                  ]
                }
              ]
            }""",
            null,
            List.of(
                    new Placeholder("title", "用户标题"),
                    new Placeholder("coreViewpoint", "核心观点"),
                    new Placeholder("targetReader", "目标读者"),
                    new Placeholder("userStylePrompt", "用户写作风格"),
                    new Placeholder("outline", "结构骨架（来自第 2 阶段）")
            ),
            List.of(),
            8
    ),

    // ===== 4. 分块初稿（AI）=====
    DRAFT(
            4, "draft", "分块初稿", StageType.AI_PROMPT,
            "风格主战场：按结构分块写初稿，强制注入用户风格。",
            """
            用以下风格写：{{userStylePrompt}}

            文章意图：
            - 标题：{{title}}
            - 核心观点：{{coreViewpoint}}
            - 目标读者：{{targetReader}}

            结构骨架（按段落职责）：
            {{outline}}

            可用素材（仅限已知项）：
            {{materials}}

            写作要求：
            - 按结构分块写，每块对应一个职责
            - 禁用套话（在当今社会 / 综上所述 / 值得深思 / 首先...其次...最后...）
            - 每个抽象论点配具体例子（仅限已知素材）
            - 句子长短交替
            - 不要主动补全不知道的数据

            输出格式（JSON）：
            {
              "draft": [
                {
                  "paragraph_index": 1,
                  "responsibility": "建立好奇",
                  "content": "第 1 段完整文本"
                }
              ]
            }

            要求：
            - content 是自然段落，不是 bullet list
            - 每段 content 控制在 2-6 句话
            - 段落之间用 JSON 结构分隔，不要在 content 里塞"第 X 段"标记""",
            null,
            List.of(
                    new Placeholder("title", "用户标题"),
                    new Placeholder("coreViewpoint", "核心观点"),
                    new Placeholder("targetReader", "目标读者"),
                    new Placeholder("userStylePrompt", "用户写作风格"),
                    new Placeholder("outline", "结构骨架"),
                    new Placeholder("materials", "素材清单")
            ),
            List.of(),
            20
    ),

    // ===== 5. 韵律检测（规则）=====
    RHYTHM_DETECT(
            5, "rhythm_detect", "韵律检测", StageType.RULE_CONFIG,
            "句长离散度 / 标点换气间隔 / 首词单调性 3 个量化指标扫描。",
            null,
            "{\"uniformLengthDelta\": 5, \"breathMaxChars\": 35, \"monotonousStartCount\": 3}",
            List.of(
                    new Placeholder("draft", "分块初稿 JSON")
            ),
            List.of(
                    ConfigField.builder()
                            .key("uniformLengthDelta").label("句长差异阈值（字）")
                            .type("number").defaultValue(5).min(1).max(20)
                            .description("连续 3 句字数差异在 ±N 字以内 → 判 AI 均匀节律")
                            .build(),
                    ConfigField.builder()
                            .key("breathMaxChars").label("标点换气上限（字）")
                            .type("number").defaultValue(35).min(10).max(100)
                            .description("超过 N 个汉字仍未出现句末标点 → 判长句无气口")
                            .build(),
                    ConfigField.builder()
                            .key("monotonousStartCount").label("首词单调阈值（个）")
                            .type("number").defaultValue(3).min(2).max(5)
                            .description("连续 5 个句子的开头词中 N 个以上词性相同 → 判句首单调")
                            .build()
            ),
            3
    ),

    // ===== 6. 韵律改写（AI）=====
    RHYTHM_REWRITE(
            6, "rhythm_rewrite", "韵律改写", StageType.AI_PROMPT,
            "针对第 5 阶段规则检测出的具体问题，定向改写（句长 / 标点 / 句首词）。",
            """
            你是一位文字编辑。请根据韵律问题清单，改写文章中的问题句子，使其更像真人写作。

            原文（JSON 分块初稿）：
            {{draft}}

            韵律问题清单：
            {{rhythmIssues}}

            改写要求：
            - 只改清单中标记的问题句子/段落，不改其他部分
            - 保持原意不变
            - 保持用户风格不变
            - 句长、标点、句首词调整以解决问题清单为准
            - 用户风格中明确要求的元素（如"多用短句破折号"）必须保留

            输出格式：与第 4 阶段相同的 JSON 结构
            {
              "draft": [
                {"paragraph_index": 1, "responsibility": "...", "content": "..."}
              ]
            }""",
            null,
            List.of(
                    new Placeholder("draft", "分块初稿"),
                    new Placeholder("rhythmIssues", "韵律问题清单")
            ),
            List.of(),
            9
    ),

    // ===== 7. 外部审视（AI）=====
    EXTERNAL_REVIEW(
            7, "external_review", "外部审视", StageType.AI_PROMPT,
            "毒舌同行：找「太正确 / 软弱 / 不敢站队 / 假大空 / 套路感 / 伪金句」等高级 AI 味。",
            """
            你是一位文笔极好、但人品极差的毒舌同行。
            你和作者有私怨，早就看他不顺眼，但你的文学判断力无可挑剔。
            你现在要读他的文章，找出所有让你冷笑的地方——不是找错别字，而是找那些"太正确""太安全""软弱无力不敢下判断"的地方。

            文章：
            {{draft}}

            任务：
            逐段审查，找出以下问题（每个问题必须具体到"第 X 段第 Y 句"）：
            1. "太正确了"：四平八稳、两边不得罪
            2. "软弱无力"：用"可能""某种程度上""也许"逃避判断
            3. "不敢站队"：核心观点被稀释，结尾回到"见仁见智"
            4. "假大空"：用抽象概念代替具体判断
            5. "套路感"：段落像填空，换个人名也能用
            6. "伪金句"：听起来漂亮但没有实质内容

            输出格式（JSON）：
            [
              {
                "paragraph": 2,
                "sentence": 1,
                "type": "太正确了",
                "original": "原句",
                "toxic_comment": "你这个说法放在任何一篇文章里都成立，所以在这篇文章里毫无意义。你到底想说什么？",
                "severity": "高"
              }
            ]

            额外要求：
            - 最后给一个整体毒舌评分（满分 10 分）和一句话总结
            - 评分标准：8 分以上算能看，5-7 分算平庸，5 分以下算废纸
            - 不要手下留情，但也不要为了骂而骂，每条点评必须有具体依据""",
            null,
            List.of(
                    new Placeholder("draft", "韵律改写后的初稿")
            ),
            List.of(),
            8
    ),

    // ===== 8. 定向改写（AI）=====
    TARGETED_REWRITE(
            8, "targeted_rewrite", "定向改写", StageType.AI_PROMPT,
            "根据毒舌同行点评，定向解决「勇气问题」。",
            """
            你是一位资深编辑。请根据毒舌同行的点评，改写文章中的问题句子。

            原文（JSON 分块初稿）：
            {{draft}}

            毒舌点评清单：
            {{toxicComments}}

            改写要求：
            - 只改点评中标记的问题句子，保留其他部分
            - 重点解决：太正确、软弱无力、不敢站队、假大空、套路感、伪金句
            - 改写后要比原句更有立场、更具体、更像真人写的
            - 保持用户风格不变
            - 不要为了"让毒舌满意"而过度修改，改到合理即可

            输出格式：与第 6 阶段相同的 JSON 分块初稿格式
            {
              "draft": [
                {"paragraph_index": 1, "responsibility": "...", "content": "..."}
              ]
            }""",
            null,
            List.of(
                    new Placeholder("draft", "韵律改写后的初稿"),
                    new Placeholder("toxicComments", "毒舌点评清单")
            ),
            List.of(),
            10
    ),

    // ===== 9. 节奏打磨（AI）=====
    RHYTHM_POLISH(
            9, "rhythm_polish", "节奏打磨", StageType.AI_PROMPT,
            "最终「调气」：句子层面打磨，砍信息密度低且不符合用户风格的内容。",
            """
            你是一位文字编辑，负责做最后的节奏打磨。

            原文（JSON 分块初稿）：
            {{draft}}

            风格约束：
            {{userStylePrompt}}

            打磨目标：
            1. 句子长短变化更明显
            2. 砍掉信息密度低的过渡句（如"值得一提的是""不难发现""让我们来看看"）
            3. 开场前 3 句检查：是否有力、是否吸引人继续读
            4. 结尾检查：不要小结式结尾，要给读者一个画面、问题或余味
            5. 段落之间的过渡是否自然

            禁区（绝对不能砍）：
            - 符合用户风格的口癖 / 鲜活表达
            - 不标准但有特色的句式
            - 看似重复但承担强调功能的句子
            - 用户风格提示词中明确要求的元素

            输出格式：与第 8 阶段相同的 JSON 分块初稿格式
            {
              "draft": [
                {"paragraph_index": 1, "responsibility": "...", "content": "..."}
              ]
            }

            要求：
            - 只改需要改的地方，不全篇重写
            - 每段 content 保持自然段落，不是 bullet list
            - 不要砍掉禁区内的内容
            - 改动后给出一个简短的修改摘要""",
            null,
            List.of(
                    new Placeholder("draft", "定向改写后的初稿"),
                    new Placeholder("userStylePrompt", "用户写作风格")
            ),
            List.of(),
            11
    ),

    // ===== 10. 字数统计（规则）=====
    WORD_COUNT(
            10, "word_count", "字数统计", StageType.RULE_CONFIG,
            "统计全文汉字数（不含标点 / 空格），与目标字数比较。",
            null,
            "{\"excludePunctuation\": true, \"excludeSpaces\": true}",
            List.of(
                    new Placeholder("draft", "节奏打磨后的初稿"),
                    new Placeholder("targetWordCount", "用户目标字数")
            ),
            List.of(
                    ConfigField.builder()
                            .key("excludePunctuation").label("统计时排除标点")
                            .type("boolean").defaultValue(true)
                            .description("勾选 = 统计汉字数时不计入标点符号")
                            .build(),
                    ConfigField.builder()
                            .key("excludeSpaces").label("统计时排除空白")
                            .type("boolean").defaultValue(true)
                            .description("勾选 = 统计时不计入空格 / 换行")
                            .build()
            ),
            3
    ),

    // ===== 11. 字数调整（AI）=====
    WORD_ADJUST(
            11, "word_adjust", "字数调整", StageType.AI_PROMPT,
            "质量优先：超过目标字数才删，欠字数不补。",
            """
            你是一位资深编辑。请根据字数统计报告，对文章做最后的字数调整。

            原文（JSON 分块初稿）：
            {{draft}}

            字数统计报告：
            {{wordStats}}

            任务：
            - 如果 actual > target：找出"删掉不损失意思"的句子/段落
            - 如果 actual ≤ target：无需增删，直接返回原文，action 为 keep

            输出格式：
            {
              "action": "cut" | "keep",
              "reason": "整体调整理由",
              "recommendations": [
                {
                  "paragraph_index": 3,
                  "sentence_index": 2,
                  "original": "原句",
                  "action": "delete",
                  "reason": "信息密度低，删掉不损失意思"
                }
              ],
              "estimated_final_count": 820
            }

            原则：
            - 不硬砍到 X 字，而是问"哪些删掉不损失意思"
            - 长一点也没关系，质量优先
            - 字数不足时不硬补，避免灌水
            - 优先删：信息密度低的过渡句、重复论证、空话
            - 绝对不删：核心观点句、关键证据、用户风格口癖""",
            null,
            List.of(
                    new Placeholder("draft", "节奏打磨后的初稿"),
                    new Placeholder("wordStats", "字数统计报告")
            ),
            List.of(),
            8
    ),

    // ===== 12. 导出模板渲染（规则）=====
    EXPORT_RENDER(
            12, "export_render", "导出模板渲染", StageType.RULE_CONFIG,
            "按用户选择的导出模板（平台）渲染成可发布格式；不改字，只改样式。",
            null,
            "{\"templateId\": \"wechat_default\", \"fallbackToPlainText\": true}",
            List.of(
                    new Placeholder("draft", "字数调整后的最终稿"),
                    new Placeholder("title", "文章标题")
            ),
            List.of(
                    ConfigField.builder()
                            .key("templateId").label("导出模板 ID")
                            .type("select").defaultValue("wechat_default")
                            .options(java.util.List.of(
                                    new ConfigField.Option("微信公众号（默认）", "wechat_default"),
                                    new ConfigField.Option("小红书（默认）", "xiaohongshu_default"),
                                    new ConfigField.Option("今日头条（默认）", "toutiao_default"),
                                    new ConfigField.Option("知乎（默认）", "zhihu_default"),
                                    new ConfigField.Option("百家号（默认）", "baijiahao_default"),
                                    new ConfigField.Option("抖音图文（默认）", "douyin_default"),
                                    new ConfigField.Option("通用（默认）", "general_default")
                            ))
                            .description("按平台渲染：字体 / 间距 / 配图位 / 标签等")
                            .build(),
                    ConfigField.builder()
                            .key("fallbackToPlainText").label("渲染失败时回退到纯文本")
                            .type("boolean").defaultValue(true)
                            .description("模板渲染失败时的回退策略")
                            .build()
            ),
            4
    ),

    // ===== 13. 发布描述标签（AI）=====
    PUBLISH_META(
            13, "publish_meta", "发布描述标签", StageType.AI_PROMPT,
            "根据最终稿生成发布描述（摘要）和推荐标签，供发布页直接使用。",
            """
            你是一位资深新媒体编辑。请根据文章标题和最终稿，生成发布描述和推荐标签。

            文章标题：{{title}}

            最终稿（JSON 分块）：
            {{finalDraft}}

            任务：
            - 写一条发布描述：概括文章核心价值，勾起点击欲，80-120 字，口语化，不要"本文介绍了"开头
            - 给 4-6 个推荐标签：简短名词或动宾短语，2-8 个字，贴合文章内容，不要泛词（如"生活""分享"）

            输出格式（JSON）：
            {
              "description": "发布描述文本",
              "tags": ["标签1", "标签2", "标签3", "标签4"]
            }

            约束：
            - 描述必须基于文章实际内容，不夸大、不虚构数据
            - 标签之间不重复、不互相包含
            - 只输出 JSON，不要输出任何解释""",
            null,
            List.of(
                    new Placeholder("title", "文章标题"),
                    new Placeholder("finalDraft", "字数调整后的最终稿")
            ),
            List.of(),
            3
    ),

    // ===== 100. 落库（非 13 阶段之一，用于 GenerationPipeline 编排器排序）=====
    PERSIST_ARTICLE(
            100, "persist_article", "持久化文章", StageType.PASSTHROUGH,
            "pipeline 收尾：把 exportResult 或 finalDraft 写到 article。",
            null,
            null,
            List.of(),
            List.of(),
            2
    );

    /** 所有 13 个 stage + persist 收尾阶段（按 index 升序）。 */
    public static final PipelineStage[] ALL = new PipelineStage[]{
            INTENT_ANCHOR, OUTLINE, MATERIAL_LIST, DRAFT,
            RHYTHM_DETECT, RHYTHM_REWRITE, EXTERNAL_REVIEW,
            TARGETED_REWRITE, RHYTHM_POLISH,
            WORD_COUNT, WORD_ADJUST, EXPORT_RENDER,
            PUBLISH_META,
            PERSIST_ARTICLE
    };

    public final int index;
    public final String key;
    public final String displayName;
    public final StageType type;
    public final String description;
    public final String defaultAiPrompt;          // type=AI_PROMPT 有值
    public final String defaultRuleConfigJson;    // type=RULE_CONFIG 有值
    public final List<Placeholder> placeholders;  // AI 阶段才有；规则阶段仅表示可消费
    public final List<ConfigField> configFields;  // 仅 RULE_CONFIG 阶段有
    public final int weight;                      // 进度权重（合计 100）

    PipelineStage(int index, String key, String displayName, StageType type,
                  String description,
                  String defaultAiPrompt,
                  String defaultRuleConfigJson,
                  List<Placeholder> placeholders) {
        this(index, key, displayName, type, description,
                defaultAiPrompt, defaultRuleConfigJson,
                placeholders, java.util.List.of(), 0);
    }

    PipelineStage(int index, String key, String displayName, StageType type,
                  String description,
                  String defaultAiPrompt,
                  String defaultRuleConfigJson,
                  List<Placeholder> placeholders,
                  List<ConfigField> configFields) {
        this(index, key, displayName, type, description,
                defaultAiPrompt, defaultRuleConfigJson,
                placeholders, configFields, 0);
    }

    PipelineStage(int index, String key, String displayName, StageType type,
                  String description,
                  String defaultAiPrompt,
                  String defaultRuleConfigJson,
                  List<Placeholder> placeholders,
                  List<ConfigField> configFields,
                  int weight) {
        this.index = index;
        this.key = key;
        this.displayName = displayName;
        this.type = type;
        this.description = description;
        this.defaultAiPrompt = defaultAiPrompt;
        this.defaultRuleConfigJson = defaultRuleConfigJson;
        this.placeholders = placeholders;
        this.configFields = configFields;
        this.weight = weight;
    }

    public static PipelineStage byIndex(int index) {
        for (PipelineStage s : ALL) {
            if (s.index == index) return s;
        }
        throw new IllegalArgumentException("unknown stage index: " + index);
    }
}
