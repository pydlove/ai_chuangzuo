# 去 AI 味文章生成流水线设计

> 日期：2026-07-09
> 状态：设计稿（讨论沉淀）
> 范围：方法论 + 落地路径，不涉及具体代码改动

---

## 1. 背景与目标

爱创作是 AI 文章生成产品，核心痛点是 **"AI 味"** —— 生成的文章虽然流畅，但读者一眼能识别出是 AI 写的，信任度与传播意愿均下降。

本文档定义一套 12 阶段生成流水线，目标按优先级排序：

1. **可读性**（读者读得舒服）—— 首要
2. **去 AI 味**（不像机器写的）—— 次要
3. **字数控制**（篇幅合适）—— 最后

若三者冲突，按上述优先级取舍。

---

## 2. 核心原则

### 原则 1：质量 > 去 AI 味 > 字数

字数是派生结果，不是目标。先让每个观点说透，最后才审计字数。

### 原则 2：阶段跟随写作过程，而非跟随约束清单

真实写作流程：想清楚 → 列结构 → 写初稿 → 回头读 → 改 → 打磨 → 定稿。

AI 生成应镜像这个过程。约束（字数、AI 味）是质检门，不是流水线节拍。

### 原则 3：诚实优于幻觉

任何阶段如果模型不确定某数据或事实，必须显式标注"未知"，绝不编造。可信度一旦崩塌，节奏越好反而越糟。

### 原则 4：风格是声线，不是化妆

用户的风格提示词必须在写作那一刻注入，最后阶段保护，不能事后"贴皮"。否则得到的是"AI 腔内核披着用户声线的皮"。

### 原则 5：AI 自我审视有上限

AI 只能识别"低级 AI 味"（"综上所述""值得深思"），识别不了"高级 AI 味"（"具有不容忽视的范式意义"）。高级 AI 味必须靠外部视角（另一个 prompt / 真人）。

---

## 3. 完整 12 阶段流程

每个阶段都标注 **AI 调用**：
- **❌ 不访问**：纯程序化（无 LLM 调用，零成本）
- **✅ AI**：调 LLM 完成该阶段
- **🔶 混合**：部分步骤 deterministic（程序化），部分需要 LLM

### 第 1 阶段：意图锚定（组装用户上下文块）

**目的**：把用户提供的 4 项输入，组装成下游阶段可嵌入 prompt 的标准化用户上下文块。不访问 AI，纯程序化组装。

**AI 调用**：❌ 不访问

**为什么需要这个阶段**：
- 用户的 4 项输入是分散字段
- 下游各阶段（2-9）的 prompt 都需要引用这些用户输入
- Stage 1 把它们拼成统一文本块，下游只需在自己的 prompt 里插入 `[user_context_block]`，不必每次重复拼装
- 4 项输入原文不变，可被多个下游阶段复用

**输入（用户必须提供，铁律）**：
- **标题**
- **核心观点**
- **目标读者**
- **用户风格**

**组装函数**（伪代码，定义输出如何产生）：

```python
def assemble_user_context(title, core_viewpoint, target_reader, style_prompt):
    """
    把用户的 4 项输入组装成下游 prompt 可嵌入的标准化上下文块。
    纯字符串拼接，无任何 AI 调用，不添加任何 AI 指令。
    """
    context = {
        "title": title,
        "core_viewpoint": core_viewpoint,
        "target_reader": target_reader,
        "style_prompt": style_prompt,
    }
    user_context_block = (
        f"标题：{title}\n"
        f"核心观点：{core_viewpoint}\n"
        f"目标读者：{target_reader}\n"
        f"风格：{style_prompt}"
    )
    return {
        "context": context,                 # 结构化对象，供下游引用具体字段
        "user_context_block": user_context_block,  # 文本块，供下游 prompt 直接嵌入
    }
```

**输出**（函数返回值）：

```yaml
context:
  title: [用户原文]
  core_viewpoint: [用户原文]
  target_reader: [用户原文]
  style_prompt: [用户原文]

user_context_block: |
  标题：[用户原文]
  核心观点：[用户原文]
  目标读者：[用户原文]
  风格：[用户原文]
```

**重要**：`user_context_block` 里**只有用户提供的原文，不写任何 AI 指令**（如"请生成""请输出"）。指令由下游阶段各自的 prompt 模板负责。

**关键约束**：
- 4 项输入原文不变 —— 不修改、不润色、不"优化"
- 纯字符串拼接 / 模板填充，不做任何 AI 处理
- 风格提示词原文嵌入，不转述、不概括
- 不添加用户没提供的字段（如"阅读场景"）
- 不添加任何"请 AI 返回什么"的指令

**常见陷阱**：
- **越俎代庖**：在组装时偷偷"润色"用户输入
- **偷偷加字段**：组装时添加用户没提供的字段
- **在上下文块里塞指令**：`user_context_block` 变成完整 prompt，混进 AI 指令
- **调用 AI 优化**：违反"不访问 AI"原则

---

### 第 2 阶段：结构骨架（职责式大纲）

**目的**：列大纲，但每段写"职责"而不是"主题"。

**AI 调用**：✅ AI

**输入**：第 1 阶段产出的 `user_context_block`

**输出**：每段的"职责"列表

**主题式 vs 职责式**：
- 主题式：第一段讲 X，第二段讲 Y → 流水账
- 职责式：第一段负责建立好奇，第二段负责打破常识，第三段负责给读者一个想不到的角度 → 段落有节奏

**职责词汇库**（可复用）：
- 建立好奇 / 制造反差 / 打破常识 / 提供新视角
- 抛出问题 / 给出答案 / 提供证据
- 引发共鸣 / 制造紧迫感 / 给出行动指引

**结构骨架 Prompt 模板**：
```
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
    {"index": 3, "responsibility": "给出新视角：提供读者想不到的角度"},
    ...
  ]
}

约束：
- 职责不能用"展开论述"这种抽象词
- 相邻段落职责不能重复
- 全文围绕核心观点服务，不要偏离
```

**关键约束**：
- 段落职责不重复（两段不能都是"举例说明"）
- 段落职责不抽象（不能是"展开论述"）
- 风格提示词**间接影响**结构 —— 讽刺作者和温情作者即使写同一主题，段落职责完全不同

---

### 第 3 阶段：素材清单（解决幻觉）

**目的**：不写初稿，先列"这篇文章需要哪些素材"。

**AI 调用**：✅ AI

**输入**：第 1 阶段的 `user_context_block` + 第 2 阶段的结构骨架

**输出**：诚实清单（已知 / 推断 / 未知 / 待补）

**素材清单 Prompt 模板**：
```
你是一位事实核查编辑。请根据以下文章意图和结构骨架，列出每段需要的支撑素材，并诚实标注来源可靠性。

[user_context_block]

结构骨架（职责式大纲）：
[第 2 阶段输出]

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
}
```

**清单结构**：
```
论点 1：需要数据支撑
  - 已知：[用户提供的、确实存在的]
  - 推断：[AI 推测但不能 100% 确定的，必须标注]
  - 未知：[AI 不确定的]
  - 待补：[需要用户提供的]

论点 2：需要案例支撑
  - 已知：[确定的案例]
  - 推断：[AI 推断的]
  - 待补：[需要用户提供的]
```

**关键约束**：
- AI **不能编造**数据、案例、人名、时间
- 显式区分"已知""推断""未知"
- 不知道的就写"未知"，绝不悄悄编

**降级路径**（某论点缺关键素材时）：
- 改成不依赖数据的论证方式
  - 讲个人观察
  - 做对比（不依赖具体数字）
  - 问问题让读者自己思考
  - 用类比

---

### 第 4 阶段：分块初稿（风格主战场）

**目的**：按结构分块写初稿，强制注入用户风格。

**AI 调用**：✅ AI（最重的一次调用，决定文章主体）

**输入**：第 1、2、3 阶段产出 + 用户风格提示词

**分块初稿 Prompt 模板**：
```
用以下风格写：[用户风格提示词原文]

文章意图：
- 标题：[标题]
- 核心观点：[核心观点]
- 目标读者：[目标读者]

结构骨架（按段落职责）：
1. [第 1 段职责]
2. [第 2 段职责]
3. [第 3 段职责]
...

可用素材（仅限已知项）：
- [论点 1]: [已知素材]
- [论点 2]: [已知素材]
- ...

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
    },
    {
      "paragraph_index": 2,
      "responsibility": "打破常识",
      "content": "第 2 段完整文本"
    }
  ]
}

要求：
- `content` 是自然段落，不是 bullet list
- 每段 content 控制在 2-6 句话
- 段落之间用 JSON 结构分隔，不要在 content 里塞"第 X 段"标记
```

**输出**：JSON 格式的分块初稿，每个段落包含 `paragraph_index`、`responsibility`、`content` 三个字段。

**关键约束**：
- 风格提示词**原文粘贴**，不转述、不概括
- 素材清单只列"已知项"，推断项必须明确标注
- 段落对应职责明确
- 输出必须是合法 JSON，下游阶段需要解析

---

### 第 5 阶段：韵律检测（Deterministic）

**目的**：用量化指标扫描全文，定位"形式层 AI 味"的可疑位置。只做检测，不做改写。

**AI 调用**：❌ 不访问（纯规则代码）

**输入**：第 4 阶段产出的 JSON 分块初稿

**处理逻辑**（伪代码）：

