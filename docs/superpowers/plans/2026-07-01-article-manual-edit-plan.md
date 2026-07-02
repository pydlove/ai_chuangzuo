# 控制台文章手动编辑实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Vue 控制台实现生成文章的手动编辑能力，包括预览页 inline 编辑和独立分块编辑页。

**Architecture:** 新增 `articleBlocks.js` 处理 body 文本与 block 数组互转；新增 `articleStorage.js` 统一读写当前文章并同步生成队列；新增 `EditIndex.vue` 作为 `/console/edit` 独立编辑页；扩展 `PreviewIndex.vue` 支持 inline 编辑态；注册新路由。

**Tech Stack:** Vue 3, Vue Router 4, Vite, Ant Design Vue, Playwright (Python)

## Global Constraints

- 保持项目纯前端、无后端、无构建系统变更。
- 新代码使用 Vue 3 Composition API (`<script setup>`)。
- 本地存储键名保持 `aichuangzuo_` 前缀。
- 品牌主色为 `#07c160`（绿色），控制台主按钮色为 `#ff2442`（红色）。
- 端到端测试使用 Playwright Python 脚本，运行在 Vite dev server (`http://127.0.0.1:5173/`)。

---

## File Structure

| 文件 | 责任 |
|------|------|
| `project/user/web/src/utils/articleBlocks.js` | 纯函数：把 `title + body` 解析成 block 数组，以及把 block 数组序列化回 `title + body`。 |
| `project/user/web/src/utils/articleStorage.js` | 读取/写入 `aichuangzuo_current_article`，并把修改同步回 `aichuangzuo_generation_queue` 对应条目。 |
| `project/user/web/src/views/console/EditIndex.vue` | `/console/edit` 独立分块编辑页，对齐原型 `edit.html`。 |
| `project/user/web/src/views/console/PreviewIndex.vue` | 扩展：增加编辑入口、inline 编辑态、保存/取消条。 |
| `project/user/web/src/router/index.js` | 注册 `/console/edit` 子路由。 |
| `tests/e2e/verify_console_content_editing.py` | 端到端测试：inline 编辑保存 + 独立编辑页保存 + 持久化验证。 |

---

### Task 1: 实现 articleBlocks.js（解析/序列化）

**Files:**
- Create: `project/user/web/src/utils/articleBlocks.js`

**Interfaces:**
- Produces: `parseBodyToBlocks(title, body)` → `{ type: string, html: string }[]`
- Produces: `serializeBlocksToArticle(blocks)` → `{ title: string, body: string }`

- [ ] **Step 1: 创建文件并写入解析/序列化函数**

