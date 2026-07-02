# 文章风格学习设计

**日期**: 2026-07-02
**状态**: 已确认，待实现
**关联文件**: `project/user/web/src/composables/useStyles.js`、`project/user/web/src/views/console/StylesIndex.vue`、`project/user/web/src/views/console/CreateIndex.vue`、`project/user/web/index.html`

---

## 1. 功能概述

在「我的风格」页面新增第三个 tab「学习的风格」。用户粘贴一篇参考文章（或上传 .txt / .md / .docx 文件），系统分析其写作风格后产出一段风格提示词与 2 个代表性片段；用户命名后保存到风格库，可在创作页一键选用。

**范围**：仅在 Vue 控制台项目（`project/user/web/`）内实现，不改动 HTML 原型（`.superpowers/brainstorm/...`）。

## 2. 设计决策

- **实现位置**：直接嵌入现有 `useStyles.js` 与 `StylesIndex.vue`，不新建页面、不引入 npm 包。
- **算法**：前端 mock，固定抽取首段 / 中段 / 最长句作为例句；提示词使用固定 4 段模板。**接口预留为 async**，未来替换为后端真实分析只需替换 `analyzeArticleStyle` 一个函数。
- **docx 解析**：通过一次性 CDN 引入 `mammoth.js`，放在 `index.html` 的 `<head>` 中。
- **持久化**：用 `localStorage`（key = `aichuangzuo_learned_styles`），刷新页面后保留。
- **分类**：在 `StylesIndex.vue` 增加第三个 tab「学习的风格」，与「我的风格」「系统预设」并列。
- **命名**：用户手填，必填，留空时保存按钮禁用，placeholder 提示「例如：我的小红书风」。
- **适用范围**：单个 input 手填，≤ 50 字，非空。卡片上以「适用：xxx」单行展示。
- **字数/大小限制**：粘贴正文最少 200 字、最多 3000 字；上传文件最大 1MB；仅支持 .txt / .md / .docx。
- **来源标题**：不采集；结果页标题通用化为「学习结果 ✓ 已从参考文章中提取风格」。
- **编辑**：已保存的学习风格可在「学习的风格」tab 点击「编辑」，修改名称、适用范围、提示词。
- **创作页集成**：在创作页风格弹框中增加第三个 tab，复用 `applyStyle` 共用同一份 `learnedStyles` 状态。

## 3. 数据模型

```js
// LearnedStyle：学到的风格
{
  name: '娱乐至死',                // 风格名称，必填，不超过 20 字，由用户手填
  sourceType: 'txt' | 'md' | 'docx' | 'paste',
  excerpt1: '原文片段 1（≤120 字）',
  excerpt2: '原文片段 2（≤80 字）',
  prompt: '四段式风格提示词（≤1000 字，可编辑）',
  scope: '公众号情感文 / 深度书评',  // 适用范围，用户手填，最长 50 字，必填
  fileHash: 'sha1 前 16 位',
  createdAt: '2026-07-02T...'
}
```

## 4. 架构与组件

### useStyles.js 改动

新增导出：

```js
// 状态
export const learnedStyles = ref(loadFromLocalStorage() || [])
export const isLearning = ref(false)

// 持久化
function saveLearnedStyles() {
  localStorage.setItem('aichuangzuo_learned_styles', JSON.stringify(learnedStyles.value))
}

function loadFromLocalStorage() {
  try {
    return JSON.parse(localStorage.getItem('aichuangzuo_learned_styles')) || []
  } catch { return [] }
}

// 核心分析函数（前端 mock，async 接口为后端预留）
export async function analyzeArticleStyle(text, meta) {
  // 1. SHA-1 hash 用于去重
  const fileHash = await simpleHash(text)
  // 2. 切段落
  const paragraphs = text.split(/\n\s*\n/).filter(p => p.trim().length > 20)
  // 3. 抽 3 个代表性片段
  const first = paragraphs[0]?.trim() || ''
  const mid   = paragraphs[Math.floor(paragraphs.length / 2)]?.trim() || ''
  // 4. 找最长句
  const sentences = text.split(/[。！？\n]/).filter(s => s.trim().length > 10)
  const longest = sentences.sort((a, b) => b.length - a.length)[0]?.trim().slice(0, 80) || ''
  // 5. 固定 4 段模板（演示用，未来由后端生成）
  const prompt = `你是一位中文写手，请模仿以下参考文章的写作风格：