```python
def detect_rhythm_issues(draft_json):
    """
    对 draft_json["draft"] 中每个段落的 content 进行三项韵律指标扫描。
    返回问题清单，不修改原文。
    """
    issues = []
    all_sentences = []  # 按段落聚合的句子列表

    for para in draft_json["draft"]:
        sentences = split_into_sentences(para["content"])
        all_sentences.append({
            "paragraph_index": para["paragraph_index"],
            "sentences": sentences
        })

    # 指标 1：句长离散度
    flat_sentences = flatten(all_sentences)
    for i in range(len(flat_sentences) - 2):
        a, b, c = flat_sentences[i:i+3]
        if abs(len(a["text"]) - len(b["text"])) <= 5 and \
           abs(len(b["text"]) - len(c["text"])) <= 5:
            issues.append({
                "type": "uniform_length",
                "paragraph_index": b["paragraph_index"],
                "sentence_index": b["sentence_index"],
                "text": b["text"],
                "suggestion": "拆成短句或合并成长句"
            })

    # 指标 2：标点换气间隔
    for para in draft_json["draft"]:
        text = para["content"]
        for segment in split_by_sentence_endings(text):
            if char_count(segment) > 35:
                issues.append({
                    "type": "no_breath",
                    "paragraph_index": para["paragraph_index"],
                    "segment": segment,
                    "suggestion": "在中间插入逗号、分号或破折号"
                })

    # 指标 3：首词单调性
    for para in draft_json["draft"]:
        sents = para["content"].split("，。！？")
        for i in range(len(sents) - 4):
            window = sents[i:i+5]
            first_words = [extract_first_word(s) for s in window]
            pos_tags = [get_pos(w) for w in first_words]
            if count_same_pos(pos_tags) >= 3:
                issues.append({
                    "type": "monotonous_start",
                    "paragraph_index": para["paragraph_index"],
                    "sentences": window,
                    "suggestion": "替换 2 个句首为动词或具象名词"
                })

    return issues
```

**三个量化指标**：

| 指标 | 检测规则 |
|------|----------|
| 句长离散度 | 连续 3 句字数差异在 ±5 字以内 → 判"AI 均匀节律" |
| 标点换气间隔 | 超过 35 个汉字仍未出现句末标点 → 判"长句无气口" |
| 首词单调性 | 连续 5 个句子的开头词中，3 个以上词性相同（特别是"这""那""我们""然而"） → 判"句首单调" |

**输出**：韵律问题清单（JSON），每个问题包含 type / paragraph_index / suggestion / 原文片段。

**横切约束：风格提示词作为豁免参考**
- 用户说"多用短句破折号" → 即使检测判定"过于跳跃"，不标记为问题
- 用户说"爱用反问" → 即使检测判定"句首单调"，不标记为问题
- 用户说"少用标点" → 标点间隔指标整体放宽

**关键约束**：
- 只检测、不改写
- 所有规则阈值可配置（5 字、35 字、3 个）
- 输出必须是结构化 JSON，供 Stage 6 消费

---

### 第 6 阶段：韵律改写（AI）

**目的**：根据第 5 阶段的韵律问题清单，对原文做定向改写。

**AI 调用**：✅ AI

**输入**：第 4 阶段产出的 JSON 分块初稿 + 第 5 阶段产出的韵律问题清单

**韵律改写 Prompt 模板**：
```
你是一位文字编辑。请根据韵律问题清单，改写文章中的问题句子，使其更像真人写作。

原文（JSON 分块初稿）：
[第 4 阶段输出]

韵律问题清单：
[第 5 阶段输出]

改写要求：
- 只改清单中标记的问题句子/段落，不改其他部分
- 保持原意不变
- 保持用户风格不变
- 句长、标点、句首词调整以解决问题清单为准
- 用户风格中明确要求的元素（如"多用短句破折号"）必须保留

输出格式：与第 4 阶段相同的 JSON 结构
{
  "draft": [
    {"paragraph_index": 1, "responsibility": "...", "content": "..."},
    ...
  ]
}
```

**输出**：JSON 分块初稿（已修正韵律问题）。

**与 Stage 9（节奏打磨）的区别**：
- **Stage 6 是"治病"**：针对第 5 阶段规则检测出的具体问题，定向改写
- **Stage 9 是"调气"**：不依赖规则，凭阅读感觉做最终润色
- **Stage 6 在外部审视之前**：先把"机器感"修掉，避免毒舌同行被形式问题干扰
- **Stage 9 在定向改写之后**：内容改完后，最后调整体节奏

**关键约束**：
- 局部改写，不全篇重写
- 尊重风格豁免项，不违反用户风格
- 输出格式与第 4 阶段一致，下游可继续解析

---

### 第 7 阶段：外部审视（毒舌同行）

**目的**：用一个**文笔极好、但人品极差的毒舌同行**角色，逼 AI 跳出安全区，找出文章"太正确""太安全""软弱无力不敢下判断"的地方。解决的不是"低级 AI 味"，而是"高级 AI 味"——四平八稳、没有勇气、不敢站队。

**为什么用毒舌同行而不是"挑剔的读者"**：
- 读者视角只能找"读不下去"的问题
- 毒舌同行视角能找"勇气不足"的问题——这是 AI 自我审视发现不了的
- 把身份给"讨厌但不得不承认他厉害的同行"，能逼 AI 给出更尖锐、更不客气的判断
- AI 能识别模式，但很难识别"勇气"；用一个敌意角色，可以把它推到安全区外找茬

**AI 调用**：✅ AI（**只调用一次**，一个角色就够了）

**输入**：第 6 阶段产出的改写后初稿

**输出**：毒舌问题清单（带 severity + 点评）

**外部审视 Prompt 模板**：
```
你是一位文笔极好、但人品极差的毒舌同行。
你和作者有私怨，早就看他不顺眼，但你的文学判断力无可挑剔。
你现在要读他的文章，找出所有让你冷笑的地方——不是找错别字，而是找那些"太正确""太安全""软弱无力不敢下判断"的地方。

文章：
[第 6 阶段输出]

任务：
逐段审查，找出以下问题（每个问题必须具体到"第 X 段第 Y 句"）：

1. **"太正确了"**：
   - 四平八稳、两边不得罪
   - 说了一堆但等于没说
   - 放在任何文章里都成立，所以在这篇文章里没意义

2. **"软弱无力"**：
   - 明明该下判断的地方用了"可能""某种程度上""也许""不无道理"来逃避
   - 用疑问句代替陈述句来躲闪
   - 该斩钉截铁的时候绕弯子

3. **"不敢站队"**：
   - 核心观点被稀释
   - 结尾回到"见仁见智""每个人都有自己的看法"
   - 作者原本的判断被包装成"客观陈述"

4. **"假大空"**：
   - 用抽象概念代替具体判断
   - 用"本质上说""从根本上""深层来看"等空话填充
   - 没有具体到人、事、场景

5. **"套路感"**：
   - 段落像填空，换个人名也能用
   - 结构工整到不自然
   - 每段长度、句式过于均匀

6. **"伪金句"**：
   - 听起来漂亮但没有实质内容
   - 像朋友圈鸡汤，不是真洞察

输出格式（JSON）：
[
  {
    "paragraph": 2,
    "sentence": 1,
    "type": "太正确了",
    "original": "原句",
    "toxic_comment": "你这个说法放在任何一篇文章里都成立，所以在这篇文章里毫无意义。你到底想说什么？",
    "severity": "高"
  },
  {
    "paragraph": 4,
    "sentence": 3,
    "type": "软弱无力",
    "original": "原句",
    "toxic_comment": "'某种程度上'？你自己都不信吧。要么下判断，要么闭嘴。",
    "severity": "中"
  }
]

额外要求：
- 最后给一个整体毒舌评分（满分 10 分）和一句话总结
- 评分标准：8 分以上算能看，5-7 分算平庸，5 分以下算废纸
- 不要手下留情，但也不要为了骂而骂，每条点评必须有具体依据
```

**输出示例**：
```json
[
  {
    "paragraph": 3,
    "sentence": 2,
    "type": "太正确了",
    "original": "选择适合自己的道路很重要。",
    "toxic_comment": "这句话印在成功学书籍封底都嫌过时。你到底要说什么道路？为什么重要？",
    "severity": "高"
  }
]
```

**关键约束**：
- **只用一个角色**，不需要多读者并行
- **不找低级 AI 套话**（"综上所述""值得深思"这些 Stage 6 已经处理过）
- **专找勇气问题**：太正确、软弱、不敢站队、假大空
- 点评必须具体，不能泛泛说"不好"
- 问题必须定位到"第 X 段第 Y 句"

---

### 第 8 阶段：定向改写

**目的**：根据第 7 阶段毒舌同行的点评，定向改写文章中的"勇气问题"（太正确、软弱、不敢站队、假大空、套路感、伪金句）。

**AI 调用**：✅ AI

**输入**：第 7 阶段毒舌问题清单 + 第 6 阶段产出的改写后初稿

**定向改写 Prompt 模板**：
```
你是一位资深编辑。请根据毒舌同行的点评，改写文章中的问题句子。

原文（JSON 分块初稿）：
[第 6 阶段输出]

毒舌点评清单：
[第 7 阶段输出]

改写要求：
- 只改点评中标记的问题句子，保留其他部分
- 重点解决：太正确、软弱无力、不敢站队、假大空、套路感、伪金句
- 改写后要比原句更有立场、更具体、更像真人写的
- 保持用户风格不变
- 不要为了"让毒舌满意"而过度修改，改到合理即可
- 每处改写说明"原问题 → 改写理由"

输出格式：与第 6 阶段相同的 JSON 分块初稿格式
{
  "draft": [
    {"paragraph_index": 1, "responsibility": "...", "content": "..."},
    ...
  ]
}
```