```javascript
const BLOCK_TYPES = {
  TITLE: 'title',
  HEADING: 'heading',
  PARAGRAPH: 'paragraph',
  HIGHLIGHT: 'highlight',
  LIST_ITEM: 'list-item'
}

const TYPE_LABELS = {
  [BLOCK_TYPES.TITLE]: '标题',
  [BLOCK_TYPES.HEADING]: '小标题',
  [BLOCK_TYPES.PARAGRAPH]: '正文段落',
  [BLOCK_TYPES.HIGHLIGHT]: '重点高亮',
  [BLOCK_TYPES.LIST_ITEM]: '列表项'
}

export function getBlockTypeLabel(type) {
  return TYPE_LABELS[type] || '内容'
}

/**
 * 把文章标题和正文解析为可编辑 block 数组
 * @param {string} title
 * @param {string} body
 * @returns {{ type: string, html: string }[]}
 */
export function parseBodyToBlocks(title, body) {
  const blocks = []

  if (title || title === '') {
    blocks.push({ type: BLOCK_TYPES.TITLE, html: escapeHtml(title) })
  }

  if (!body) return blocks

  const lines = body.split('\n')
  let listBuffer = []

  const flushList = () => {
    if (listBuffer.length === 0) return
    listBuffer.forEach(item => {
      blocks.push({ type: BLOCK_TYPES.LIST_ITEM, html: escapeHtml(item) })
    })
    listBuffer = []
  }

  lines.forEach((line) => {
    const trimmed = line.trim()
    if (!trimmed) return

    const headingMatch = trimmed.match(/^【([^】]+)】$/)
    if (headingMatch) {
      flushList()
      blocks.push({ type: BLOCK_TYPES.HEADING, html: escapeHtml(headingMatch[1]) })
      return
    }

    if (trimmed.startsWith('> ')) {
      flushList()
      blocks.push({ type: BLOCK_TYPES.HIGHLIGHT, html: escapeHtml(trimmed.slice(2)) })
      return
    }

    const listMatch = trimmed.match(/^(?:[-•]|\d+\.)\s+(.*)$/)
    if (listMatch) {
      listBuffer.push(listMatch[1])
      return
    }

    flushList()
    blocks.push({ type: BLOCK_TYPES.PARAGRAPH, html: escapeHtml(trimmed) })
  })

  flushList()
  return blocks
}

/**
 * 把 block 数组序列化为标题和正文
 * @param {{ type: string, html: string }[]} blocks
 * @returns {{ title: string, body: string }}
 */
export function serializeBlocksToArticle(blocks) {
  const titleBlock = blocks.find(b => b.type === BLOCK_TYPES.TITLE)
  const title = titleBlock ? stripHtml(titleBlock.html).trim() : ''

  const bodyBlocks = blocks.filter(b => b.type !== BLOCK_TYPES.TITLE)
  const parts = []

  for (let i = 0; i < bodyBlocks.length; i++) {
    const block = bodyBlocks[i]
    const text = stripHtml(block.html).trim()
    if (!text) continue

    switch (block.type) {
      case BLOCK_TYPES.HEADING:
        parts.push(`【${text}】`)
        break
      case BLOCK_TYPES.HIGHLIGHT:
        parts.push(`> ${text}`)
        break
      case BLOCK_TYPES.LIST_ITEM:
        parts.push(`- ${text}`)
        break
      case BLOCK_TYPES.PARAGRAPH:
      default:
        parts.push(text)
    }
  }

  return { title, body: parts.join('\n\n') }
}

function escapeHtml(text) {
  if (text == null) return ''
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

function stripHtml(html) {
  if (html == null) return ''
  const tmp = document.createElement('div')
  tmp.innerHTML = html
  return tmp.textContent || tmp.innerText || ''
}

export { BLOCK_TYPES }
```

- [ ] **Step 2: 提交**

```bash
git add project/user/web/src/utils/articleBlocks.js
git commit -m "feat(utils): add article block parser/serializer"
```

---

### Task 2: 实现 articleStorage.js（读写与队列同步）

**Files:**
- Create: `project/user/web/src/utils/articleStorage.js`

**Interfaces:**
- Consumes: none
- Produces: `loadCurrentArticle()` → `object | null`
- Produces: `saveCurrentArticle(article)` → `boolean`
- Produces: `syncArticleToQueue(article)` → `boolean`

- [ ] **Step 1: 创建文件并写入存储函数**

```javascript
const CURRENT_ARTICLE_KEY = 'aichuangzuo_current_article'
const QUEUE_KEY = 'aichuangzuo_generation_queue'

export function loadCurrentArticle() {
  try {
    const raw = localStorage.getItem(CURRENT_ARTICLE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch (e) {
    console.error('load current article failed', e)
    return null
  }
}

export function saveCurrentArticle(article) {
  try {
    localStorage.setItem(CURRENT_ARTICLE_KEY, JSON.stringify(article))
    return true
  } catch (e) {
    console.error('save current article failed', e)
    return false
  }
}

export function syncArticleToQueue(article) {
  if (!article || !article.id) return false
  try {
    const raw = localStorage.getItem(QUEUE_KEY) || '[]'
    const queue = JSON.parse(raw)
    const idx = queue.findIndex(item => item.id === article.id)
    if (idx >= 0) {
      queue[idx].content = { title: article.title, body: article.body }
      queue[idx].title = article.title
      queue[idx].wordCount = estimateWordCount(article.body)
      localStorage.setItem(QUEUE_KEY, JSON.stringify(queue))
      return true
    }
    return false
  } catch (e) {
    console.error('sync article to queue failed', e)
    return false
  }
}

function estimateWordCount(body) {
  if (!body) return 0
  return String(body).replace(/\s/g, '').length
}
```

