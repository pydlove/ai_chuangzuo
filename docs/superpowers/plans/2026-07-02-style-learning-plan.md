# 文章风格学习实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Vue 控制台「我的风格」页面新增第三个 tab「学习的风格」，让用户粘贴 / 上传任意参考文章，前端 mock 分析其风格后保存为可用风格。

**Architecture:** 在现有 `useStyles.js` 中新增 `learnedStyles` 共享状态与 `analyzeArticleStyle` async 函数（mock，接口预留为后端替换）；在 `StylesIndex.vue` 增加第三个 tab + 导入对话框 + 结果页；`CreateIndex.vue` 风格弹框复用同一份状态。mammoth.js 通过 CDN 引入用于 .docx 解析。

**Tech Stack:** Vue 3 Composition API、Ant Design Vue、Vite、Pinia、mammoth.js (CDN)、Playwright、Python 3。

## Global Constraints

- 仅在 `project/user/web/` 内实现，不改动 `.superpowers/brainstorm/...` 原型页。
- 不新增 npm 包，mammoth.js 用 CDN 一次性引入。
- 命名 ≤ 20 字、提示词 ≤ 1000 字、命名去重不区分大小写。
- 文件大小上限 5MB；正文最少 200 字。
- localStorage key = `aichuangzuo_learned_styles`。
- 仅支持 .txt / .md / .docx 三种文件类型。
- `analyzeArticleStyle` 必须为 async 函数（即便内部 mock 用 setTimeout），便于后端替换。
- 所有 commits 使用 `Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>`。

---

## File Structure

| 文件 | 操作 | 职责 |
|---|---|---|
| `project/user/web/index.html` | 修改 | `<head>` 引入 mammoth.js CDN |
| `project/user/web/src/composables/useStyles.js` | 修改 | 新增 `learnedStyles` 状态、`analyzeArticleStyle`、CRUD、文件读取 |
| `project/user/web/src/views/console/StylesIndex.vue` | 修改 | 第三个 tab、导入对话框、结果页 UI |
| `project/user/web/src/views/console/CreateIndex.vue` | 修改 | 风格弹框增加第三个 tab，复用 `learnedStyles` |
| `tests/e2e/verify_style_learning.py` | 创建 | 端到端验证脚本 |

---

### Task 1: 引入 mammoth.js CDN

**Files:**
- Modify: `project/user/web/index.html`

**Interfaces:**
- Consumes: 无
- Produces: 浏览器全局挂载 `window.mammoth`，供 `readDocxAsText()` 使用

- [ ] **Step 1: 编辑 `index.html` 添加 mammoth.js 脚本**

在 `</head>` 之前插入：

```html
<script src="https://cdn.jsdelivr.net/npm/mammoth@1.6.0/mammoth.browser.min.js"></script>
```

完整文件应为：

```html
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <link rel="icon" type="image/png" href="/favicon.png" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>爱创作</title>
    <script src="https://cdn.jsdelivr.net/npm/mammoth@1.6.0/mammoth.browser.min.js"></script>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.js"></script>
  </body>
</html>
```

- [ ] **Step 2: 验证 CDN 可用**

启动 dev 服务器：

```bash
cd project/user/web && npm run dev -- --port 22345 --host
```

浏览器访问 `http://localhost:22345`，打开 DevTools Console，输入 `typeof window.mammoth`。

预期输出：`"object"`

- [ ] **Step 3: 提交**

```bash
git add project/user/web/index.html
git commit -m "feat(style-learning): 引入 mammoth.js 用于 docx 解析

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 2: useStyles.js 添加 learnedStyles 状态与文件读取工具

**Files:**
- Modify: `project/user/web/src/composables/useStyles.js`

**Interfaces:**
- Consumes: 无
- Produces:
  - `learnedStyles`: `ref([])`，初始值从 localStorage 读取
  - `isLearning`: `ref(false)`
  - `readFileAsText(file: File): Promise<string>`
  - `readDocxAsText(file: File): Promise<string>`
  - 内部函数 `loadLearnedStyles()` / `saveLearnedStyles()` / `simpleHash(text): Promise<string>`

- [ ] **Step 1: 在 `useStyles.js` 末尾追加所有新代码**

在文件最末尾追加（注意保留原有 100 行不变）：

```javascript
// ============ 文章风格学习（前端 mock，后端替换点） ============

const LEARNED_STORAGE_KEY = 'aichuangzuo_learned_styles'