**关键约束**：
- 局部修改而非全篇重写（保留好的部分）
- 改写的目标是"更有勇气"，不是"更正确"
- 横切：再次确认"改写没破坏用户风格"

---

### 第 9 阶段：节奏打磨（风格主保护区）

**目的**：句子层面打磨，只砍"信息密度低且不符合用户风格"的内容。这是全文的最终"调气"阶段。

**AI 调用**：✅ AI

**输入**：第 8 阶段产出

**与 Stage 6（韵律改写）的区别**：
- **Stage 9 是"调气"**：凭阅读感觉打磨，不依赖规则清单
- **Stage 6 是"治病"**：只改第 5 阶段规则检测出的具体问题
- **Stage 9 关注整体感觉**：开场是否有力、过渡是否自然、结尾是否有余味
- **Stage 6 关注形式指标**：句长、标点、句首词是否单调
- **Stage 9 在 Stage 6 之后**：形式问题解决后，再调感觉

**节奏打磨 Prompt 模板**：
```
你是一位文字编辑，负责做最后的节奏打磨。

原文（JSON 分块初稿）：
[第 8 阶段输出]

风格约束：
[用户风格提示词原文]

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
    {"paragraph_index": 1, "responsibility": "...", "content": "..."},
    ...
  ]
}

要求：
- 只改需要改的地方，不全篇重写
- 每段 content 保持自然段落，不是 bullet list
- 不要砍掉禁区内的内容
- 改动后给出一个简短的修改摘要：改了哪几处、为什么
```

**输出**：JSON 分块初稿（已做最终节奏打磨）+ 修改摘要。

**横切约束：风格提示词作为禁区依据**
- 用户说"爱用'你想想'" → 即使显得重复，不砍
- 用户说"反问多" → 即使节奏跳跃，保留
- 用户说"少用感叹号" → 即使语气平淡，不加

---

### 第 10 阶段：字数统计（Deterministic）

**目的**：统计实际字数与目标字数的差距，为调整阶段提供数据。只做统计，不做判断。

**AI 调用**：❌ 不访问（纯规则代码）

**输入**：第 9 阶段产出的 JSON 分块初稿 + 用户选择的目标字数 N

**处理逻辑**（伪代码）：

```python
def count_words(draft_json, target):
    """
    统计全文汉字数（不含标点），与目标字数比较。
    """
    total = 0
    for para in draft_json["draft"]:
        total += count_chinese_chars(para["content"])
    
    return {
        "target": target,
        "actual": total,
        "diff": total - target,
        "status": "over" if total > target else "under" if total < target else "ok"
    }
```

**输出**：字数统计报告（JSON）

```json
{
  "target": 800,
  "actual": 1150,
  "diff": 350,
  "status": "over"
}
```

**关键约束**：
- 只统计，不判断"该删什么"
- 统计规则要稳定（如：是否含标点、是否含空格）
- 输出必须是结构化 JSON，供 Stage 11 消费

---

### 第 11 阶段：字数调整（AI）

**目的**：根据第 10 阶段的字数统计报告，只在文章**超过**目标字数时决定删什么。字数是派生结果，质量优先；字数不足时无需硬补。

**AI 调用**：✅ AI

**输入**：第 9 阶段产出的 JSON 分块初稿 + 第 10 阶段产出的字数统计报告

**字数调整 Prompt 模板**：
```
你是一位资深编辑。请根据字数统计报告，对文章做最后的字数调整。

原文（JSON 分块初稿）：
[第 9 阶段输出]

字数统计报告：
[第 10 阶段输出]

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
- 绝对不删：核心观点句、关键证据、用户风格口癖
```

**输出**：字数调整建议清单 + 调整后 JSON 分块初稿。

**关键约束**：
- 调整目标是"质量优先"，不是"严格达标"
- 只处理超字数情况，不处理欠字数情况
- 优先删过渡句和重复内容，不删核心论点
- 不破坏用户风格

---

### 第 12 阶段：导出模板渲染（平台样式适配）

**目的**：根据用户在爱创作中选择的导出模板（微信公众号、小红书、今日头条、知乎、百家号、抖音图文等），把第 11 阶段产出的最终文章渲染成对应平台的可发布格式。**只改呈现样式，不改文章内容**。

**AI 调用**：❌ 不访问（纯模板渲染）

**输入**：第 11 阶段产出的 JSON 分块初稿 + 用户选择的模板 ID

**模板来源**：爱创作已有的模板库（`templatePresets`、`getTemplateStyles`），按平台分组：`wechat`、`xiaohongshu`、`toutiao`、`baijiahao`、`zhihu`、`douyin`、`general`。

**处理逻辑**（伪代码）：

```python
def render_export_template(draft_json, template_id):
    """
    根据模板 ID 将最终文章渲染为平台-specific 输出。
    纯规则代码，不调用 LLM。
    """
    template = load_template(template_id)
    paragraphs = [p["content"] for p in draft_json["draft"]]
    
    rendered = template.apply(
        title=draft_json["title"],
        paragraphs=paragraphs,
        style_hints=draft_json["style_prompt"]
    )
    
    return {
        "format": template.output_format,        # html / markdown / docx / card-json
        "platform": template.platform,           # wechat / xiaohongshu / ...
        "rendered_document": rendered,
        "source_draft": draft_json               # 保留原文，便于二次编辑
    }
```

**各平台典型适配规则**：

| 平台 | 样式重点 |
|------|----------|
| 微信公众号 | 支持引用块、分段小标题、作者信息栏、行间距 1.75 |
| 小红书 | 段落短、每段 1-3 行、预留封面图/话题标签区、emoji 克制 |
| 今日头条 | 段落紧凑、避免标题党敏感词、支持加粗重点 |
| 知乎 | 支持分级标题、引用来源、结论前置 |
| 百家号 | 适合信息流阅读，段落不宜过长，支持摘要卡片 |
| 抖音图文 | 短文案 + 多图卡片，每页文字不超过 80 字 |
| 通用 | 保留原始段落结构，仅做基础排版 |

**输出**：带平台样式的导出文档 + 原始 JSON 备份。

**关键约束**：
- **只改格式，不改字**：`content` 文本内容原样保留
- 模板渲染失败时回退到纯文本/基础 HTML 输出
- 不覆盖第 9 阶段保护下来的用户风格口癖或特色句式
- 每套模板的渲染规则必须可配置、可回归测试

---

## 4. AI 调用失败重试

每个 AI 阶段（第 2、3、4、6、7、8、9、11 阶段）都必须实现统一的失败重试机制。程序化阶段（第 1、5、10、12 阶段）不调用 LLM，因此不参与该重试机制。

### 4.1 触发重试的失败类型

- **调用异常**：LLM API 超时、网络错误、5xx 服务端错误
- **JSON 解析失败**：返回内容不是合法 JSON
- **Schema 校验失败**：返回 JSON 缺少必填字段，或字段类型与该阶段约定不符
- **约束违反**：返回内容违反该阶段硬约束（如 Stage 4 把"推断"素材标为"已知"）

### 4.2 重试配置

重试次数与策略从创作设置配置读取：

```yaml
generation:
  llm_retry_max_attempts: 3        # 默认最多重试 3 次
  llm_retry_base_delay_ms: 500     # 首次重试等待 500ms
  llm_retry_backoff_multiplier: 2  # 指数退避倍数
```

### 4.3 重试策略

- 同一 prompt 最多重试 `llm_retry_max_attempts` 次，不无限循环
- 首次失败立即重试；后续按指数退避等待（500ms → 1000ms → 2000ms）
- 每次重试把上一次的错误信息（如 "JSON 解析失败：缺少 draft 字段"）注入 prompt，要求模型自纠
- 重试过程必须记录日志，包含阶段名、尝试次数、错误类型、最终状态

### 4.4 重试耗尽后的回退

| 阶段 | 耗尽后回退 |
|------|------------|
| 第 2 阶段 | 报错，无法生成大纲 |
| 第 3 阶段 | 降级到不依赖数据的论证方式 |
| 第 4 阶段 | 回退到第 3 阶段补素材 |
| 第 6 阶段 | 回退到第 4 阶段初稿 |
| 第 7 阶段 | 跳过外部审视，直接进入第 8 阶段 |
| 第 8 阶段 | 回退到第 6 阶段输出 |
| 第 9 阶段 | 回退到第 8 阶段输出 |
| 第 11 阶段 | 跳过字数调整，直接输出第 9 阶段结果 |

## 5. 创作模板配置化设计

前文描述的 12 阶段流水线作为**默认创作模板**。产品需要支持用户/运营基于这 12 阶段创建自定义创作模板：每个阶段可独立配置类型、提示词与规则参数，新建模板时自动填充默认值。

---

### 5.1 模板 = 12 个阶段配置

每个创作模板由 12 个阶段配置组成，阶段索引 1–12 与第 3 节完全对应，顺序固定、不可增删。模板运行时按顺序执行阶段，上一阶段的输出作为下一阶段的输入。

### 5.2 阶段类型

每个阶段必须选择以下三种类型之一：

| 类型 | 说明 | 是否调用 LLM | 典型阶段 |
|------|------|--------------|----------|
| **AI** | 通过可编辑提示词调用大模型完成该阶段 | 是 | 2、3、4、6、7、8、9、11 |
| **RULE** | 程序化规则，参数可配置 | 否 | 5、10 |
| **STATIC** | 固定输出或内容展示，不调用 AI | 否 | 1、12 |

- **AI 类型**：需要写提示词，运行时调用 LLM。
- **RULE 类型**：不需要提示词，只需要配置规则与阈值。
- **STATIC 类型**：不需要提示词，只展示该阶段产出的固定内容说明。