- [ ] **Step 2: 提交**

```bash
git add project/user/web/src/utils/articleStorage.js
git commit -m "feat(utils): add article storage helpers with queue sync"
```

---

### Task 3: 注册 /console/edit 路由

**Files:**
- Modify: `project/user/web/src/router/index.js`

**Interfaces:**
- Produces: route `/console/edit` → `EditIndex.vue`

- [ ] **Step 1: 在 console 子路由中新增 edit 路由**

在 `project/user/web/src/router/index.js` 的 `children` 数组中，在 `preview` 路由之前插入：

```javascript
      {
        path: 'edit',
        name: 'ConsoleEdit',
        component: () => import('@/views/console/EditIndex.vue')
      },
```

完整 `children` 数组应类似：

```javascript
    children: [
      {
        path: '',
        redirect: '/console/create'
      },
      {
        path: 'create',
        name: 'ConsoleCreate',
        component: () => import('@/views/console/CreateIndex.vue')
      },
      {
        path: 'queue',
        redirect: '/console/works'
      },
      {
        path: 'works',
        name: 'ConsoleWorks',
        component: () => import('@/views/console/WorksIndex.vue')
      },
      {
        path: 'styles',
        name: 'ConsoleStyles',
        component: () => import('@/views/console/StylesIndex.vue')
      },
      {
        path: 'edit',
        name: 'ConsoleEdit',
        component: () => import('@/views/console/EditIndex.vue')
      },
      {
        path: 'preview',
        name: 'ConsolePreview',
        component: () => import('@/views/console/PreviewIndex.vue')
      }
    ]
```

- [ ] **Step 2: 提交**

```bash
git add project/user/web/src/router/index.js
git commit -m "feat(router): register /console/edit route"
```

---

### Task 4: 实现 EditIndex.vue（独立分块编辑页）

**Files:**
- Create: `project/user/web/src/views/console/EditIndex.vue`

**Interfaces:**
- Consumes: `loadCurrentArticle`, `saveCurrentArticle`, `syncArticleToQueue` from `articleStorage.js`
- Consumes: `parseBodyToBlocks`, `serializeBlocksToArticle`, `getBlockTypeLabel`, `BLOCK_TYPES` from `articleBlocks.js`

- [ ] **Step 1: 创建编辑页组件**

