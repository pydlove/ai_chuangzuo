# 风格学习 AI 分析设计

**日期**: 2026-07-16
**状态**: 已确认
**关联**: `2026-07-02-style-learning-design.md` 第 7 节（后端替换点）；`TitleOptimizeServiceImpl`（AI 调用与 prompt 模板范式）、`TitleOptimizeAiService`（同步模型调用器范式）

---

## 1. 功能概述

把「学习的风格」的前端 mock 分析替换为后端真实 AI 分析：用户粘贴/上传参考文章 → 后端调大模型拆解风格 → 返回可直接入库的风格提示词 + 2 段原文摘录 → 用户在结果页命名、填适用范围后保存（`sourceType=2`），之后创作页选用该风格生成文章。

**范围**：user/api 新增分析接口；user/web 仅替换 `analyzeArticleStyle` 函数体并删除 mock 代码。不改 `u_user_style` 表结构、不改保存链路、不改生成链路。

## 2. 方案选型

| 方案 | 做法 | 结论 |
|---|---|---|
| **A. 用户端同步接口（选定）** | `POST /api/v1/user/styles/analyze`，仿 `TitleOptimizeAiService`：读 `a_model_config` active 模型 → AES 解密 key → RestTemplate 同步调用 | 与标题优化完全同构，代码量小；3000 字分析约 10-30 秒，前端已有「分析中」进度态可承受 |
| B. 异步任务表 | 沿用 generation 任务表 + 前端轮询 | 对一次性短分析过重，YAGNI |
| C. 前端直连模型 | 浏览器直接调模型 API | 密钥暴露，不可行 |

## 3. 接口设计

```
POST /api/v1/user/styles/analyze
```

**请求**（`AnalyzeStyleRequest`）：

```json
{ "text": "参考文章正文" }
```

- `text`：`@NotBlank` + `@Size(min = 200, max = 3000)`，由 `@Valid` 全局校验拦截（前端按钮同样做 200-3000 字禁用，双重防线）。

**响应**（`Result<StyleAnalyzeVO>`）：

```json
{
  "excerpt1": "原文代表性片段（≤120字，逐字摘自原文）",
  "excerpt2": "原文代表性片段（≤80字，逐字摘自原文）",
  "prompt": "四段式风格提示词（≤1000字，可直接入库 u_user_style.prompt）"
}
```

- excerpt 仅用于结果页展示，**不入库**（与现状一致，不加表字段）。
- prompt 存库后由生成链路原样消费：`GenerationTaskService` 快照 `u_user_style.prompt` 注入任务 payload，无需二次加工。

**错误码**：`StyleErrorCode` 新增 `STYLE_ANALYZE_FAILED(112006, "风格分析失败，请重试")`。

## 4. 后端组件

### 4.1 组件清单

| 组件 | 职责 |
|---|---|
| `UserStyleController` | 新增 `POST /analyze` 端点，委托 `StyleAnalyzeService` |
| `StyleAnalyzeService` / `StyleAnalyzeServiceImpl`（新增） | 拼 prompt、调 AI、解析 JSON、校验与降级 |
| `StyleAnalyzeAiService`（新增） | 同步模型调用器，骨架照抄 `TitleOptimizeAiService`，差异仅 `temperature=0.3`（分析任务要稳定，标题优化是 0.8） |

复制而非抽象共用：项目已有此惯例（`TitleOptimizeAiService` 注释自述是"管理端 GenerationAiService 的精简版"），两个场景各自写死自己的温度与超时，不强行提炼通用 client。

### 4.2 解析、校验与降级（StyleAnalyzeServiceImpl）

1. `stripCodeFence` 兜底剥 markdown 围栏后解析 JSON（复制 `TitleOptimizeServiceImpl` 的防御逻辑）。
2. JSON 解析失败，或 `prompt` 字段缺少【语气】【词汇】【句式】【结构】任一标记，或 `prompt` 超过 1000 字 → 抛 `BusinessException(STYLE_ANALYZE_FAILED)`（与标题优化"解析失败即抛错"一致，由前端提示重试）。
3. `excerpt1` / `excerpt2` 必须被原文 `text.contains(...)` 逐字命中（防模型编造）；未命中时分别降级：`excerpt1` → 首个长度 > 20 字的段落截取 120 字，`excerpt2` → 最长句截取 80 字（mock 同款截取逻辑挪到后端），**不报错**。
4. 调用失败、超时（connect 10s / read 60s，同标题优化）→ `STYLE_ANALYZE_FAILED`。

### 4.3 权益门

v1 **不加**权益门。理由：风格学习是核心创作链路（学到的风格直接用于文章生成），不同于标题优化是增值点缀；且 mock 版对全员开放，上线 AI 分析不应收窄可用人群。如需商业化限制，后续单独评估加 benefit 编码。

## 5. 最终提示词

### system message

```text
你是一位资深的中文文体分析师，擅长拆解中文自媒体文章的写作风格，并把风格特征提炼成可直接指导 AI 写作的提示词。
```

### user message 模板（`%s` 为参考文章正文）