### 5.3 新建模板默认填充

用户创建新模板时，系统自动复制默认 12 阶段配置作为初始值：

- **AI 阶段**：自动填入第 3 节各阶段 Prompt 模板作为默认提示词。
- **RULE 阶段**：自动填入默认规则参数（如第 5 阶段的阈值、第 10 阶段的统计规则）。
- **STATIC 阶段**：自动填入默认输出说明（如第 1 阶段生成 `user_context_block`、第 12 阶段按平台模板渲染）。

运营/用户可在此基础上修改，无需从零编写提示词。

### 5.4 模板数据结构示例

```yaml
creative_template:
  id: "default-v1"
  name: "默认去 AI 味模板"
  description: "标准 12 阶段去 AI 味写作流水线"
  stages:
    - index: 1
      name: "意图锚定"
      type: "STATIC"
      enabled: true
      static_config:
        output_format: "user_context_block"
        description: "把用户的标题、核心观点、目标读者、风格提示词原文拼接为标准化上下文块，不做任何 AI 处理。"

    - index: 2
      name: "结构骨架"
      type: "AI"
      enabled: true
      ai_config:
        prompt_template: |
          你是一位资深编辑。请根据以下文章意图，为这篇文章生成一份职责式大纲。
          [user_context_block]
          ...
        placeholders:
          - user_context_block
        model_params:
          temperature: 0.7

    - index: 5
      name: "韵律检测"
      type: "RULE"
      enabled: true
      rule_config:
        metrics:
          - name: "uniform_length"
            enabled: true
            threshold: 5
            description: "连续 3 句字数差异在 ±5 字以内判为 AI 均匀节律"
          - name: "no_breath"
            enabled: true
            threshold: 35
            description: "超过 35 个汉字未出现句末标点判为长句无气口"
          - name: "monotonous_start"
            enabled: true
            threshold: 3
            description: "连续 5 句开头词中 3 个以上词性相同判为句首单调"
```

### 5.5 UI 展示与编辑

模板编辑页面以 12 个卡片（或折叠面板）展示阶段：

- 每个卡片默认收起，显示：阶段序号、阶段名称、类型标签（AI / RULE / STATIC）、启用开关。
- 点击卡片展开后，根据阶段类型显示不同编辑区：
  - **AI 类型**：显示可编辑的 Prompt 文本框，占位符（如 `[user_context_block]`）高亮显示；下方显示模型参数（temperature、max_tokens 等）。
  - **RULE 类型**：显示规则参数表单，可增删规则、调整阈值、设置风格豁免项。
  - **STATIC 类型**：只读显示该阶段产出的内容说明，不展示 Prompt 编辑框。
- 若某阶段不需要 AI，则界面不显示提示词编辑区，只显示"提供的内容"或规则配置。
- AI 阶段若关闭，运行时按第 4 节重试耗尽后的回退策略处理。

### 5.6 提示词占位符

AI 阶段 Prompt 模板使用占位符引用上游输出，运行时引擎按顺序填充：

| 占位符 | 来源阶段 | 说明 |
|--------|----------|------|
| `[user_context_block]` | 第 1 阶段 | 用户上下文块 |
| `[outline]` | 第 2 阶段 | 职责式大纲 |
| `[materials]` | 第 3 阶段 | 素材清单 |
| `[draft]` | 第 4 / 6 / 8 / 9 阶段 | 分块初稿 |
| `[rhythm_issues]` | 第 5 阶段 | 韵律问题清单 |
| `[toxic_review]` | 第 7 阶段 | 毒舌同行点评清单 |
| `[word_count_report]` | 第 10 阶段 | 字数统计报告 |

### 5.7 规则可配置项

第 5 阶段（韵律检测）和第 10 阶段（字数统计）的规则参数必须可配置：

| 阶段 | 可配置项 | 默认值 | 说明 |
|------|----------|--------|------|
| 第 5 阶段 | `uniform_length.threshold` | 5 字 | 连续 3 句字数差异阈值 |
| 第 5 阶段 | `no_breath.threshold` | 35 字 | 无句末标点的最长字符数 |
| 第 5 阶段 | `monotonous_start.threshold` | 3 个 | 连续 5 句句首中相同词性数量阈值 |
| 第 5 阶段 | `style_exemptions` | 空列表 | 用户风格中明确要求的元素，检测时豁免 |
| 第 10 阶段 | `count_punctuation` | false | 字数统计是否包含标点 |
| 第 10 阶段 | `count_whitespace` | false | 字数统计是否包含空格 |
| 第 10 阶段 | `word_count_mode` | `chinese_char` | 汉字按字计，英文按词计 |

### 5.8 阶段启用与回退

每个阶段可独立启用/禁用：

- **AI 阶段禁用**：等同于该阶段 LLM 调用失败且重试耗尽，按第 4 节回退表处理。
- **RULE 阶段禁用**：跳过该阶段，下游使用上一阶段输出继续执行。
- **STATIC 阶段禁用**：必须指定替代输入来源，否则运行时直接报错。

### 5.9 关键约束

1. **阶段顺序固定**：模板必须包含且仅包含 12 个阶段，顺序不可调整，以保证输入/输出依赖链稳定。
2. **第 1 阶段必须为 STATIC**：确保用户 4 项输入被原文组装，不被 AI 修改。
3. **第 12 阶段必须为 STATIC 或 RULE**：只做平台模板渲染，不修改文章内容。
4. **提示词必须可编辑**：所有 AI 阶段的默认提示词允许用户修改，但修改后需校验占位符完整性。
5. **模板版本化**：模板修改后生成新版本，已开始运行的生成任务仍按旧版本执行，避免中途变更导致输出不一致。

### 5.10 默认创作模板完整配置

下面是系统内置默认模板 `default-v1` 的完整配置。新建模板时，系统复制该配置作为初始值。AI 阶段的默认提示词全文见第 3 节对应阶段，这里用占位符和结构说明标注。

```yaml
creative_template:
  id: "default-v1"
  name: "默认去 AI 味模板"
  description: "标准 12 阶段去 AI 味写作流水线"
  version: 1
  is_builtin: true
  is_default: true
  stages:
    - index: 1
      name: "意图锚定"
      type: "STATIC"
      enabled: true
      static_config:
        output_format: "user_context_block"
        output_schema:
          type: "object"
          required: ["context", "user_context_block"]
        description: |
          把用户的标题、核心观点、目标读者、风格提示词原文拼接为标准化上下文块。
          不调用 AI，不修改、不润色、不添加任何用户未提供的字段。

    - index: 2
      name: "结构骨架"
      type: "AI"
      enabled: true
      ai_config:
        prompt_template: |
          # 默认提示词见第 3 节「第 2 阶段：结构骨架」
          # 占位符：[user_context_block]
          ...
        placeholders:
          - "user_context_block"
        required_inputs:
          - "user_context_block"
        output_schema:
          type: "object"
          required: ["paragraphs"]
        model_params:
          temperature: 0.7
          max_tokens: 2000

    - index: 3
      name: "素材清单"
      type: "AI"
      enabled: true
      ai_config:
        prompt_template: |
          # 默认提示词见第 3 节「第 3 阶段：素材清单」
          # 占位符：[user_context_block]、[outline]
          ...
        placeholders:
          - "user_context_block"
          - "outline"
        required_inputs:
          - "user_context_block"
          - "outline"
        output_schema:
          type: "object"
          required: ["materials"]
        model_params:
          temperature: 0.5
          max_tokens: 3000

    - index: 4
      name: "分块初稿"
      type: "AI"
      enabled: true
      ai_config:
        prompt_template: |
          # 默认提示词见第 3 节「第 4 阶段：分块初稿」
          # 占位符：[user_context_block]、[outline]、[materials]
          ...
        placeholders:
          - "user_context_block"
          - "outline"
          - "materials"
        required_inputs:
          - "user_context_block"
          - "outline"
          - "materials"
        output_schema:
          type: "object"
          required: ["draft"]
        model_params:
          temperature: 0.8
          max_tokens: 4000

    - index: 5
      name: "韵律检测"
      type: "RULE"
      enabled: true
      rule_config:
        description: "对第 4 阶段产出的 JSON 分块初稿进行三项韵律指标扫描，只检测、不改写。"
        input_ref: "draft"
        output_schema:
          type: "array"
          items:
            type: "object"
        metrics:
          - name: "uniform_length"
            enabled: true
            threshold: 5
            window_size: 3
            description: "连续 3 句字数差异在 ±5 字以内判为 AI 均匀节律"
          - name: "no_breath"
            enabled: true
            threshold: 35
            description: "超过 35 个汉字未出现句末标点判为长句无气口"
          - name: "monotonous_start"
            enabled: true
            threshold: 3
            window_size: 5
            description: "连续 5 句开头词中 3 个以上词性相同判为句首单调"
        style_exemptions: []

    - index: 6
      name: "韵律改写"
      type: "AI"
      enabled: true
      ai_config:
        prompt_template: |
          # 默认提示词见第 3 节「第 6 阶段：韵律改写」
          # 占位符：[draft]、[rhythm_issues]
          ...
        placeholders:
          - "draft"
          - "rhythm_issues"
        required_inputs:
          - "draft"
          - "rhythm_issues"
        output_schema:
          type: "object"
          required: ["draft"]
        model_params:
          temperature: 0.6
          max_tokens: 4000

    - index: 7
      name: "外部审视"
      type: "AI"
      enabled: true
      ai_config:
        prompt_template: |
          # 默认提示词见第 3 节「第 7 阶段：外部审视」
          # 占位符：[draft]
          ...
        placeholders:
          - "draft"
        required_inputs:
          - "draft"
        output_schema:
          type: "array"
        model_params:
          temperature: 0.9
          max_tokens: 3000

    - index: 8
      name: "定向改写"
      type: "AI"
      enabled: true
      ai_config:
        prompt_template: |
          # 默认提示词见第 3 节「第 8 阶段：定向改写」
          # 占位符：[draft]、[toxic_review]
          ...
        placeholders:
          - "draft"
          - "toxic_review"
        required_inputs:
          - "draft"
          - "toxic_review"
        output_schema:
          type: "object"
          required: ["draft"]
        model_params:
          temperature: 0.7
          max_tokens: 4000

    - index: 9
      name: "节奏打磨"
      type: "AI"
      enabled: true
      ai_config:
        prompt_template: |
          # 默认提示词见第 3 节「第 9 阶段：节奏打磨」
          # 占位符：[draft]、[user_context_block]
          ...
        placeholders:
          - "draft"
          - "user_context_block"
        required_inputs:
          - "draft"
          - "user_context_block"
        output_schema:
          type: "object"
          required: ["draft"]
        model_params:
          temperature: 0.6
          max_tokens: 4000

    - index: 10
      name: "字数统计"
      type: "RULE"
      enabled: true
      rule_config:
        description: "统计实际字数与目标字数的差距，只统计、不做判断。"
        input_ref: "draft"
        output_schema:
          type: "object"
          required: ["target", "actual", "diff", "status"]
        count_config:
          count_punctuation: false
          count_whitespace: false
          word_count_mode: "chinese_char"
          # chinese_char: 汉字按字，英文按词
          # char_all: 所有字符
          # word_all: 所有词

    - index: 11
      name: "字数调整"
      type: "AI"
      enabled: true
      ai_config:
        prompt_template: |
          # 默认提示词见第 3 节「第 11 阶段：字数调整」
          # 占位符：[draft]、[word_count_report]
          ...
        placeholders:
          - "draft"
          - "word_count_report"
        required_inputs:
          - "draft"
          - "word_count_report"
        output_schema:
          type: "object"
          required: ["action", "recommendations"]
        model_params:
          temperature: 0.5
          max_tokens: 3000

    - index: 12
      name: "导出模板渲染"
      type: "STATIC"
      enabled: true
      static_config:
        output_format: "platform_rendered_document"
        output_schema:
          type: "object"
          required: ["format", "platform", "rendered_document", "source_draft"]
        description: |
          根据用户选择的导出模板（微信公众号、小红书、今日头条、知乎、百家号、抖音图文、通用），
          把第 11 阶段产出的最终文章渲染成对应平台的可发布格式。
          只改呈现样式，不改文章内容。
```