```vue
<template>
  <div class="edit-index">
    <div class="edit-header">
      <button class="back-btn" @click="$router.push('/console/preview')">
        ← 返回预览
      </button>
      <h2 class="edit-title">编辑内容</h2>
    </div>

    <div v-if="!blocks.length" class="edit-empty">
      <a-empty description="暂无文章内容">
        <button class="empty-btn" @click="$router.push('/console/create')">去创作</button>
      </a-empty>
    </div>

    <div v-else class="edit-blocks">
      <div v-for="(block, idx) in blocks" :key="idx" class="edit-block">
        <div class="edit-block-label">{{ getBlockTypeLabel(block.type) }} #{{ idx + 1 }}</div>
        <div
          :class="['edit-block-area', block.type]"
          contenteditable="true"
          @paste="onPaste"
          @input="onInput(idx, $event)"
          v-html="block.html"
        />
      </div>
    </div>

    <div class="edit-actions">
      <button class="cancel" @click="cancel">取消</button>
      <button class="save" @click="save">保存修改</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { loadCurrentArticle, saveCurrentArticle, syncArticleToQueue } from '@/utils/articleStorage.js'
import { parseBodyToBlocks, serializeBlocksToArticle, getBlockTypeLabel } from '@/utils/articleBlocks.js'

const router = useRouter()
const blocks = ref([])
const originalArticle = ref(null)

onMounted(() => {
  const article = loadCurrentArticle()
  if (!article) return
  originalArticle.value = JSON.parse(JSON.stringify(article))
  blocks.value = parseBodyToBlocks(article.title, article.body)
})

const onPaste = (e) => {
  e.preventDefault()
  const text = (e.clipboardData || window.clipboardData).getData('text/plain')
  document.execCommand('insertText', false, text)
}

const onInput = (idx, e) => {
  blocks.value[idx].html = e.target.innerHTML
}

const save = () => {
  const titleBlock = blocks.value.find(b => b.type === 'title')
  if (!titleBlock || !stripHtml(titleBlock.html).trim()) {
    message.error('标题不能为空')
    return
  }

  const { title, body } = serializeBlocksToArticle(blocks.value)
  const article = {
    ...originalArticle.value,
    title,
    body
  }

  if (!saveCurrentArticle(article)) {
    message.error('保存失败，请检查浏览器存储权限')
    return
  }

  syncArticleToQueue(article)
  message.success('内容已保存')
  router.push('/console/preview')
}

const cancel = () => {
  router.push('/console/preview')
}

function stripHtml(html) {
  if (!html) return ''
  const tmp = document.createElement('div')
  tmp.innerHTML = html
  return tmp.textContent || tmp.innerText || ''
}
</script>

<style scoped>
.edit-index {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px;
}

.edit-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.back-btn {
  padding: 6px 12px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
}

.back-btn:hover {
  border-color: #ff2442;
  color: #ff2442;
}

.edit-title {
  font-size: 20px;
  margin: 0;
  color: #1a1a1a;
}

.edit-empty {
  padding: 60px 0;
}

.empty-btn {
  padding: 8px 20px;
  background: #ff2442;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
}

.edit-blocks {
  padding-bottom: 100px;
}

.edit-block {
  margin-bottom: 16px;
}

.edit-block-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 6px;
}

.edit-block-area {
  width: 100%;
  min-height: 60px;
  padding: 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 15px;
  line-height: 1.7;
  color: #262626;
  box-sizing: border-box;
  outline: none;
}

.edit-block-area:focus {
  border-color: #07c160;
  box-shadow: 0 0 0 2px rgba(7, 193, 96, 0.15);
}

.edit-block-area.title {
  font-size: 22px;
  font-weight: 700;
}

.edit-block-area.heading {
  font-size: 18px;
  font-weight: 600;
}

.edit-block-area.highlight {
  background: #f6ffed;
  border-left: 4px solid #07c160;
}

.edit-actions {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 12px;
  padding: 10px 16px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 28px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  z-index: 10003;
}

.edit-actions button {
  padding: 8px 18px;
  border-radius: 18px;
  border: none;
  font-size: 14px;
  cursor: pointer;
}

.edit-actions .cancel {
  background: #f5f5f5;
  color: #595959;
}

.edit-actions .save {
  background: #07c160;
  color: #fff;
}
</style>
```

- [ ] **Step 2: 提交**

```bash
git add project/user/web/src/views/console/EditIndex.vue
git commit -m "feat(console): add standalone article block editor"
```

---

### Task 5: 扩展 PreviewIndex.vue（inline 编辑）

**Files:**
- Modify: `project/user/web/src/views/console/PreviewIndex.vue`

**Interfaces:**
- Consumes: `loadCurrentArticle`, `saveCurrentArticle`, `syncArticleToQueue` from `articleStorage.js`
- Consumes: `parseBodyToBlocks`, `serializeBlocksToArticle`, `BLOCK_TYPES` from `articleBlocks.js`

- [ ] **Step 1: 在 `<template>` 头部增加“编辑正文”按钮**

在 `preview-header-actions` div 中，在“导出 Word”按钮之前插入：

```vue
        <button class="action-btn" @click="enterEditMode">
          编辑正文
        </button>
```

- [ ] **Step 2: 修改文章渲染区域，支持编辑态**