```text
请分析以下参考文章的写作风格，完成两件事：

【文章正文】
%s

【任务】
1. 从【语气】【词汇】【句式】【结构】四个维度拆解风格特征。每条特征必须具体、可模仿，禁止空泛形容（不要写"语言优美"，要写"多用15字以内短句，段间留白多"这类可执行描述）。
2. 从原文中逐字摘录 2 个最能代表该风格的片段。

【输出 JSON 结构】
{"excerpt1":"原文中最能代表风格的连续片段，不超过120字，必须逐字摘自原文","excerpt2":"另一个代表性片段，不超过80字，必须逐字摘自原文，且不与excerpt1重复","prompt":"不超过800字的风格提示词"}

其中 prompt 字段严格使用以下模板：
你是一位中文写手，请模仿以下参考文章的写作风格：

【语气】（人称视角、情感温度、与读者的距离感，1-2句）
【词汇】（书面/口语倾向、网络用语与语气词的使用习惯，1-2句）
【句式】（句子长短与节奏、标点习惯、常用修辞，1-2句）
【结构】（开头方式、段落组织、结尾处理，1-2句）

请在生成新内容时严格遵循以上风格特征。

最终输出要求（覆盖以上所有说明，必须严格遵守）：
  1. 只输出一个合法 JSON 对象。不要任何前言、说明、免责声明、思路解释、markdown 标题或后记。
  2. 不要用 ```json 或任何代码围栏包裹。
  3. 第一个字符必须是 {，最后一个字符必须是 }。
  4. 所有需要解释、标注、声明的信息，必须放进 JSON 字段里，不能写在 JSON 之外。
```

要点：
- `temperature=0.3`：分析与摘录要稳定，不要发挥。
- 摘录要求"逐字"：结果页展示的是「原文风格示例」，模型自编片段会很奇怪；服务端 `contains` 校验兜底。
- 结尾的「最终输出要求」四条规定与 `TitleOptimizeServiceImpl` 已落地版本逐字一致，是项目内所有 JSON 返回 prompt 的固定后缀。
- prompt 字段直接产出可入库成品，下游生成链路零改动。

## 6. 前端改动

- `src/api/style.js`：新增 `analyzeStyle(text)` → `POST /styles/analyze`。
- `src/composables/useStyles.js`：`analyzeArticleStyle` 函数体替换为调用 `analyzeStyle`，返回结构（`sourceType / excerpt1 / excerpt2 / prompt / scope / createdAt`）保持不变，调用方（导入对话框、结果页）零改动——正是旧 spec 第 7 节预留的替换方式。
- **删除 mock 代码**：段落抽取、最长句查找、固定四段模板、1.5 秒 `setTimeout` 延迟全部删掉（CLAUDE.md 清理原则：不用的代码开发结束后必须删掉）。`simpleHash` / `findLearnedStyleByHash` 已无调用方（后端保存不存 fileHash），一并删除。

## 7. 边界与错误处理

| 场景 | 处理 |
|---|---|
| text < 200 字或 > 3000 字 | `@Valid` 参数校验拦截（前端按钮禁用为第一道防线） |
| 模型调用失败 / 超时 | `STYLE_ANALYZE_FAILED`，前端提示「分析失败，请重试」 |
| 响应带 markdown 围栏 | `stripCodeFence` 剥掉再解析 |
| JSON 解析失败 / prompt 缺四标记 / prompt > 1000 字 | `STYLE_ANALYZE_FAILED` |
| excerpt 未逐字命中原文 | 该条降级为首段/中段截取，不报错 |
| 分析中重复点击 | 前端「开始学习」按钮 disabled（现状保留） |
| 未登录 / token 失效 | SecurityConfig 统一拦截（现状） |

## 8. 非目标

- 不加权益门（见 4.3）。
- 不改 `u_user_style` 表结构（excerpt 不入库）。
- 不改生成链路、标题优化链路。
- 不做分析结果缓存与原文去重（同文重复学习的 token 成本由用户行为自担，量小）。

## 9. 测试要点

1. **单测** `StyleAnalyzeServiceImplTest`（mock `StyleAnalyzeAiService.call`）：
   - 正常 JSON → 三字段返回
   - 带 ```json 围栏 → 剥围栏后正常解析
   - 非法 JSON → `STYLE_ANALYZE_FAILED`
   - prompt 缺【结构】标记 → `STYLE_ANALYZE_FAILED`
   - prompt 超过 1000 字 → `STYLE_ANALYZE_FAILED`
   - excerpt1 未命中原文 → 降级为首段截取且不报错
   - 空 excerpt → 降级
2. **手动 E2E**：粘贴 2000 字公众号文章 → 学习 → 结果页摘录确为原文片段 → 命名保存 → 创作页选该风格生成 → 成品文章风格可辨认。

## 10. 实现位置汇总

| 文件 | 操作 |
|---|---|
| `project/user/api/.../style/controller/UserStyleController.java` | 修改：新增 `POST /analyze` |
| `project/user/api/.../style/dto/request/AnalyzeStyleRequest.java` | 新增 |
| `project/user/api/.../style/vo/StyleAnalyzeVO.java` | 新增 |
| `project/user/api/.../style/service/StyleAnalyzeService.java` | 新增接口 |
| `project/user/api/.../style/service/impl/StyleAnalyzeServiceImpl.java` | 新增 |
| `project/user/api/.../style/service/StyleAnalyzeAiService.java` | 新增（仿 `TitleOptimizeAiService`） |
| `project/user/api/.../style/enums/StyleErrorCode.java` | 修改：+`STYLE_ANALYZE_FAILED(112006)` |
| `project/user/api/.../style/service/impl/StyleAnalyzeServiceImplTest.java`（test 目录） | 新增单测 |
| `project/user/web/src/api/style.js` | 修改：+`analyzeStyle` |
| `project/user/web/src/composables/useStyles.js` | 修改：替换 `analyzeArticleStyle`，删 mock 与 hash 死代码 |