### 5.11 模板运行时流程

模板执行引擎负责按配置运行 12 个阶段。核心流程如下：

#### 5.11.1 执行流程

```
加载模板 → 绑定版本 → 校验依赖 → 顺序执行阶段 → 输出最终文档
            ↑            ↓         ↑
          用户选择模板  占位符替换   失败重试/回退
```

#### 5.11.2 详细步骤

1. **加载模板**
   - 根据生成任务创建时选择的 `template_id` 加载模板配置。
   - 同时绑定模板版本号到任务，后续模板修改不影响已创建任务。

2. **校验阶段依赖**
   - 检查每个启用阶段的 `required_inputs` 是否能在上游阶段找到输出。
   - 检查 AI 阶段 Prompt 中的占位符是否都在 `placeholders` 列表中声明。
   - 检查第 1 阶段是否为 STATIC、第 12 阶段是否为 STATIC 或 RULE。

3. **初始化上下文**
   - 创建 `stage_outputs` 字典，用于保存每个阶段的输出。
   - 第 1 阶段（STATIC）直接根据用户输入生成 `user_context_block`。

4. **顺序执行阶段**
   - 按 index 1–12 依次执行每个阶段。
   - **STATIC**：执行固定逻辑，结果写入 `stage_outputs`。
   - **RULE**：按 `rule_config` 运行程序化规则，结果写入 `stage_outputs`。
   - **AI**：替换 Prompt 中的占位符为 `stage_outputs` 中的上游输出，调用 LLM，解析输出并校验 schema。

5. **占位符替换规则**
   - `[user_context_block]` → `stage_outputs[1].user_context_block`
   - `[outline]` → `stage_outputs[2]`
   - `[materials]` → `stage_outputs[3]`
   - `[draft]` → 最近一次 `draft` 输出（第 4 / 6 / 8 / 9 阶段）
   - `[rhythm_issues]` → `stage_outputs[5]`
   - `[toxic_review]` → `stage_outputs[7]`
   - `[word_count_report]` → `stage_outputs[10]`

6. **失败重试与回退**
   - AI 阶段失败按第 4 节重试机制处理。
   - 重试耗尽后按对应阶段的回退策略执行。

7. **阶段禁用处理**
   - AI 阶段禁用 → 按回退策略执行。
   - RULE 阶段禁用 → 跳过，下游使用上一阶段输出。
   - STATIC 阶段禁用 → 若未指定替代输入则报错。

8. **输出最终文档**
   - 第 12 阶段输出即为最终可发布文档。
   - 保留所有中间阶段输出，便于调试和二次编辑。

#### 5.11.3 运行时上下文示例

```yaml
runtime_context:
  task_id: "task-xxx"
  template_id: "default-v1"
  template_version: 1
  user_inputs:
    title: "..."
    core_viewpoint: "..."
    target_reader: "..."
    style_prompt: "..."
  target_word_count: 800
  selected_export_template: "wechat-default"
  stage_outputs:
    1: { user_context_block: "...", context: {...} }
    2: { paragraphs: [...] }
    3: { materials: [...] }
    4: { draft: [...] }
    5: [ ... ]
    6: { draft: [...] }
    7: [ ... ]
    8: { draft: [...] }
    9: { draft: [...] }
    10: { target: 800, actual: 1150, diff: 350, status: "over" }
    11: { action: "cut", recommendations: [...] }
    12: { format: "html", platform: "wechat", rendered_document: "...", source_draft: {...} }
```

#### 5.11.4 关键约束

- 阶段执行必须严格按 1–12 顺序，不能并行。
- 每个阶段只能读取上游阶段输出，不能读取下游阶段。
- AI 阶段输出必须能通过 JSON Schema 校验，否则触发重试。
- 模板版本在任务创建时锁定，运行时不可切换版本。

### 5.12 示例自定义模板

下面给出三个基于默认模板改造而来的自定义模板示例，展示如何修改 AI 提示词和 RULE 参数。

#### 5.12.1 小红书爆款体模板

目标：生成适合小红书发布的短图文，段落更短、节奏更快、emoji 克制，并相应放宽韵律检测。

**核心改动**：

| 阶段 | 改动 |
|------|------|
| 第 2 阶段 | 大纲职责强调"第一句必须抓眼球""每段 1-3 行" |
| 第 4 阶段 | 提示词要求"每段最多 3 句话，多用短句和口语" |
| 第 5 阶段 | `no_breath` 阈值从 35 调整为 25（短平台气口更短） |
| 第 9 阶段 | 打磨目标增加"砍掉书面语，保留口语感" |
| 第 11 阶段 | 超字数时优先删过渡句，保留情绪词和具体画面 |

**第 4 阶段提示词片段示例**：

```yaml
prompt_template: |
  你是一位小红书博主，请用口语化风格写初稿。

  [user_context_block]
  [outline]
  [materials]

  写作要求：
  - 每段最多 3 句话，整体段落短而有力
  - 少用"我觉得""其实""说实话"等弱化表达
  - 多给具体画面，少给抽象结论
  - 禁用"在当今社会""值得深思""综上所述"
  - 风格提示词原文注入，不转述
```

#### 5.12.2 数据严谨风模板

目标：用于财经、科普类文章，对素材真实性要求更高，减少 AI 推测。

**核心改动**：

| 阶段 | 改动 |
|------|------|
| 第 3 阶段 | 素材清单中所有"推断"项必须标注来源，禁止把推断标为已知 |
| 第 4 阶段 | 提示词要求"每个数据论点必须引用素材清单中的已知项，否则降级" |
| 第 7 阶段 | 毒舌同行额外审查"数据是否被滥用""因果推断是否过度" |
| 第 10 阶段 | 字数统计采用 `word_all` 模式，中英文统一按词计 |

**第 3 阶段提示词片段示例**：

```yaml
prompt_template: |
  你是一位事实核查编辑。列出每段需要的支撑素材，并严格标注来源可靠性。

  [user_context_block]
  [outline]

  规则：
  - 只有用户明确提供或你能 100% 验证的才能标"已知"
  - 所有 AI 推测必须标"推断"，并说明推断依据
  - 无可靠来源的数据，建议降级为类比、对比或提问
  - 严禁编造数据、案例、人名、时间
```

#### 5.12.3 极简二稿模板

目标：只保留最核心的 4 个 AI 阶段，其余关闭，用于快速出稿。

**阶段启用配置**：

| 阶段 | 类型 | 启用 | 说明 |
|------|------|------|------|
| 1 | STATIC | 是 | 意图锚定 |
| 2 | AI | 是 | 结构骨架 |
| 4 | AI | 是 | 分块初稿 |
| 9 | AI | 是 | 节奏打磨 |
| 12 | STATIC | 是 | 导出渲染 |
| 其余 | - | 否 | 跳过 |

**回退说明**：

- 第 6、7、8 阶段关闭 → 不执行韵律改写、外部审视、定向改写。
- 第 11 阶段关闭 → 不调整字数，直接输出第 9 阶段结果。
- 风险：AI 味和字数控制较弱，但出稿速度最快，适合草稿或内部备忘。