替换 `<div class="article-body" v-html="formattedBody"></div>` 为：

```vue
        <div v-if="!isEditing" class="article-body" v-html="formattedBody" @click="onBodyClick"></div>
        <div v-else class="article-body editing-body" @click="onBodyClick">
          <div
            v-for="(block, idx) in blocks"
            :key="idx"
            :class="['edit-block', block.type, { modified: modifiedIndices.has(idx) }]"
            :data-type="block.type"
            contenteditable="true"
            @paste="onPaste"
            @input="onBlockInput(idx, $event)"
            v-html="renderBlockHtml(block)"
          />
        </div>
```

- [ ] **Step 3: 在底部浮动操作栏位置增加编辑态保存条**

在 `floating-action-bar` 同级新增：

```vue
    <!-- 编辑态保存条 -->
    <div v-if="article && isEditing" class="edit-floating-bar">
      <span class="edit-hint">{{ modifiedIndices.size > 0 ? `已修改 ${modifiedIndices.size} 处` : '正在编辑' }}</span>
      <button class="float-btn" @click="cancelEdit">取消</button>
      <button class="float-btn primary" @click="saveEdit">保存修改</button>
    </div>
```

- [ ] **Step 4: 在 `<script setup>` 中引入依赖并新增编辑相关状态与函数**

在顶部引入：

```javascript
import { loadCurrentArticle, saveCurrentArticle, syncArticleToQueue } from '@/utils/articleStorage.js'
import { parseBodyToBlocks, serializeBlocksToArticle, BLOCK_TYPES } from '@/utils/articleBlocks.js'
```

在 `cardsModalVisible` 等状态之后新增：

```javascript
// 编辑态
const isEditing = ref(false)
const blocks = ref([])
const modifiedIndices = ref(new Set())
const articleSnapshot = ref(null)
```

在 `loadArticle` 函数后新增：

```javascript
const enterEditMode = () => {
  if (!article.value) return
  articleSnapshot.value = JSON.parse(JSON.stringify(article.value))
  blocks.value = parseBodyToBlocks(article.value.title, article.value.body)
  modifiedIndices.value = new Set()
  isEditing.value = true
}

const onBodyClick = (e) => {
  if (isEditing.value) return
  const target = e.target
  const editableTags = ['H1', 'H2', 'H3', 'P', 'LI', 'DIV']
  if (editableTags.includes(target.tagName) && target.closest('.article-body')) {
    enterEditMode()
    nextTick(() => {
      target.focus()
    })
  }
}

const onPaste = (e) => {
  e.preventDefault()
  const text = (e.clipboardData || window.clipboardData).getData('text/plain')
  document.execCommand('insertText', false, text)
}

const onBlockInput = (idx, e) => {
  blocks.value[idx].html = e.target.innerHTML
  modifiedIndices.value.add(idx)
}

const renderBlockHtml = (block) => {
  if (block.type === BLOCK_TYPES.TITLE) {
    return `<h1 style="font-size:24px;font-weight:700;margin-bottom:16px;line-height:1.4;color:#1a1a1a;">${block.html}</h1>`
  }
  if (block.type === BLOCK_TYPES.HEADING) {
    return `<h2 style="font-size:18px;font-weight:600;color:#1a1a1a;margin:24px 0 12px;">${block.html}</h2>`
  }
  if (block.type === BLOCK_TYPES.LIST_ITEM) {
    return `<li style="margin-bottom:8px;">${block.html}</li>`
  }
  if (block.type === BLOCK_TYPES.HIGHLIGHT) {
    return `<div style="background:#f6ffed;border-left:4px solid #07c160;padding:16px;margin:20px 0;border-radius:0 8px 8px 0;">${block.html}</div>`
  }
  return `<p style="margin-bottom:16px;">${block.html}</p>`
}

const saveEdit = () => {
  const titleBlock = blocks.value.find(b => b.type === BLOCK_TYPES.TITLE)
  if (!titleBlock || !stripHtml(titleBlock.html).trim()) {
    message.error('标题不能为空')
    return
  }

  const { title, body } = serializeBlocksToArticle(blocks.value)
  const updated = { ...article.value, title, body }

  if (!saveCurrentArticle(updated)) {
    message.error('保存失败，请检查浏览器存储权限')
    return
  }

  syncArticleToQueue(updated)
  article.value = updated
  isEditing.value = false
  modifiedIndices.value = new Set()
  message.success('内容已保存')
}

const cancelEdit = () => {
  if (articleSnapshot.value) {
    article.value = articleSnapshot.value
  }
  isEditing.value = false
  modifiedIndices.value = new Set()
  blocks.value = []
}

function stripHtml(html) {
  if (!html) return ''
  const tmp = document.createElement('div')
  tmp.innerHTML = html
  return tmp.textContent || tmp.innerText || ''
}
```