【语气】克制、文学化，善用短句与留白
【词汇】避免网络用语，偏书面表达
【句式】长短句交替，节奏感强
【结构】起承转合清晰，结尾有余味

请在生成新内容时参考以下片段的风格特征。`
  // mock 延迟 1.5 秒（让 UI 有进度反馈）
  await new Promise(r => setTimeout(r, 1500))
  return {
    sourceType: meta.sourceType,
    excerpt1: (first || mid).slice(0, 120),
    excerpt2: longest,
    prompt,
    scope: '',     // 适用范围，由用户在结果页手填
    fileHash,
    createdAt: new Date().toISOString()
  }
}

// 命名去重（与 myStyles 共用）
export function isLearnedStyleNameExists(name, excludeName = null) {
  const target = name.trim().toLowerCase()
  if (!target) return false
  if (excludeName && target === excludeName.trim().toLowerCase()) return false
  return learnedStyles.value.some(s => s.name.trim().toLowerCase() === target)
}

// 新增 learned style
export function addLearnedStyle(style) {
  learnedStyles.value.unshift(style)
  saveLearnedStyles()
}

// 删除
export function removeLearnedStyle(name) {
  const idx = learnedStyles.value.findIndex(s => s.name === name)
  if (idx > -1) learnedStyles.value.splice(idx, 1)
  saveLearnedStyles()
}

// 编辑
export function updateLearnedStyle(oldName, style) {
  const idx = learnedStyles.value.findIndex(s => s.name === oldName)
  if (idx > -1) {
    const updated = {
      ...learnedStyles.value[idx],
      name: style.name.trim(),
      prompt: style.prompt.trim(),
      scope: (style.scope || '').trim()
    }
    learnedStyles.value[idx] = updated
    if (currentStyle.value && currentStyle.value.name === oldName) {
      currentStyle.value = updated
    }
    saveLearnedStyles()
  }
}

// 重复检测
export function findLearnedStyleByHash(hash) {
  return learnedStyles.value.find(s => s.fileHash === hash)
}

// 文件读取工具
export function readFileAsText(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = e => resolve(e.target.result)
    reader.onerror = reject
    reader.readAsText(file)
  })
}

export async function readDocxAsText(file) {
  if (!window.mammoth) throw new Error('mammoth.js 未加载')
  const buffer = await file.arrayBuffer()
  const result = await window.mammoth.extractRawText({ arrayBuffer: buffer })
  return result.value
}

async function simpleHash(text) {
  const sample = text.slice(0, 1000) + '|' + text.length
  const bytes = new TextEncoder().encode(sample)
  const buffer = await crypto.subtle.digest('SHA-1', bytes)
  const hex = Array.from(new Uint8Array(buffer))
    .map(b => b.toString(16).padStart(2, '0'))
    .join('')
  return hex.slice(0, 16)
}
```

### StylesIndex.vue 改动

- 顶部 tab 由两个改为三个：「我的风格」「系统预设」「学习的风格」
- 「学习的风格」tab 内容区：
  - 空状态：显示横幅 + 「+ 学习新风格」按钮
  - 非空状态：「+ 学习新风格」按钮 + 卡片网格
- 卡片复用现有 `.style-card` 样式，显示来源类型 + 日期 + 适用范围 + 「使用」「查看完整」「编辑」「删除」按钮
- 点击「编辑」打开同款结果表单，可修改名称、适用范围、提示词

### 导入对话框（新增组件，定义在 StylesIndex.vue 内）

模态对话框，二级 tab「粘贴正文 / 上传文件」：

**粘贴正文 tab**：
- 大文本域（maxLength = 3000，placeholder 提示粘贴完整文章）
- 字数统计：`{count} / 3000`
- 「开始学习」按钮（disabled when 文本 < 200 字 or > 3000 字）

**上传文件 tab**：
- 拖拽 / 点击文件选择区
- 已选择文件显示文件名 + 大小
- 「开始学习」按钮（disabled when 无文件）

**分析进度态**（点击「开始学习」后替换对话框主体）：
- 居中显示「● ● ● 分析中…」+ 加载圈
- 「开始学习」按钮 disabled 防重复