#### 5.12.4 自定义模板设计建议

1. **先复制默认模板**：不要从零写，避免遗漏阶段依赖或占位符。
2. **一次只改一个阶段**：便于对比效果，出问题容易定位。
3. **保留核心阶段**：第 1、4、9、12 阶段建议保留，分别负责输入、主体、打磨、输出。
4. **规则阈值要配合平台**：小红书、抖音图文用短句规则更严；知乎、公众号可适当放宽。
5. **提示词修改后要做回归测试**：用同一组输入跑默认模板和自定义模板，对比输出差异。

### 5.13 前端模板编辑页线框

本节描述创作模板后台管理页的布局与交互，目标是让用户能直观看到 12 个阶段、快速判断每个阶段类型、并进入对应编辑区。

#### 5.13.1 页面整体布局

```
┌─────────────────────────────────────────────────────────────┐
│  顶部操作栏                                                    │
│  [返回列表]  模板名称输入框  [保存草稿] [发布] [复制模板] [删除]    │
├─────────────────────────────────────────────────────────────┤
│  左侧：模板信息                                                │
│  ───────────────                                             │
│  ID: default-v1-copy                                         │
│  版本: 1                                                     │
│  内置模板: 否                                                │
│  状态: 草稿 / 已发布                                          │
│                                                              │
│  描述：                                                       │
│  [多行文本框]                                                 │
├─────────────────────────────────────────────────────────────┤
│  右侧：12 阶段流水线（垂直时间轴）                              │
│  ─────────────────────                                       │
│  ○ 1  意图锚定            [STATIC]  [启用●]  [展开▼]          │
│  ○ 2  结构骨架            [AI]      [启用●]  [展开▼]          │
│  ○ 3  素材清单            [AI]      [启用●]  [展开▼]          │
│  ...                                                         │
│  ○ 12 导出模板渲染        [STATIC]  [启用●]  [展开▼]          │
└─────────────────────────────────────────────────────────────┘
```

#### 5.13.2 阶段卡片收起态

每个阶段以卡片形式展示，收起时显示：

- 左侧序号圆圈（1–12）
- 阶段名称
- 类型标签：`AI` / `RULE` / `STATIC`
- 启用开关
- 展开/收起箭头
- （可选）快捷提示：如 AI 阶段显示"提示词 N 个占位符"，RULE 阶段显示"N 条规则"

#### 5.13.3 AI 阶段展开态

展开后显示：

```
┌────────────────────────────────────────┐
│ 2. 结构骨架                    [AI]    │
│ ────────────────────────────────────── │
│ 模型参数                                │
│ temperature: [0.7]  max_tokens: [2000] │
│                                        │
│ 占位符声明                              │
│ [user_context_block]                    │
│                                        │
│ Prompt 编辑区                           │
│ ┌────────────────────────────────────┐ │
│ │ 你是一位资深编辑...                │ │
│ │ [user_context_block]               │ │
│ │ ...                                │ │
│ └────────────────────────────────────┘ │
│                                        │
│ [恢复默认提示词]  [占位符自动补全]       │
└────────────────────────────────────────┘
```

交互细节：

- Prompt 文本框支持占位符高亮（如 `[user_context_block]` 显示为蓝色背景标签）。
- 点击"占位符自动补全"弹出可插入占位符列表，自动插入光标位置。
- "恢复默认提示词"二次确认后覆盖当前 Prompt 为 `default-v1` 对应阶段的默认提示词。
- 模型参数只暴露 `temperature` 和 `max_tokens`，其他参数隐藏。

#### 5.13.4 RULE 阶段展开态

展开后显示规则配置表单：

```
┌────────────────────────────────────────┐
│ 5. 韵律检测                    [RULE]  │
│ ────────────────────────────────────── │
│ 输入引用: [draft]                       │
│                                        │
│ 规则列表                                │
│ ┌────────────────────────────────────┐ │
│ │ ☑ uniform_length                   │ │
│ │   阈值: [5]  窗口: [3]              │ │
│ │   描述: 连续 3 句字数差异...        │ │
│ ├────────────────────────────────────┤ │
│ │ ☑ no_breath                        │ │
│ │   阈值: [35]                        │ │
│ │   描述: 超过 35 个汉字...           │ │
│ ├────────────────────────────────────┤ │
│ │ ☑ monotonous_start                 │ │
│ │   阈值: [3]  窗口: [5]              │ │
│ │   描述: 连续 5 句句首...            │ │
│ └────────────────────────────────────┘ │
│                                        │
│ 风格豁免项                              │
│ [+ 添加]                                │
│ ┌────────────────────────────────────┐ │
│ │ 用户风格中明确要求保留的元素        │ │
│ └────────────────────────────────────┘ │
└────────────────────────────────────────┘
```

交互细节：

- 每条规则可单独启用/禁用。
- 阈值和窗口大小为数字输入框。
- 风格豁免项为字符串标签列表，可增删。

#### 5.13.5 STATIC 阶段展开态

展开后只读展示：

```
┌────────────────────────────────────────┐
│ 1. 意图锚定                 [STATIC]   │
│ ────────────────────────────────────── │
│ 输出格式: user_context_block            │
│                                        │
│ 说明：                                   │
│ 把用户的标题、核心观点、目标读者、风格    │
│ 提示词原文拼接为标准化上下文块。不调用    │
│ AI，不修改、不润色。                     │
│                                        │
│ 输出 schema:                            │
│ { context, user_context_block }         │
└────────────────────────────────────────┘
```

STATIC 阶段不可编辑内容，但可切换启用/禁用。

#### 5.13.6 全局校验与提示

- 保存时校验：
  - 第 1 阶段是否为 STATIC
  - 第 12 阶段是否为 STATIC 或 RULE
  - 每个启用 AI 阶段的 Prompt 占位符是否在 `placeholders` 中声明
  - 每个启用阶段的 `required_inputs` 是否能在上游找到输出
- 校验未通过时，错误阶段卡片边框变红，顶部显示错误摘要。
- 发布前必须至少启用 1 个 AI 阶段用于生成主体内容。

#### 5.13.7 与生成任务的关联

- 模板保存为草稿后，可被复制但不可被生成任务引用。
- 模板发布后，生成任务创建页面才能选择该模板。
- 模板发布后修改会自动生成新版本，旧版本仍被历史任务引用。

### 5.14 模板生命周期管理

本节描述模板从创建到废弃的完整生命周期，以及围绕生命周期的权限与状态管理。

#### 5.14.1 模板类型

| 类型 | 来源 | 能否编辑 | 能否删除 |
|------|------|----------|----------|
| **内置模板**（`is_builtin: true`） | 系统随产品发布预置 | 仅超级管理员可改 | 不可删除 |
| **用户模板**（`is_builtin: false`） | 管理员在后台创建 | 创建者与超级管理员可改 | 创建者与超级管理员可删 |

内置模板 `default-v1` 是所有用户模板的复制源。

#### 5.14.2 状态机

```
                  ┌─────────┐
                  │  草稿   │
                  └────┬────┘
                       │ 发布
                       ▼
                  ┌─────────┐         ┌─────────┐
                  │  已发布  │ ──────▶ │  已下线  │
                  └────┬────┘  下线    └─────────┘
                       │                ▲
                       └────────────────┘
                          重新发布
```

| 状态 | 是否可被生成任务引用 | 是否可编辑 |
|------|----------------------|------------|
| 草稿 | 否 | 是 |
| 已发布 | 是 | 是（修改会产生新版本） |
| 已下线 | 否 | 是（重新发布需创建新版本） |

#### 5.14.3 核心操作

| 操作 | 触发 | 效果 |
|------|------|------|
| **复制模板** | 从模板列表/详情页"复制模板"按钮 | 创建新模板，类型为用户模板，状态为草稿，内容是源模板的完整拷贝（含全部 12 阶段配置） |
| **保存草稿** | 编辑页"保存草稿"按钮 | 校验通过后写入数据库，状态保持草稿；不创建新版本 |
| **发布** | 编辑页"发布"按钮 | 校验通过后写入数据库，状态变为已发布；首次发布创建版本 1，后续发布基于草稿递增版本号 |
| **编辑已发布** | 编辑页修改已发布模板 | 提示用户"将创建新版本"，确认后进入草稿态编辑 |
| **下线** | 模板详情页"下线"按钮 | 状态变为已下线，正在运行的生成任务继续完成，新任务不能再引用 |
| **重新发布** | 已下线模板的"重新发布"按钮 | 创建新版本并发布，旧版本保留 |
| **删除** | 模板详情页"删除"按钮 | 仅草稿或已下线可物理删除；已发布模板执行软删除 |

#### 5.14.4 版本管理

- 模板每次发布生成新版本号（自增整数）。
- 同一模板多个版本共存，旧版本不可修改但可被历史任务引用。
- 生成任务创建时锁定模板版本（见 5.11.4），保证可复现。
- 模板列表展示"最新版本号"，详情页提供版本切换器查看历史版本（只读）。

#### 5.14.5 权限模型

| 角色 | 权限 |
|------|------|
| 超级管理员 | 内置模板可改、用户模板可改可删、任何模板可发布/下线 |
| 模板管理员 | 创建用户模板、编辑自己创建的模板、发布/下线自己创建的模板 |
| 普通运营 | 查看模板列表、复制模板到自己名下、不能直接编辑 |
| 普通用户 | 仅在生成任务页面选择已发布的用户模板或默认模板 |