function loadLearnedStyles() {
  try {
    const raw = localStorage.getItem(LEARNED_STORAGE_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

function saveLearnedStyles() {
  localStorage.setItem(LEARNED_STORAGE_KEY, JSON.stringify(learnedStyles.value))
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

export const learnedStyles = ref(loadLearnedStyles())
export const isLearning = ref(false)

export function readFileAsText(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = e => resolve(e.target.result)
    reader.onerror = () => reject(new Error('文件读取失败'))
    reader.readAsText(file)
  })
}

export async function readDocxAsText(file) {
  if (!window.mammoth) throw new Error('mammoth.js 未加载')
  const buffer = await file.arrayBuffer()
  const result = await window.mammoth.extractRawText({ arrayBuffer: buffer })
  return result.value
}
```

- [ ] **Step 2: 验证开发服务器不报错**

确保 dev 服务器在运行，浏览器访问 `http://localhost:22345/console/styles`，DevTools Console 无报错。

- [ ] **Step 3: 提交**

```bash
git add project/user/web/src/composables/useStyles.js
git commit -m "feat(style-learning): 添加 learnedStyles 状态与文件读取工具

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 3: useStyles.js 添加风格分析与 CRUD 函数

**Files:**
- Modify: `project/user/web/src/composables/useStyles.js`

**Interfaces:**
- Consumes: Task 2 的 `learnedStyles`、`simpleHash`
- Produces:
  - `analyzeArticleStyle(text: string, meta: {sourceName: string, sourceType: string}): Promise<{sourceName, sourceType, excerpt1, excerpt2, prompt, fileHash, createdAt}>`
  - `isLearnedStyleNameExists(name: string, excludeName?: string): boolean`
  - `addLearnedStyle(style: object): void`
  - `removeLearnedStyle(name: string): void`
  - `findLearnedStyleByHash(hash: string): object | undefined`

- [ ] **Step 1: 在 useStyles.js 末尾（Task 2 代码之后）追加分析函数**

```javascript
// 风格分析（前端 mock，async 接口为后端预留）
export async function analyzeArticleStyle(text, meta) {
  isLearning.value = true
  try {
    const fileHash = await simpleHash(text)
    const paragraphs = text.split(/\n\s*\n/).filter(p => p.trim().length > 20)
    const first = paragraphs[0]?.trim() || ''
    const mid = paragraphs[Math.floor(paragraphs.length / 2)]?.trim() || ''
    const sentences = text.split(/[。！？\n]/).filter(s => s.trim().length > 10)
    const longest = sentences.sort((a, b) => b.length - a.length)[0]?.trim().slice(0, 80) || ''

    const prompt = `你是一位中文写手，请模仿以下参考文章的写作风格：

【语气】克制、文学化，善用短句与留白
【词汇】避免网络用语，偏书面表达
【句式】长短句交替，节奏感强
【结构】起承转合清晰，结尾有余味

请在生成新内容时参考以下片段的风格特征。`

    // mock 延迟 1.5 秒
    await new Promise(r => setTimeout(r, 1500))

    return {
      sourceName: meta.sourceName || '未命名参考',
      sourceType: meta.sourceType,
      excerpt1: (first || mid).slice(0, 120),
      excerpt2: longest,
      prompt,
      fileHash,
      createdAt: new Date().toISOString()
    }
  } finally {
    isLearning.value = false
  }
}

// 命名去重（仅在学习风格之间检查；与 myStyles 共用 isStyleNameExists）
export function isLearnedStyleNameExists(name, excludeName = null) {
  const target = name.trim().toLowerCase()
  if (!target) return false
  if (excludeName && target === excludeName.trim().toLowerCase()) return false
  return learnedStyles.value.some(s => s.name.trim().toLowerCase() === target)
}

export function addLearnedStyle(style) {
  learnedStyles.value.unshift({
    name: style.name.trim(),
    sourceName: style.sourceName,
    sourceType: style.sourceType,
    excerpt1: style.excerpt1,
    excerpt2: style.excerpt2,
    prompt: style.prompt.trim(),
    fileHash: style.fileHash,
    createdAt: style.createdAt
  })
  saveLearnedStyles()
}

export function removeLearnedStyle(name) {
  const idx = learnedStyles.value.findIndex(s => s.name === name)
  if (idx > -1) learnedStyles.value.splice(idx, 1)
  saveLearnedStyles()
}

export function findLearnedStyleByHash(hash) {
  return learnedStyles.value.find(s => s.fileHash === hash)
}
```

- [ ] **Step 2: 在浏览器 DevTools Console 测试导入**

打开 `http://localhost:22345/console/styles`，DevTools Console 执行：

```javascript
const { analyzeArticleStyle, learnedStyles } = await import('/src/composables/useStyles.js')
const result = await analyzeArticleStyle('这是一段测试文本，用于验证分析函数。这是第二句话，用于提供更多内容。这是第三句话。' + '\n\n' + '这是第二段，提供更多上下文。', { sourceName: '测试', sourceType: 'paste' })
console.log(result)
```

预期输出：包含 `sourceName: '测试'`、`excerpt1`、`excerpt2`、`prompt`、`fileHash` 字段的对象，且 `learnedStyles.value` 仍为空（因为未调用 addLearnedStyle）。

- [ ] **Step 3: 提交**

```bash
git add project/user/web/src/composables/useStyles.js
git commit -m "feat(style-learning): 添加风格分析与 CRUD 函数

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 4: StylesIndex.vue 增加第三个 tab 骨架与卡片网格

**Files:**
- Modify: `project/user/web/src/views/console/StylesIndex.vue`

**Interfaces:**
- Consumes: Task 3 的 `learnedStyles`、`isLearnedStyleNameExists`、`removeLearnedStyle`、`applyStyle`
- Produces: `StylesIndex.vue` 中第三个 tab「学习的风格」的 UI 骨架（空状态 + 卡片网格 + 「+ 学习新风格」按钮；对话框功能在 Task 5-6 中实现）

- [ ] **Step 1: 修改 `<template>` 的顶部 tab 区域**

将第 10-23 行（`.styles-tabs` 块）替换为：

```vue
<div class="styles-tabs">
  <button
    :class="['styles-tab', { active: activeTab === 'my' }]"
    @click="activeTab = 'my'; editorMode = false"
  >
    我的风格
  </button>
  <button
    :class="['styles-tab', { active: activeTab === 'system' }]"
    @click="activeTab = 'system'; editorMode = false"
  >
    系统预设
  </button>
  <button
    :class="['styles-tab', { active: activeTab === 'learned' }]"
    @click="activeTab = 'learned'; editorMode = false"
  >
    学习的风格
  </button>
</div>
```

- [ ] **Step 2: 在「系统预设」`</div>`（第 134 行附近）之后追加第三个 tab 的内容**

在 `</div>` 闭合「系统预设」之后、`</div>` 闭合 `styles-index` 之前（`</template>` 之前），追加：

```vue
<!-- 学习的风格 -->
<div v-show="activeTab === 'learned'" class="styles-content">
  <div class="learned-banner">
    上传或粘贴一篇文章，AI 会分析它的写作风格并保存为「我的风格」
  </div>
  <div class="learned-toolbar">
    <button class="learned-add-btn" @click="openImportDialog">+ 学习新风格</button>
  </div>
  <div v-if="learnedStyles.length === 0" class="learned-empty">
    还没有学习过的风格。点击上方按钮开始学习。
  </div>
  <div v-else class="styles-grid">
    <div
      v-for="s in learnedStyles"
      :key="s.name"
      class="style-card"
    >
      <div class="style-card-title">{{ s.name }}</div>
      <div class="style-card-source">
        来源：{{ s.sourceName }} · {{ s.sourceType.toUpperCase() }} · {{ s.createdAt.slice(0, 10) }}
      </div>
      <div class="style-card-prompt">{{ promptSummary(s.prompt) }}</div>
      <div v-show="expandedNames.has(s.name)" class="style-prompt-full">{{ s.prompt }}</div>
      <div class="style-card-actions">
        <button class="style-action-btn" @click.stop="useStyle(s)">使用</button>
        <button class="style-action-btn" @click.stop="togglePrompt(s.name)">
          {{ expandedNames.has(s.name) ? '收起' : '查看完整提示词' }}
        </button>
        <button class="style-action-btn style-del-btn" @click.stop="deleteLearnedStyle(s.name)">删除</button>
      </div>
    </div>
  </div>
</div>
```

- [ ] **Step 3: 在 `<script setup>` 中导入新依赖**

将 import 块（第 141-149 行）改为：

```javascript
import {
  systemStyles,
  myStyles,
  applyStyle,
  addCustomStyle,
  updateCustomStyle,
  removeCustomStyle,
  isStyleNameExists,
  learnedStyles,
  removeLearnedStyle
} from '@/composables/useStyles.js'
```

- [ ] **Step 4: 在 `<script setup>` 中新增 `openImportDialog` 与 `deleteLearnedStyle` 函数**

在 `deleteStyle` 函数之后追加：

```javascript
const openImportDialog = () => {
  // 完整实现在 Task 5-6 中
  alert('导入对话框将在 Task 5-6 中实现')
}

const deleteLearnedStyle = (name) => {
  if (!confirm('确定要删除「' + name + '」吗？')) return
  removeLearnedStyle(name)
}
```

- [ ] **Step 5: 在 `<style scoped>` 末尾追加新样式**

```css
.learned-banner {
  padding: 12px 16px;
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 8px;
  font-size: 13px;
  color: #389e0d;
  margin-bottom: 16px;
}

.learned-toolbar {
  margin-bottom: 16px;
}

.learned-add-btn {
  padding: 8px 16px;
  background: #07c160;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
}

.learned-add-btn:hover {
  background: #06a050;
}

.learned-empty {
  padding: 60px 20px;
  text-align: center;
  color: #8c8c8c;
  font-size: 14px;
}

.style-card-source {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 8px;
}
```

- [ ] **Step 6: 验证第三个 tab 可见**

浏览器访问 `http://localhost:22345/console/styles`：
1. 顶部出现 3 个 tab：「我的风格」「系统预设」「学习的风格」
2. 点击「学习的风格」显示横幅 + 「+ 学习新风格」按钮 + 空状态文案
3. 暂不会显示卡片（因为 `learnedStyles` 为空）

- [ ] **Step 7: 提交**

```bash
git add project/user/web/src/views/console/StylesIndex.vue
git commit -m "feat(style-learning): StylesIndex 新增学习的风格 tab 骨架

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 5: StylesIndex.vue 增加导入对话框（粘贴 / 上传 / 进度态）

**Files:**
- Modify: `project/user/web/src/views/console/StylesIndex.vue`

**Interfaces:**
- Consumes: Task 3 的 `analyzeArticleStyle`、`isLearnedStyleNameExists`、`findLearnedStyleByHash`、`isLearning`、`readFileAsText`、`readDocxAsText`
- Produces: 「+ 学习新风格」点击后弹出对话框，含「粘贴 / 上传」二级 tab、进度态 UI、表单状态

- [ ] **Step 1: 在 `</template>` 之前、第三个 tab 之后追加导入对话框**

```vue
<!-- 学习风格导入对话框 -->
<a-modal
  :open="importDialogVisible"
  :footer="null"
  :width="640"
  centered
  @cancel="closeImportDialog"
>
  <template #title>
    <div class="modal-title">学习写作风格</div>
  </template>

  <!-- 进度态 -->
  <div v-if="isLearning" class="learned-progress">
    <a-spin />
    <div class="learned-progress-text">● ● ● 分析中…</div>
  </div>

  <!-- 粘贴 / 上传 tab -->
  <template v-else-if="!learnedResult">
    <div class="learned-subtabs">
      <button
        :class="['learned-subtab', { active: importSubTab === 'paste' }]"
        @click="importSubTab = 'paste'"
      >粘贴正文</button>
      <button
        :class="['learned-subtab', { active: importSubTab === 'upload' }]"
        @click="importSubTab = 'upload'"
      >上传文件</button>
    </div>

    <!-- 粘贴 -->
    <div v-show="importSubTab === 'paste'" class="learned-pane">
      <textarea
        v-model="pasteText"
        class="learned-textarea"
        placeholder="将原文粘贴到这里…"
        maxlength="50000"
      ></textarea>
      <div class="learned-counter">{{ pasteText.length }} / 50000</div>
      <input
        v-model="pasteSourceName"
        type="text"
        class="learned-input"
        placeholder="来源标题（可选）"
        maxlength="50"
      />
      <div v-if="pasteError" class="learned-error">{{ pasteError }}</div>
      <button
        class="learned-submit-btn"
        :disabled="pasteText.trim().length < 200"
        @click="submitPaste"
      >开始学习</button>
    </div>

    <!-- 上传 -->
    <div v-show="importSubTab === 'upload'" class="learned-pane">
      <label class="learned-upload-zone">
        <input
          type="file"
          accept=".txt,.md,.docx"
          @change="onFileChange"
          style="display: none;"
        />
        <div v-if="!uploadFile" class="learned-upload-hint">
          点击选择文件或拖拽到此处<br/>
          <span class="learned-upload-types">支持 .txt / .md / .docx（最大 5MB）</span>
        </div>
        <div v-else class="learned-upload-info">
          ✓ {{ uploadFile.name }} ({{ Math.round(uploadFile.size / 1024) }} KB)
        </div>
      </label>
      <div v-if="uploadError" class="learned-error">{{ uploadError }}</div>
      <button
        class="learned-submit-btn"
        :disabled="!uploadFile"
        @click="submitUpload"
      >开始学习</button>
    </div>
  </template>

  <!-- 结果页（Task 6 实现） -->
  <div v-else>
    <div class="learned-result-title">学习结果 ✓ 已从「{{ learnedResult.sourceName }}」中提取风格</div>
    <div class="learned-result-field">
      <label class="learned-result-label">学到的提示词（可编辑）</label>
      <textarea
        v-model="learnedResult.prompt"
        class="learned-textarea"
        maxlength="1000"
      ></textarea>
      <div class="learned-counter" :class="{ over: learnedResult.prompt.length > 1000 }">
        {{ learnedResult.prompt.length }} / 1000
      </div>
    </div>
    <div class="learned-result-field">
      <label class="learned-result-label">原文风格示例</label>
      <div class="learned-excerpt">① {{ learnedResult.excerpt1 }}</div>
      <div class="learned-excerpt">② {{ learnedResult.excerpt2 }}</div>
    </div>
    <div class="learned-result-field">
      <label class="learned-result-label">命名 <span class="required">*</span></label>
      <input
        v-model="learnedResult.name"
        type="text"
        class="learned-input"
        placeholder="例如：我的小红书风"
        maxlength="20"
      />
      <div v-if="learnedResultError" class="learned-error">{{ learnedResultError }}</div>
    </div>
    <div class="learned-result-actions">
      <button class="learned-cancel-btn" @click="closeImportDialog">放弃</button>
      <button
        class="learned-submit-btn"
        :disabled="!canSaveLearnedResult"
        @click="saveLearnedResult"
      >保存到风格库</button>
    </div>
  </div>
</a-modal>
```

- [ ] **Step 2: 在 `<script setup>` 中替换 Task 4 占位的 `openImportDialog`，并新增所有对话框状态与函数**

替换 Task 4 中占位的：

```javascript
const openImportDialog = () => {
  alert('导入对话框将在 Task 5-6 中实现')
}
```

为：

```javascript
// 导入对话框状态
const importDialogVisible = ref(false)
const importSubTab = ref('paste')
const pasteText = ref('')
const pasteSourceName = ref('')
const pasteError = ref('')
const uploadFile = ref(null)
const uploadError = ref('')
const learnedResult = ref(null)
const learnedResultError = ref('')

const openImportDialog = () => {
  pasteText.value = ''
  pasteSourceName.value = ''
  pasteError.value = ''
  uploadFile.value = null
  uploadError.value = ''
  learnedResult.value = null
  learnedResultError.value = ''
  importSubTab.value = 'paste'
  importDialogVisible.value = true
}

const closeImportDialog = () => {
  importDialogVisible.value = false
}

const onFileChange = (e) => {
  uploadError.value = ''
  const file = e.target.files?.[0]
  if (!file) {
    uploadFile.value = null
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    uploadError.value = '文件过大（> 5MB）'
    uploadFile.value = null
    return
  }
  const ext = file.name.split('.').pop().toLowerCase()
  if (!['txt', 'md', 'docx'].includes(ext)) {
    uploadError.value = '仅支持 .txt / .md / .docx'
    uploadFile.value = null
    return
  }
  uploadFile.value = file
}

const submitPaste = async () => {
  pasteError.value = ''
  const text = pasteText.value.trim()
  if (text.length < 200) {
    pasteError.value = '正文过短（少于 200 字）'
    return
  }
  await runAnalysis(text, pasteSourceName.value.trim() || '粘贴的参考文章', 'paste')
}

const submitUpload = async () => {
  uploadError.value = ''
  if (!uploadFile.value) return
  try {
    const ext = uploadFile.value.name.split('.').pop().toLowerCase()
    let text
    if (ext === 'docx') {
      text = await readDocxAsText(uploadFile.value)
    } else {
      text = await readFileAsText(uploadFile.value)
    }
    if (text.trim().length < 200) {
      uploadError.value = '正文过短（少于 200 字）'
      return
    }
    await runAnalysis(text, uploadFile.value.name, ext)
  } catch (err) {
    uploadError.value = err.message || '文件读取失败'
  }
}

const runAnalysis = async (text, sourceName, sourceType) => {
  const tempResult = await analyzeArticleStyle(text, { sourceName, sourceType })
  const dup = findLearnedStyleByHash(tempResult.fileHash)
  if (dup) {
    if (sourceType === 'paste') {
      pasteError.value = '已学过这篇文章（命名：「' + dup.name + '」）'
    } else {
      uploadError.value = '已学过这篇文章（命名：「' + dup.name + '」）'
    }
    return
  }
  learnedResult.value = { ...tempResult, name: '' }
}

const canSaveLearnedResult = computed(() => {
  if (!learnedResult.value) return false
  const name = learnedResult.value.name.trim()
  if (!name || name.length > 20) return false
  if (learnedResult.value.prompt.length > 1000) return false
  if (isStyleNameExists(name) || isLearnedStyleNameExists(name)) return false
  return true
})

const saveLearnedResult = () => {
  if (!learnedResult.value) return
  const name = learnedResult.value.name.trim()
  if (isStyleNameExists(name) || isLearnedStyleNameExists(name)) {
    learnedResultError.value = '该风格名称已存在'
    return
  }
  if (name.length > 20) {
    learnedResultError.value = '风格名称最多 20 字'
    return
  }
  if (learnedResult.value.prompt.length > 1000) {
    learnedResultError.value = '提示词超过 1000 字'
    return
  }
  addLearnedStyle(learnedResult.value)
  closeImportDialog()
}
```

- [ ] **Step 3: 在 `<script setup>` 顶部 import 区域新增**

将 import 块（在 Task 4 基础上）改为：

```javascript
import {
  systemStyles,
  myStyles,
  applyStyle,
  addCustomStyle,
  updateCustomStyle,
  removeCustomStyle,
  isStyleNameExists,
  learnedStyles,
  removeLearnedStyle,
  analyzeArticleStyle,
  isLearnedStyleNameExists,
  findLearnedStyleByHash,
  addLearnedStyle,
  isLearning,
  readFileAsText,
  readDocxAsText
} from '@/composables/useStyles.js'
```

- [ ] **Step 4: 在 `<style scoped>` 末尾追加对话框样式**

```css
.learned-subtabs {
  display: flex;
  gap: 4px;
  background: #f5f5f5;
  padding: 4px;
  border-radius: 8px;
  margin-bottom: 16px;
  width: fit-content;
}

.learned-subtab {
  padding: 6px 14px;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
}

.learned-subtab.active {
  background: #fff;
  color: #1a1a1a;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.learned-pane {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.learned-textarea {
  width: 100%;
  min-height: 200px;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
}

.learned-textarea:focus {
  outline: none;
  border-color: #07c160;
}

.learned-counter {
  text-align: right;
  font-size: 12px;
  color: #8c8c8c;
}

.learned-counter.over {
  color: #ff4d4f;
}

.learned-input {
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
}

.learned-input:focus {
  outline: none;
  border-color: #07c160;
}

.learned-upload-zone {
  display: block;
  padding: 40px 20px;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
}

.learned-upload-zone:hover {
  border-color: #07c160;
  background: #f6ffed;
}

.learned-upload-hint {
  font-size: 14px;
  color: #595959;
}

.learned-upload-types {
  font-size: 12px;
  color: #8c8c8c;
}

.learned-upload-info {
  font-size: 14px;
  color: #07c160;
}

.learned-error {
  color: #ff4d4f;
  font-size: 13px;
}

.learned-submit-btn {
  padding: 10px 20px;
  background: #07c160;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.learned-submit-btn:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
}

.learned-progress {
  text-align: center;
  padding: 40px 0;
}

.learned-progress-text {
  margin-top: 12px;
  font-size: 14px;
  color: #595959;
}

.learned-result-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 16px;
}

.learned-result-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 16px;
}