注意：`nextTick` 已经在顶部 `import { ref, computed, onMounted, watch, nextTick } from 'vue'` 中引入，无需重复添加。

- [ ] **Step 5: 调整浮动操作栏，编辑态隐藏**

将浮动操作栏改为：

```vue
    <div v-if="article && !isEditing" class="floating-action-bar">
```

- [ ] **Step 6: 添加编辑态样式**

在 `<style scoped>` 末尾追加：

```css
.editing-body .edit-block {
  outline: none;
  border: 1px solid transparent;
  border-radius: 6px;
  padding: 4px;
  transition: border-color 0.2s;
}

.editing-body .edit-block:focus,
.editing-body .edit-block:focus-within {
  border-color: #07c160;
  box-shadow: 0 0 0 2px rgba(7, 193, 96, 0.15);
}

.editing-body .edit-block.modified {
  background: #f6ffed;
}

.edit-floating-bar {
  position: fixed;
  bottom: 0;
  left: 200px;
  right: 0;
  background: #fff;
  border-top: 1px solid #eee;
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.06);
  padding: 10px 24px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  z-index: 100;
}

.edit-hint {
  font-size: 13px;
  color: #595959;
  margin-right: 8px;
}

@media (max-width: 768px) {
  .edit-floating-bar {
    left: 64px;
  }
}
```

- [ ] **Step 7: 提交**

```bash
git add project/user/web/src/views/console/PreviewIndex.vue
git commit -m "feat(console): add inline article editing on preview page"
```

---

### Task 6: 端到端测试

**Files:**
- Create: `tests/e2e/verify_console_content_editing.py`

**Interfaces:**
- Consumes: Vite dev server at `http://127.0.0.1:5173/`

- [ ] **Step 1: 启动 Vite dev server**

```bash
cd project/user/web && npm run dev
```

确认服务运行在 `http://127.0.0.1:5173/`。

- [ ] **Step 2: 创建测试脚本**