权限模型基于现有管理端 RBAC 体系（见 `docs/architecture/security-conventions.md`），不引入新的角色。

#### 5.14.6 删除策略

- 草稿模板可直接物理删除。
- 已发布模板必须先下线，再物理删除。
- 物理删除前检查是否被生成任务引用：
  - 若有引用，禁止删除，提示"请先下线，等待历史任务归档后再删除"。
  - 归档策略：超过 30 天未活跃的生成任务自动归档，归档后任务引用变为弱引用（仅保留模板 ID 快照，不影响模板删除）。

#### 5.14.7 关键约束

- 内置模板不可删除，但可被超级管理员修改并发布新版本。
- 用户模板不可重置为内置模板，反之亦然。
- 模板一旦发布，结构（12 阶段数量、顺序）不可调整，仅允许调整阶段配置（提示词、参数、启用状态）。
- 阶段结构变更（如增删阶段）必须通过创建新模板实现。

### 5.15 数据表与接口设计

本节定义模板配置的数据库表与管理端/用户端接口。命名遵循 `docs/architecture/mysql-table-conventions.md` 与 `docs/architecture/api-interface-conventions.md`。

> **实施说明（2026-07-09 落地）**
>
> 阶段 1 + 阶段 2 已落地（迁移 `V2.0.0_017`、`V2.0.0_018`，管理端 plan `2026-07-09-creative-template-stage1-default-seed.md` 与 `2026-07-09-creative-template-stage2-lifecycle.md`）。实际表名与本节初稿略有差异：
>
> - **逻辑身份表**：`a_creative_template` → 复用已有 `t_prompt_template`（V2.0.0_011）
> - **12 阶段配置**：`config_json` blob → 拆为独立行存 `t_prompt_template_stage`（V2.0.0_014），运行时按 stage_index 12 行装配
> - **版本快照表**：`a_creative_template_version` → 新增 `t_prompt_template_version`（V2.0.0_018）
> - **12 阶段配置作为版本快照**：`config_json` 内 `stages[]` 数组，结构与 §5.10 一致
> - **生成任务侧消费版本**：stage 1 / 2 未做，§5.15.4 / §5.15.6 留待阶段 3 落地

#### 5.15.1 数据表

模板实际分三张表：`t_prompt_template`（逻辑身份）+ `t_prompt_template_stage`（12 阶段行）+ `t_prompt_template_version`（版本快照）。逻辑身份表保存当前元数据；阶段表保存当前草稿/已发布阶段的 12 行配置；版本表保存每次发布的历史快照。

```sql
-- 模板逻辑身份表（已存在，V2.0.0_011 / V2.0.0_018 加 template_status + latest_published_version）
CREATE TABLE IF NOT EXISTS t_prompt_template (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(64) NOT NULL COMMENT '模板名称（管理后台显示）',
    base_content MEDIUMTEXT COMMENT '基础内容（已废弃，保留兼容）',
    enabled TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否启用（与 template_status=PUBLISHED 等价）',
    template_status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '模板状态：0-草稿，1-已发布，2-已下线（V2.0.0_018 新增）',
    latest_published_version INT UNSIGNED DEFAULT NULL COMMENT '当前最新已发布版本号（V2.0.0_018 新增）',
    remark VARCHAR(256) DEFAULT NULL COMMENT '备注',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_t_pt_enabled (enabled),
    KEY idx_t_pt_template_status (template_status, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='创作提示词模板';

-- 12 阶段配置表（已存在，V2.0.0_014）
CREATE TABLE IF NOT EXISTS t_prompt_template_stage (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    template_id BIGINT UNSIGNED NOT NULL COMMENT '所属模板ID',
    stage_index TINYINT UNSIGNED NOT NULL COMMENT '阶段序号 1-12（设计文档固定 12 阶段）',
    stage_type VARCHAR(16) NOT NULL COMMENT 'ai_prompt / rule_config / passthrough',
    stage_key VARCHAR(32) NOT NULL COMMENT '阶段稳定标识符：outline / draft / ...',
    ai_prompt MEDIUMTEXT COMMENT '仅 stage_type=ai_prompt 有值',
    rule_config JSON COMMENT '仅 stage_type=rule_config 有值',
    enabled TINYINT UNSIGNED NOT NULL DEFAULT 1,
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_template_stage (template_id, stage_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='创作提示词模板 12 阶段配置';

-- 模板版本快照表（V2.0.0_018 新增）
CREATE TABLE IF NOT EXISTS t_prompt_template_version (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    template_id BIGINT UNSIGNED NOT NULL COMMENT '所属模板ID',
    version INT UNSIGNED NOT NULL COMMENT '版本号，从 1 开始自增',
    version_status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '版本状态：0-草稿，1-已发布，2-已下线',
    config_json JSON NOT NULL COMMENT '12 阶段配置完整快照（stages[] 数组，结构对齐 §5.10）',
    change_note VARCHAR(512) DEFAULT NULL COMMENT '本次发布变更说明',
    published_at DATETIME(3) DEFAULT NULL COMMENT '发布时间',
    published_by BIGINT UNSIGNED DEFAULT NULL COMMENT '发布人ID',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_t_pt_version (template_id, version),
    KEY idx_t_pt_version_status (version_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='创作模板版本快照';
```

`config_json` 的数据结构与 §5.10 `creative_template.stages[]` 一致。运行时按 (template_id, version) 读取，12 阶段从 stage 表装配。

> 关于 `config_hash`：阶段 2 未实现 `config_json` SHA-256 比对；发布时无去重检测。后续可加（不属于本阶段范围）。

#### 5.15.2 Flyway 脚本位置

```
project/admin/api/src/main/resources/db/migration/
├── V2.0.0_011__create_prompt_template_table.sql       # 模板主表
├── V2.0.0_012__drop_prompt_template_style_and_system_columns.sql
├── V2.0.0_014__create_prompt_template_stage_table.sql # 12 阶段
├── V2.0.0_017__seed_default_prompt_template.sql       # 阶段 1：seed 默认模板
└── V2.0.0_018__add_template_status_and_version_table.sql # 阶段 2：状态机 + 版本表
```

#### 5.15.3 管理端接口

模块前缀 `/api/v1/admin/prompt-templates`（命名沿用已有 `t_prompt_template` 表，复用 `AdminGenerationErrorCode` 308xxx 错误码段）。

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/v1/admin/prompt-templates` | 模板列表，支持按关键字筛选 |
| `GET` | `/api/v1/admin/prompt-templates/{id}` | 模板详情（含 12 阶段配置） |
| `POST` | `/api/v1/admin/prompt-templates` | 创建模板，自动建 12 阶段默认值（状态=草稿） |
| `PUT` | `/api/v1/admin/prompt-templates/{id}` | 更新模板（含 12 阶段） |
| `POST` | `/api/v1/admin/prompt-templates/{id}/init-stages` | 老模板补齐 12 阶段默认值 |
| `POST` | `/api/v1/admin/prompt-templates/{id}/enable` | 启用模板（保留兼容，状态机等价于 `actions/publish`） |
| `POST` | `/api/v1/admin/prompt-templates/{id}/disable` | 停用模板（保留兼容，状态机等价于 `actions/offline`） |
| `POST` | `/api/v1/admin/prompt-templates/{id}/actions/publish` | **阶段 2**：发布当前 12 阶段为新版本号（`PublishTemplateRequest`，含 `changeNote`） |
| `POST` | `/api/v1/admin/prompt-templates/{id}/actions/offline` | **阶段 2**：下线模板（仅 PUBLISHED 可下线） |
| `POST` | `/api/v1/admin/prompt-templates/{id}/actions/clone` | **阶段 2**：克隆源模板为新草稿（`CloneTemplateRequest`，支持 `sourceVersion`） |
| `GET` | `/api/v1/admin/prompt-templates/{id}/versions` | **阶段 2**：模板全部版本快照摘要 |
| `DELETE` | `/api/v1/admin/prompt-templates/{id}` | 删除草稿/已下线模板；内置模板（id=1）抛 `308012` |

请求体命名沿用 `PromptTemplateSaveRequest` / `PromptTemplateStageSaveItem` / `PublishTemplateRequest` / `CloneTemplateRequest`。

响应体：
- 列表项：`PromptTemplateAdminVO`（含 `isBuiltin`、`templateStatus`、`templateStatusLabel`、`latestPublishedVersion`）
- 详情：复用 `PromptTemplateAdminVO`（含 12 阶段 `PromptTemplateStageVO[]`）
- 版本摘要：`PromptTemplateVersionVO`（不含 `config_json` 全文）

#### 5.15.4 用户端接口（阶段 3 落地）

用户端只读访问，用于生成任务创建时选择模板。**当前未实现**，留给阶段 3。

预期端点（设计）：

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/v1/user/creative-templates` | 列出已发布的模板（仅返回 `template_status = 1` 的最新版本配置） |
| `GET` | `/api/v1/user/creative-templates/{id}` | 查看已发布模板详情，用于任务创建预览 |

注：实际命名可能沿用 `/api/v1/user/prompt-templates`，与表名 `t_prompt_template` 对齐。

#### 5.15.5 业务错误码

实际落地的错误码在 `AdminGenerationErrorCode`（308xxx 段）：

| 错误码 | 含义 | 来源 |
|--------|------|------|
| `308001` | 提示词模板不存在 | 已存在 |
| `308002` | 模板占位符不合法 | 已存在 |
| `308003` | 已有启用的提示词模板 | 已存在 |
| `308010` | 当前没有启用的提示词模板 | 已存在 |
| `308012` | **内置模板不可删除** | 阶段 1 新增 |
| `308013` | **模板状态不允许该操作** | 阶段 2 新增 |