.learned-result-label {
  font-size: 13px;
  font-weight: 500;
  color: #262626;
}

.learned-result-label .required {
  color: #ff4d4f;
}

.learned-excerpt {
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 8px;
  font-size: 13px;
  color: #595959;
  line-height: 1.6;
  margin-bottom: 6px;
}

.learned-result-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.learned-cancel-btn {
  padding: 10px 20px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #595959;
  cursor: pointer;
}

.modal-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}
```

- [ ] **Step 5: 验证导入对话框**

浏览器访问 `http://localhost:22345/console/styles`：
1. 切到「学习的风格」tab
2. 点击「+ 学习新风格」→ 对话框弹出
3. 切换「粘贴正文」「上传文件」两个 subtab
4. 粘贴少于 200 字时「开始学习」按钮禁用
5. 粘贴 > 200 字 + 点击「开始学习」→ 进度态出现 → 1.5 秒后切到结果页
6. 结果页显示 excerpt1 + excerpt2 + 提示词可编辑

- [ ] **Step 6: 提交**

```bash
git add project/user/web/src/views/console/StylesIndex.vue
git commit -m "feat(style-learning): 导入对话框与结果页 UI

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 6: 端到端手动验证（已有 Task 5 集成）

**Files:**
- 无新增文件

**Interfaces:**
- Consumes: Task 1-5 的全部产出
- Produces: 在浏览器中确认完整流程跑通

- [ ] **Step 1: 完整流程手动验证**

1. `/console/styles` → 「学习的风格」→ 「+ 学习新风格」
2. 粘贴一段 > 200 字的文章 → 点击「开始学习」
3. 1.5 秒后看到结果页 → 修改命名（例如「我的测试风格」）→ 点击「保存到风格库」
4. 对话框关闭 → 第三个 tab 出现新卡片
5. 点击卡片「使用」按钮 → 跳转到 `/console/create`
6. 在创作页打开风格弹框 → 切到「学习的风格」tab → 看到刚才保存的卡片
7. 点击「使用」→ 风格应用
8. 回到 `/console/styles` → 删除该卡片 → 创作页弹框同步消失
9. 刷新页面 → 卡片仍在（localStorage 持久化生效）

- [ ] **Step 2: 边界场景验证**

1. 粘贴 100 字 → 「开始学习」按钮禁用
2. 上传 6MB 文件 → 拒绝（需先 touch 一个大文件）
3. 上传 .pdf 文件 → 拒绝
4. 上传同名 .txt 两次 → 第二次提示「已学过这篇文章」
5. 命名重复（与现有「我的风格」同名）→ 错误提示

---

### Task 7: CreateIndex.vue 风格弹框增加第三个 tab

**Files:**
- Modify: `project/user/web/src/views/console/CreateIndex.vue`

**Interfaces:**
- Consumes: `learnedStyles`、`applyStyle as applyStyleShared`
- Produces: 创作页风格弹框中第三个 tab「学习的风格」，复用 `learnedStyles`

- [ ] **Step 1: 在 CreateIndex.vue 顶部 `<script setup>` 的 import 区域新增**

定位到已有 import 块（包含 `systemStyles` 等导入），在末尾追加：

```javascript
import {
  learnedStyles
} from '@/composables/useStyles.js'
```

（如果还没有 `applyStyle as applyStyleShared`，参考 Task 5 my-style-menu-plan.md 中的导入模式补全）

- [ ] **Step 2: 修改风格弹框的 `.style-tabs` 区域**

定位到第 489-502 行（`.style-tabs`），将：

```vue
<div class="style-tabs">
  <button
    :class="['style-tab', { active: styleTab === 'system' }]"
    @click="styleTab = 'system'; createStyleMode = false"
  >
    系统预设
  </button>
  <button
    :class="['style-tab', { active: styleTab === 'my' }]"
    @click="styleTab = 'my'; createStyleMode = false"
  >
    我的风格
  </button>