**学习结果页**（分析完成后展示，编辑时复用）：
- 标题：「学习结果 ✓ 已从参考文章中提取风格」（编辑模式标题为「编辑风格」）
- 「学到的提示词」textarea（可编辑，maxLength = 1000，字数统计）
- 「原文风格示例」展示 excerpt1 + excerpt2
- 「适用范围」输入框（必填，maxLength = 50）
- 「命名」输入框（maxLength = 20，命名重复校验，编辑时排除自身旧名）
- 「保存到风格库」「放弃」按钮

### CreateIndex.vue 改动

- 创作页风格弹框 tab 由两个改为三个，与 `StylesIndex.vue` 共用 `learnedStyles`（useStyles.js 中的共享状态）
- 卡片同样显示来源行 + 「使用」「查看完整」「删除」按钮
- 删除卡片在两个页面同步生效（同一份 ref）

## 5. 数据流

```
用户点击「+ 学习新风格」
  ↓
打开导入对话框
  ↓
用户粘贴文本 / 选择文件
  ↓
点击「开始学习」
  ↓
读取文件（FileReader / mammoth）→ 得到纯文本
  ↓
analyzeArticleStyle(text, meta)  ← 2 秒后返回结果
  ↓
显示学习结果页（用户可编辑提示词 / 修改命名）
  ↓
点击「保存到风格库」
  ↓
addLearnedStyle({ name, ...result }) → localStorage
  ↓
关闭对话框，第三个 tab 自动显示新卡片
```

## 6. 边界与错误处理

| 场景 | 处理 |
|---|---|
| 文本 < 200 字 | 「开始学习」按钮禁用，提示「正文过短」 |
| 文本 > 3000 字 | 「开始学习」按钮禁用，提示「正文过长」 |
| 文件 > 1MB | 拒绝，提示「文件过大（> 1MB）」 |
| 非 .txt / .md / .docx | 拒绝，提示「仅支持 .txt / .md / .docx」 |
| docx 解析失败 | 提示「Word 文档读取失败」 |
| 同一文章重复（fileHash 命中） | 提示「已学过这篇文章」，定位到原卡片 |
| 命名重复 | 提示「该风格名称已存在」，保存按钮禁用 |
| 命名 > 20 字 | maxlength 截断 |
| 提示词 > 1000 字 | 字数统计变红，保存禁用 |
| 分析中重复点击 | 「开始学习」按钮 disabled |
| 编辑时命名冲突 | 排除自身旧名后仍重复才报错 |

## 7. 后端替换点

将来后端实现风格分析时，只需替换 `useStyles.js` 中的 `analyzeArticleStyle` 函数：

```js
export async function analyzeArticleStyle(text, meta) {
  const res = await fetch('/api/styles/analyze', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ text, sourceName: meta.sourceName })
  })
  if (!res.ok) throw new Error('分析失败')
  return await res.json()
}
```

调用方（导入对话框、结果页）无需改动。

## 8. 测试要点

1. **粘贴学习**：粘贴 > 200 字文本 → 学习 → 命名 → 保存 → 卡片出现
2. **上传 txt / md / docx**：三种文件类型分别走通
3. **过短文本**：粘贴 100 字 → 按钮禁用
4. **大文件**：上传 > 1MB 文件 → 拒绝
5. **错误文件类型**：上传 .pdf → 拒绝
6. **重复学习**：粘贴相同文本两次 → 第二次提示已存在
7. **命名重复**：保存与现有风格同名 → 错误提示
8. **编辑学习风格**：修改名称 / 适用范围 / 提示词 → 卡片同步更新
9. **创作页联动**：在创作页风格弹框的第三个 tab 看到学到的风格，点击「使用」可应用
10. **刷新保留**：刷新页面后 learnedStyles 仍在
11. **删除**：第三个 tab 删除卡片后，创作页弹框同步消失

## 9. 实现位置汇总

| 文件 | 操作 |
|---|---|
| `project/user/web/index.html` | 修改：`<head>` 中增加 mammoth.js CDN 脚本 |
| `project/user/web/src/composables/useStyles.js` | 修改：增加 `learnedStyles` 状态、`analyzeArticleStyle` 等函数 |
| `project/user/web/src/views/console/StylesIndex.vue` | 修改：增加第三个 tab、导入对话框、结果页 |
| `project/user/web/src/views/console/CreateIndex.vue` | 修改：风格弹框增加第三个 tab，复用 `learnedStyles` |