```python
from playwright.sync_api import sync_playwright

BASE = 'http://127.0.0.1:5173'


def test_console_content_editing():
    errors = []
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={'width': 1280, 'height': 900})

        def on_console(msg):
            if msg.type == 'error':
                errors.append(msg.text)
        page.on('console', on_console)

        article = {
            'id': 'test-edit-001',
            'title': '如何高效管理时间：从混乱到掌控的 5 个方法',
            'body': '时间对每个人来说都是公平的。\n\n【优先级排序】\n先做最重要的事。\n\n- 列出今日最重要的 3 件事\n- 先完成最难的那一件\n\n【时间块】\n给任务一个容器。\n\n> 关键结论：管理时间本质是管理注意力。',
            'completedAt': '2026-06-22',
            'wordCount': 1500,
            'style': '专业严谨'
        }

        queue = [{
            'id': 'test-edit-001',
            'status': 'completed',
            'title': article['title'],
            'platform': '微信公众号',
            'wordCount': article['wordCount'],
            'style': article['style'],
            'completedAt': article['completedAt'],
            'content': {'title': article['title'], 'body': article['body']}
        }]

        # Seed data
        page.goto(BASE + '/')
        page.wait_for_load_state('networkidle')
        page.evaluate(f"""() => {{
            localStorage.setItem('aichuangzuo_current_article', JSON.stringify({article}));
            localStorage.setItem('aichuangzuo_generation_queue', JSON.stringify({queue}));
        }""")

        # 1. Open preview
        page.goto(BASE + '/console/preview')
        page.wait_for_load_state('networkidle')
        page.wait_for_timeout(500)

        assert page.locator('.article-title:has-text("如何高效管理时间")').count() > 0, 'preview title not rendered'

        # 2. Inline edit
        page.click('button:has-text("编辑正文")')
        page.wait_for_timeout(300)
        assert page.locator('.edit-floating-bar').count() > 0, 'edit floating bar not shown'

        # 3. Modify title
        title_block = page.locator('.editing-body .edit-block.title').first
        title_block.click()
        title_block.fill('用户修改后的标题')
        page.wait_for_timeout(200)

        # 4. Save inline edit
        page.click('.edit-floating-bar button:has-text("保存修改")')
        page.wait_for_timeout(500)

        assert page.locator('.article-title:has-text("用户修改后的标题")').count() > 0, 'inline edited title not applied'

        # 5. Verify queue sync
        saved_queue = page.evaluate("""() => {
            try {
                return JSON.parse(localStorage.getItem('aichuangzuo_generation_queue') || '[]');
            } catch(e) { return []; }
        }""")
        assert any(item['title'] == '用户修改后的标题' for item in saved_queue), 'queue not synced after inline edit'

        # 6. Open standalone edit page
        page.goto(BASE + '/console/edit')
        page.wait_for_load_state('networkidle')
        page.wait_for_timeout(500)

        assert page.locator('.edit-block-area.title').count() > 0, 'edit page blocks not rendered'

        # 7. Modify title on edit page
        edit_title = page.locator('.edit-block-area.title').first
        edit_title.click()
        edit_title.fill('独立编辑页修改后的标题')
        page.wait_for_timeout(200)

        # 8. Save on edit page
        page.click('.edit-actions .save')
        page.wait_for_timeout(500)

        # 9. Verify back on preview
        assert page.url.endswith('/console/preview'), 'did not redirect to preview after edit page save'
        assert page.locator('.article-title:has-text("独立编辑页修改后的标题")').count() > 0, 'edit page save not reflected'

        page.screenshot(path='/tmp/verify_console_content_editing.png', full_page=True)

        browser.close()
        print('console content editing verification passed')
        if errors:
            print(f'console errors: {len(errors)}')
            for e in errors:
                print(f'  ERROR: {e}')


if __name__ == '__main__':
    test_console_content_editing()
```

- [ ] **Step 3: 运行测试**

```bash
python3 tests/e2e/verify_console_content_editing.py
```

预期输出：

```
console content editing verification passed
```

- [ ] **Step 4: 提交**

```bash
git add tests/e2e/verify_console_content_editing.py
git commit -m "test(e2e): add console content editing verification"
```

---

## Self-Review

**1. Spec coverage：**

- 独立编辑页 ✓ Task 4
- 预览页 inline 编辑 ✓ Task 5
- Block 解析/序列化 ✓ Task 1
- 持久化并同步生成队列 ✓ Task 2 + Task 5
- 粘贴纯文本 ✓ Task 4 / Task 5
- 标题非空校验 ✓ Task 4 / Task 5
- 端到端测试 ✓ Task 6

**2. Placeholder scan：** 无 TBD/TODO，所有步骤包含具体代码与命令。

**3. Type consistency：**
- `parseBodyToBlocks(title, body)` / `serializeBlocksToArticle(blocks)` 在 Task 1 与 Task 4/5 中一致。
- `loadCurrentArticle` / `saveCurrentArticle` / `syncArticleToQueue` 在 Task 2 与 Task 4/5 中一致。
- `BLOCK_TYPES` 常量贯穿 Task 1/4/5。

无冲突。