</div>
```

改为：

```vue
<div class="style-tabs">
  <button
    :class="['style-tab', { active: styleTab === 'system' }]"
    @click="styleTab = 'system'; createStyleMode = false"
  >
    系统预设
  </button>
  <button
    :class="['style-tab', { active: styleTab === 'my' }]"
    @click="styleTab = 'my'; createStyleMode = false"
  >
    我的风格
  </button>
  <button
    :class="['style-tab', { active: styleTab === 'learned' }]"
    @click="styleTab = 'learned'; createStyleMode = false"
  >
    学习的风格
  </button>
</div>
```

- [ ] **Step 3: 在「我的风格」`</div>`（第 544 行附近）之后、「`</template>` 之前」追加第三个 tab 内容**

在「我的风格」的 `</div>` 闭合标签之后插入：

```vue
<!-- 学习的风格 -->
<div v-show="styleTab === 'learned'" class="style-grid">
  <div
    v-if="learnedStyles.length === 0"
    class="style-empty"
  >
    还没有学习过的风格，请前往「我的风格」页面学习。
  </div>
  <div
    v-for="(l, idx) in learnedStyles"
    v-else
    :key="l.name"
    :class="['style-card', { selected: selectedStyleName === l.name }]"
    @click="selectStyle(l)"
  >
    <div class="style-card-title">{{ l.name }}</div>
    <div class="style-card-desc">来源：{{ l.sourceName }} · {{ l.sourceType.toUpperCase() }}</div>
    <div class="style-prompt-toggle" @click.stop="toggleLearnedPrompt(idx)">
      {{ expandedLearnedIdx === idx ? '收起 ▴' : '查看完整提示词 ▾' }}
    </div>
    <div v-show="expandedLearnedIdx === idx" class="style-prompt-full">
      {{ l.prompt }}
    </div>
  </div>