> 原设计 `125xxx` 模块编码 `25` 未启用；实际沿用 `AdminGenerationErrorCode` 308xxx 段，理由是与已有 `308001`–`308010` 模板相关错误码同段，便于管理端统一处理。

#### 5.15.6 与生成任务的关联（数据层，阶段 3 落地）

> **当前未实现**。阶段 1 / 2 未对 `u_generation_task` 加列；运行时 `PipelineTemplateResolver` 直接读 `t_prompt_template.enabled=1` 的模板（无版本概念）。
>
> 阶段 3 落地时预期加：

```sql
ALTER TABLE u_generation_task
    ADD COLUMN creative_template_id BIGINT UNSIGNED DEFAULT NULL COMMENT '创作模板ID',
    ADD COLUMN creative_template_version INT UNSIGNED DEFAULT NULL COMMENT '创作模板版本号';
```

任务创建时锁定版本，删除模板前查询 `u_generation_task` 中是否存在未归档的引用（详见 5.14.6 归档策略）。

#### 5.15.7 关键约束

- 模板配置仅由管理端写入，用户端只能读取已发布的最新版本（阶段 3）。
- 草稿版本不暴露给用户端。
- 模板版本号在同一 template_id 下单调递增，不复用。
- 内置模板（`isBuiltin` 由 `id == 1L` 判定）不可删除，但可派生新版本。
- `config_hash` 字段本节设计存在但未实现，发布时无去重检测（已知 gap）。

### 5.16 实施路径与待讨论问题

#### 5.16.1 实施分阶段

| 阶段 | 目标 | 主要交付物 |
|------|------|------------|
| **阶段 1：固化默认模板** | 把现有 12 阶段流水线的提示词与规则固化为内置模板 `default-v1`，无需任何 UI | 数据表 Flyway 脚本、模板配置 JSON 文件、运行时按固定配置加载 |
| **阶段 2：管理端模板管理** | 让运营能复制/编辑/发布/下线模板 | 管理端 CRUD + 生命周期接口（5.15.3）、模板编辑页线框（5.13）落地 |
| **阶段 3：用户端模板选择** | 任务创建时能选择已发布模板 | 用户端只读接口（5.15.4）、生成任务侧消费模板版本（5.15.6）落地 |
| **阶段 4：自定义模板生效** | 用户模板能真正影响生成结果 | 运行时按 (template_id, version) 读取配置、替换占位符、调度 12 阶段执行 |
| **阶段 5：示例模板库** | 提供开箱即用的自定义模板 | 把 5.12 节的三个示例发布为预设模板 |

每个阶段结束都需完成对应的端到端验证（参考 `docs/superpowers/plans/` 现有规范格式）。

#### 5.16.2 关键接口请求/响应示例

**创建模板**：

```text
POST /api/v1/admin/creative-templates
Idempotency-Key: uuid
```

```json
{
  "name": "小红书爆款体",
  "description": "适用于小红书图文短文案的模板",
  "sourceTemplateId": 1,
  "sourceTemplateVersion": 1
}
```

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 10001,
    "name": "小红书爆款体",
    "draftVersion": 1,
    "templateStatus": 0
  }
}
```

**更新草稿配置**：

```text
PUT /api/v1/admin/creative-templates/10001/versions/1
```

```json
{
  "stages": [
    { "index": 1, "type": "STATIC", "enabled": true, "...": "..." },
    { "index": 2, "type": "AI", "enabled": true, "aiConfig": { "promptTemplate": "...", "...": "..." } }
  ]
}
```

**发布草稿**：

```text
POST /api/v1/admin/creative-templates/10001/actions/publish
```

```json
{
  "changeNote": "调整第 4 阶段提示词，要求每段不超过 3 句话"
}
```

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "templateId": 10001,
    "publishedVersion": 2,
    "publishedAt": "2026-07-09T14:30:00.000+08:00"
  }
}
```

#### 5.16.3 待讨论问题

| 编号 | 问题 | 影响范围 | 建议方向 |
|------|------|----------|----------|
| Q1 | 是否允许非 12 阶段结构？如运营想加"13 阶段：标题打分"。 | 配置模型、运行时分发 | 短期固定 12 阶段，长期通过 `stages[]` 长度可变 + 必填阶段校验实现 |
| Q2 | 内置模板 `default-v1` 升级时，是否要保留旧版供历史任务继续使用？ | 版本管理 | 必须保留，发布升级自动建新版本 |
| Q3 | 用户模板是否可以"基于某个用户模板再复制"？ | 复制操作 | 允许，但禁止循环引用（必须以内置模板为根） |
| Q4 | 模板编辑是否要做"提示词版本对比"功能（diff 视图）？ | 前端复杂度 | 后续迭代，阶段 2 不强求 |
| Q5 | LLM 调用失败重试是否对所有 AI 阶段一视同仁？ | 运行时 | 暂统一，后续按阶段成本区分 |
| Q6 | 模板占用额度如何计算？是否单独计费？ | 额度体系 | 模板自身不计费，使用模板生成的生成任务按既有规则计费 |
| Q7 | 模板配置的 Schema 校验用 JSON Schema 还是手写？ | 后端 | 建议 JSON Schema，可维护性更高 |

#### 5.16.4 风险与边界

- **配置复杂度**：12 阶段 × 3 种类型 × 多版本组合可能让配置爆炸。需要在阶段 1 提供可视化编辑器或在线模板预览能力。
- **运行时一致性**：模板配置变更可能与正在运行的任务冲突，5.11.4 通过版本锁定已规避，但需在监控中观察是否有跨版本污染。
- **LLM 成本**：每个 AI 阶段都可能产生调用费用，建议在阶段 4 上线前建立"每模板平均调用次数"的监控指标。
- **数据迁移**：已发布但被引用的模板下线后，仍可能被 30 天内的任务引用，需要保留快照至少 30 天。

### 5.17 监控与可观测性

12 阶段流水线涉及多次 LLM 调用、规则检测、模板渲染，需要在生产环境具备完整的监控能力。本节定义关键指标、日志与告警。

#### 5.17.1 核心指标

按维度分为四类：

| 维度 | 指标 | 说明 |
|------|------|------|
| **业务** | 任务成功率 | 单个任务 12 阶段全部成功的比例 |
| **业务** | 平均完成时长 | 从任务创建到第 12 阶段产出的耗时 |
| **业务** | 各阶段平均调用次数 | 按模板 × 阶段统计 LLM 调用次数，用于成本核算 |
| **业务** | 字数一次达标率 | 第 11 阶段无需删减即达标的概率 |
| **性能** | 单阶段 P50/P95/P99 耗时 | 按阶段拆分 |
| **性能** | LLM 调用 QPS | 按阶段和模板拆分 |
| **质量** | 第 7 阶段毒舌评分均值 | 模板质量好坏的间接指标 |
| **质量** | 第 5 阶段检测出问题数均值 | 模板规则命中密度 |
| **成本** | 单任务平均 token 消耗 | 按模板 × 阶段 |
| **成本** | 单任务平均成本（元） | 用于对账 |

#### 5.17.2 日志规范

日志命名遵循 `docs/architecture/logging-conventions.md`，并补充：

- 每个阶段的进入与退出都打一条 `INFO` 日志，包含：`taskId`、`templateId`、`templateVersion`、`stageIndex`、`stageName`、`durationMs`、`status`。
- 失败阶段打 `WARN`（重试）或 `ERROR`（耗尽）日志，包含错误类型、上次响应片段。
- AI 阶段把 prompt 与 response 摘要（截断 500 字）写入 `DEBUG` 日志，便于回溯。
- 用户输入与生成结果原文不写入日志，仅保留元数据，避免泄露。

#### 5.17.3 Trace

- 每个生成任务生成一个 `traceId`，贯穿 12 个阶段的所有日志。
- 阶段之间传递 `spanId`，按 `parentSpanId → spanId` 形成调用树。
- 第 11 阶段的回退路径要在 trace 中单独成 span，便于对比正常 vs 回退的耗时。

#### 5.17.4 告警阈值建议

| 告警 | 阈值 | 触发条件 |
|------|------|----------|
| 阶段失败率突增 | 单阶段失败率 > 20%（5 分钟滑动窗口） | 立即告警 |
| 任务平均耗时突增 | 较昨日同时段 P95 > 2 倍 | 立即告警 |
| LLM 调用 5xx 比例 | 单模板 > 10% | 立即告警 |
| 单任务成本异常 | 单任务 > ¥5 | 立即告警 |
| 模板被批量下线 | 5 分钟内下线 > 3 个模板 | 立即告警 |

#### 5.17.5 仪表盘建议

- **运营视角**：各模板近 7 天成功率、平均耗时、平均成本、字数一次达标率。
- **质量视角**：第 7 阶段毒舌评分分布、第 5 阶段问题类型分布。
- **成本视角**：按模板 × 阶段拆分的 token 消耗与金额。
- **运行视角**：实时任务队列长度、各阶段并发数、LLM 调用 QPS。

#### 5.17.6 关键约束

- 监控指标必须在阶段 1 就埋点，避免后续补埋点导致历史数据缺失。
- 日志和 trace 中不得包含用户输入原文与生成结果原文。
- 告警阈值上线后应持续观察 2 周，根据实际数据微调。

---

至此，去 AI 味文章生成流水线的设计已经覆盖：12 阶段方法论、配置模型、运行时、生命周期、前端线框、数据表与接口、实施路径与待讨论问题、监控可观测性。后续按阶段 1–5 落地，每个阶段配套单独的实施方案（见 `docs/superpowers/plans/`）。