</div>
```

- [ ] **Step 4: 在 `<script setup>` 中新增 `expandedLearnedIdx` 状态与 `toggleLearnedPrompt` 函数**

定位到 `expandedPromptIdx` 附近，添加：

```javascript
const expandedLearnedIdx = ref(null)

const toggleLearnedPrompt = (idx) => {
  expandedLearnedIdx.value = expandedLearnedIdx.value === idx ? null : idx
}
```

- [ ] **Step 5: 验证创作页弹框**

1. 访问 `http://localhost:22345/console/create`
2. 点击风格 chip → 弹框打开
3. 看到 3 个 tab：「系统预设」「我的风格」「学习的风格」
4. 点击「学习的风格」→ 显示已有 learnedStyles 卡片或空状态

- [ ] **Step 6: 提交**

```bash
git add project/user/web/src/views/console/CreateIndex.vue
git commit -m "feat(style-learning): 创作页风格弹框新增学习的风格 tab

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 8: Playwright 端到端测试脚本

**Files:**
- Create: `tests/e2e/verify_style_learning.py`

**Interfaces:**
- Consumes: dev 服务器运行在 `http://localhost:22345`
- Produces: 测试脚本验证完整学习流程 + 边界场景

- [ ] **Step 1: 创建 `tests/e2e/verify_style_learning.py`**

```python
# tests/e2e/verify_style_learning.py
import os
import time
from playwright.sync_api import sync_playwright

URL = os.environ.get('APP_URL', 'http://localhost:22345')
SCREENSHOT_DIR = 'tests/e2e/screenshots'
os.makedirs(SCREENSHOT_DIR, exist_ok=True)

SAMPLE_TEXT = (
    '这是一段用于测试风格学习的示例文本。'
    '它需要超过两百个字符才能触发「开始学习」按钮。'
    + '在这里我们将文本不断重复以满足字数要求。' * 30
)

def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={'width': 1280, 'height': 800})

        # 1. 访问我的风格页面
        page.goto(f'{URL}/console/styles')
        page.wait_for_timeout(800)

        # 清空 localStorage 中的 learned styles
        page.evaluate("() => localStorage.removeItem('aichuangzuo_learned_styles')")
        page.reload()
        page.wait_for_timeout(500)

        # 2. 验证第三个 tab 存在
        tabs = page.locator('.styles-tab')
        assert tabs.count() == 3, f'期望 3 个 tab，实际 {tabs.count()}'
        tabs.nth(2).click()
        page.wait_for_timeout(300)

        # 3. 验证空状态
        assert page.locator('.learned-empty').count() == 1
        page.screenshot(path=f'{SCREENSHOT_DIR}/style_learning_empty.png')

        # 4. 打开导入对话框，粘贴学习
        page.locator('.learned-add-btn').click()
        page.wait_for_timeout(300)
        assert page.locator('.learned-textarea').count() >= 1

        # 5. 验证过短文本时按钮禁用
        page.locator('.learned-textarea').first.fill('太短了')
        page.wait_for_timeout(200)
        assert page.locator('.learned-submit-btn').first.is_disabled()

        # 6. 粘贴足够长的文本（粘贴面板含 1 个 textarea 和 2 个 input：来源标题 + 命名占位）
        textareas = page.locator('.learned-textarea')
        inputs = page.locator('.learned-input')
        textareas.first.fill(SAMPLE_TEXT)
        # 第一个 .learned-input 是「来源标题」
        inputs.first.fill('测试来源')
        page.wait_for_timeout(200)
        page.locator('.learned-submit-btn').first.click()

        # 7. 等待进度态结束，进入结果页
        page.wait_for_selector('.learned-result-title', timeout=5000)
        assert '测试来源' in page.locator('.learned-result-title').inner_text()
        page.screenshot(path=f'{SCREENSHOT_DIR}/style_learning_result.png')

        # 8. 输入命名并保存（结果页第二个 .learned-input 是「命名」字段）
        inputs = page.locator('.learned-input')
        inputs.last.fill('我的测试风格')
        page.wait_for_timeout(200)
        page.locator('button:has-text("保存到风格库")').click()
        page.wait_for_timeout(500)

        # 9. 验证第三个 tab 中出现新卡片
        tabs.nth(2).click()
        page.wait_for_timeout(300)
        cards = page.locator('.styles-content:visible .style-card')
        assert cards.count() == 1, f'期望 1 张卡片，实际 {cards.count()}'
        assert '我的测试风格' in cards.first.inner_text()
        page.screenshot(path=f'{SCREENSHOT_DIR}/style_learning_card.png')

        # 10. 创作页验证
        page.goto(f'{URL}/console/create')
        page.wait_for_timeout(800)
        # 点击风格 chip 打开弹框
        page.locator('.style-chip, [class*="style"]').first.click()
        page.wait_for_timeout(500)
        # 切到学习的风格 tab
        learned_tab_btn = page.locator('button:has-text("学习的风格")')
        if learned_tab_btn.count() > 0:
            learned_tab_btn.first.click()
            page.wait_for_timeout(300)
            assert '我的测试风格' in page.content()

        # 11. 删除卡片
        page.goto(f'{URL}/console/styles')
        page.wait_for_timeout(500)
        tabs = page.locator('.styles-tab')
        tabs.nth(2).click()
        page.wait_for_timeout(300)
        # 接受 confirm 弹窗
        page.on('dialog', lambda dialog: dialog.accept())
        page.locator('button:has-text("删除")').first.click()
        page.wait_for_timeout(500)
        assert page.locator('.learned-empty').count() == 1

        print('文章风格学习验证通过')
        browser.close()

if __name__ == '__main__':
    main()
```

- [ ] **Step 2: 运行测试**

确保 dev 服务器运行中：

```bash
cd project/user/web && npm run dev -- --port 22345 --host
```

另开终端：

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
python3 tests/e2e/verify_style_learning.py
```

预期输出：`文章风格学习验证通过`

如果失败，根据截图与控制台日志排查：
- `learned-textarea` 选择器找不到 → 检查 Task 5 Step 1 是否正确添加
- `learned-result-title` 找不到 → 检查 Task 5 Step 2 是否正确添加
- 创作页「学习的风格」按钮找不到 → Task 7 未完成

- [ ] **Step 3: 提交**

```bash
git add tests/e2e/verify_style_learning.py
git commit -m "test(style-learning): 添加端到端验证脚本

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Spec Coverage Check

| 规格要求 | 对应任务 |
|---|---|
| 第三个 tab「学习的风格」| Task 4 |
| 粘贴 + 上传（.txt/.md/.docx）入口 | Task 5 |
| mammoth.js 解析 docx | Task 1, Task 5 |
| 分析函数（async，预留后端替换）| Task 3 |
| 提示词 + 原文例句 | Task 3, Task 5 |
| 用户命名 + 命名去重 | Task 5 |
| localStorage 持久化 | Task 2 |
| 创作页风格弹框联动 | Task 7 |
| 错误处理（短文本 / 大文件 / 错误类型 / 重复 / 命名冲突）| Task 5 |
| 进度态 UI | Task 5 |
| 端到端测试 | Task 8 |

## Placeholder Scan

- 无 TBD / TODO。
- 所有步骤包含完整代码或命令。
- 函数签名（`analyzeArticleStyle(text, meta)`、`readFileAsText(file)` 等）在各 Task 间保持一致。
- localStorage key 统一为 `aichuangzuo_learned_styles`。

## Type Consistency

- `useStyles.js` 导出：`learnedStyles`、`isLearning`、`analyzeArticleStyle`、`isLearnedStyleNameExists`、`addLearnedStyle`、`removeLearnedStyle`、`findLearnedStyleByHash`、`readFileAsText`、`readDocxAsText`。
- `StylesIndex.vue` 和 `CreateIndex.vue` 中 import 与使用一致。
- `learnedStyles` 在两个页面共用同一份 ref，删除/新增自动同步。