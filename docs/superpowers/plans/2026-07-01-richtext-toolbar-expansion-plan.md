# 富文本工具栏扩展 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `/console/edit` 工具栏补齐行内文字样式与段落对齐操作,并把样式以 `styleOverrides` 字段独立持久化,刷新后仍可还原。

**Architecture:** 沿用 `contentEditable` + `document.execCommand` 方案,不动 `body` 纯文本语法,新增 `styleOverrides = { blocks, inlines }` 字段按 block 索引与字符区间记录样式。`articleBlocks.js` 增加 3 个新函数:`bodyToHtmlWithStyles`、`htmlToBodyWithStyles`、`applyStyleOverrides`。`EditIndex.vue` 工具栏扩展为三段式,光标处样式状态实时高亮。

**Tech Stack:** Vue 3 + Ant Design Vue 4.2 + Vite 5;Playwright(端到端验证)。

## Global Constraints

- 文章 `body` 字段保持纯文本语法(`【小标题】`、`> 引用`、`- 列表项`、`**加粗**`、`*斜体*`),不改变。
- 工具栏沿用 `contentEditable` + `document.execCommand`,不引入富文本库。
- `styleOverrides` 字段缺失或格式异常时降级为空对象 `{ blocks: {}, inlines: [] }`,保持向后兼容。
- `onPaste` 强制纯文本(`insertText`),不引入富文本粘贴。
- 颜色预设 8 色(黑 `#1a1a1a`、灰 `#595959`、红 `#ff2442`、橙 `#fa8c16`、黄 `#faad14`、绿 `#07c160`、蓝 `#1677ff`、紫 `#722ed1`)+ 自定义。
- 字号 5 档:`xs`(12px)、`sm`(14px)、`base`(15px)、`lg`(18px)、`xl`(22px)。
- 字体 4 档:`system`(系统默认)、`serif`(宋体)、`sans`(黑体)、`kai`(楷体)。
- 对齐 4 种:`left`、`center`、`right`、`justify`。
- 行高 3 档:`1.5`、`1.75`、`2.0`。
- 缩进 0/1/2 三档。
- 工具栏首屏宽 ≤ 1024px 时,行内样式多余项折叠到"更多"下拉菜单。
- 开发服务器端口 22345(Vite 配置);端到端测试用 `BASE = 'http://localhost:22345'`。
- Playwright 端到端验证脚本保存到 `tests/e2e/verify_richtext_toolbar.py`。

---

## File Structure

| 文件 | 职责 | 变更类型 |
|------|------|---------|
| `project/user/web/src/utils/articleBlocks.js` | 新增 3 个富文本辅助函数,内部使用现有 `bodyToHtml`/`htmlToBody` 解析逻辑 | Modify |
| `project/user/web/src/utils/articleStorage.js` | 读写时校验 `styleOverrides` 字段,缺失/异常则降级 | Modify |
| `project/user/web/src/views/console/EditIndex.vue` | 工具栏三段式扩展、按钮高亮、保存/加载 `styleOverrides` | Modify |
| `tests/e2e/verify_richtext_toolbar.py` | 端到端验证脚本:灌入文章、操作工具栏、保存、刷新、验证样式 | Create |

---

## Task 1: 扩展 `articleBlocks.js` 富文本辅助函数

**Files:**
- Modify: `project/user/web/src/utils/articleBlocks.js`
- Test: 在 `tests/e2e/verify_richtext_toolbar.py` 中间接覆盖(本任务内用 Node 脚本做基础自测)

**Interfaces:**
- Consumes: 现有 `bodyToHtml(body)`、`htmlToBody(html)`、`BLOCK_TYPES`
- Produces:
  ```js
  // 把纯文本 body 转成富文本 HTML,并按 styleOverrides 在对应位置贴样式
  // 返回的 HTML 可直接 v-html 到 contentEditable 编辑器
  bodyToHtmlWithStyles(body: string, styleOverrides?: { blocks: object, inlines: Array }) => string

  // 把 contentEditable 的 innerHTML 拆成纯文本 body + 样式覆盖
  // 用于保存
  htmlToBodyWithStyles(html: string) => { body: string, styleOverrides: { blocks: object, inlines: Array } }

  // 在已生成的 HTML 上叠加样式覆盖(给预览/导出复用,本任务可暂不实现,留空)
  applyStyleOverrides(html: string, styleOverrides: { blocks: object, inlines: Array }) => string
  ```

`styleOverrides` 数据结构(与规范一致):
```js
{
  blocks: { [blockIndex: number]: { align?, lineHeight?, indent?, fontSize?, fontFamily? } },
  inlines: Array<{ block: number, start: number, end: number, styles: { bold?, italic?, underline?, strike?, code?, color?, backgroundColor? } }>
}
```

- [ ] **Step 1: 在 `articleBlocks.js` 末尾新增空壳与导出**

在 `export { BLOCK_TYPES }` 这一行之后追加 3 个函数占位,确保模块仍能正常导出:

```js
export function bodyToHtmlWithStyles(body, styleOverrides) {
  // 详见后续步骤
  return bodyToHtml(body || '')
}

export function htmlToBodyWithStyles(html) {
  // 详见后续步骤
  const body = htmlToBody(html || '')
  return { body, styleOverrides: { blocks: {}, inlines: [] } }
}

export function applyStyleOverrides(html, styleOverrides) {
  // 详见后续步骤,本任务内先返回原 HTML
  return html || ''
}
```

- [ ] **Step 2: 用 Node 自测,确认占位实现不破坏现有调用**

Run:
```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/web && \
  node --input-type=module -e "
    import { bodyToHtmlWithStyles, htmlToBodyWithStyles, applyStyleOverrides } from './src/utils/articleBlocks.js';
    const html = bodyToHtmlWithStyles('hello\n\n【a】');
    const { body, styleOverrides } = htmlToBodyWithStyles('<p>hello</p>');
    console.log(JSON.stringify({ html, body, styleOverrides }));
  "
```
Expected: 输出 3 行 JSON,无报错(具体内容可变化)。这验证模块导入与基础函数调用不报错。

- [ ] **Step 3: 提交占位实现**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo && \
  git add project/user/web/src/utils/articleBlocks.js && \
  git commit -m "feat(articleBlocks): 新增富文本辅助函数占位"
```

- [ ] **Step 4: 实现 `htmlToBodyWithStyles` - 块级样式**

在 `htmlToBodyWithStyles` 函数体内(占位处)替换为:

```js
export function htmlToBodyWithStyles(html) {
  if (!html) return { body: '', styleOverrides: { blocks: {}, inlines: [] } }

  const tmp = document.createElement('div')
  tmp.innerHTML = html

  const blocks = {}
  const inlines = []
  const parts = []

  // 行内递归:把 DOM 节点序列化为字符流 + 行内样式区间
  const extractInline = (node, blockIdx, charOffsetRef) => {
    if (node.nodeType === Node.TEXT_NODE) {
      return node.textContent
    }
    if (node.nodeType !== Node.ELEMENT_NODE) {
      return ''
    }
    if (node.tagName === 'BR') {
      return '\n'
    }

    const tag = node.tagName.toLowerCase()
    // 行内标签样式映射
    const inlineStyleMap = {
      b: { bold: true }, strong: { bold: true },
      i: { italic: true }, em: { italic: true },
      u: { underline: true },
      s: { strike: true }, del: { strike: true }, strike: { strike: true },
      code: { code: true }
    }
    // span 上的 color / background-color / font-size / font-family
    let spanStyles = null
    if (tag === 'span' && node.getAttribute('style')) {
      const styleStr = node.getAttribute('style')
      spanStyles = {}
      const colorMatch = styleStr.match(/color\s*:\s*([^;]+)/i)
      if (colorMatch) spanStyles.color = colorMatch[1].trim()
      const bgMatch = styleStr.match(/background-color\s*:\s*([^;]+)/i)
      if (bgMatch) spanStyles.backgroundColor = bgMatch[1].trim()
      const sizeMatch = styleStr.match(/font-size\s*:\s*([^;]+)/i)
      if (sizeMatch) {
        const px = parseInt(sizeMatch[1], 10)
        spanStyles.fontSize = pxToFontSize(px)
      }
      const familyMatch = styleStr.match(/font-family\s*:\s*([^;]+)/i)
      if (familyMatch) spanStyles.fontFamily = familyMatch[1].trim()
      if (Object.keys(spanStyles).length === 0) spanStyles = null
    }

    const children = Array.from(node.childNodes)
    let buf = ''
    const childTexts = children.map(child => {
      const startOffset = charOffsetRef.value + buf.length
      const text = extractInline(child, blockIdx, { value: startOffset })
      buf += text
      return { startOffset, endOffset: startOffset + text.length, text, child }
    })
    const result = childTexts.map(c => c.text).join('')

    const baseStyles = inlineStyleMap[tag] || null
    const finalStyles = baseStyles || spanStyles
    if (finalStyles && result.length > 0) {
      const minStart = Math.min(...childTexts.map(c => c.startOffset))
      const maxEnd = Math.max(...childTexts.map(c => c.endOffset))
      inlines.push({
        block: blockIdx,
        start: minStart,
        end: maxEnd,
        styles: finalStyles
      })
    }
    return result
  }

  // 块级解析
  let blockIdx = 0
  Array.from(tmp.childNodes).forEach(node => {
    if (node.nodeType !== Node.ELEMENT_NODE) {
      if (node.nodeType === Node.TEXT_NODE && node.textContent.trim()) {
        const charOffsetRef = { value: 0 }
        const text = extractInline(node, blockIdx, charOffsetRef)
        parts.push(text.trim())
        blockIdx++
      }
      return
    }
    const tag = node.tagName.toLowerCase()
    const charOffsetRef = { value: 0 }
    const text = extractInline(node, blockIdx, charOffsetRef)
    const trimmed = text.trim()
    if (!trimmed) return

    // 块级样式
    const blockStyle = {}
    if (tag === 'h1' || tag === 'h2' || tag === 'h3' || tag === 'h4') {
      parts.push(`【${trimmed}】`)
    } else if (tag === 'blockquote') {
      parts.push(`> ${trimmed}`)
    } else if (tag === 'ul' || tag === 'ol') {
      Array.from(node.children).forEach(li => {
        if (li.tagName.toLowerCase() === 'li') {
          const liText = extractInline(li, blockIdx, { value: 0 }).trim()
          if (liText) parts.push(`- ${liText}`)
        }
      })
      // 列表的整块 align/lineHeight 也归到当前 blockIdx
      collectBlockStyle(node, blockStyle)
      if (Object.keys(blockStyle).length > 0) blocks[blockIdx] = blockStyle
    } else {
      parts.push(trimmed)
    }
    if (tag !== 'ul' && tag !== 'ol') {
      collectBlockStyle(node, blockStyle)
      if (Object.keys(blockStyle).length > 0) blocks[blockIdx] = blockStyle
    }
    blockIdx++
  })

  return {
    body: parts.join('\n\n'),
    styleOverrides: { blocks, inlines }
  }
}

function collectBlockStyle(el, target) {
  const align = el.getAttribute && el.getAttribute('align')
  if (align) target.align = align
  const styleStr = el.getAttribute && el.getAttribute('style')
  if (!styleStr) return
  const textAlignMatch = styleStr.match(/text-align\s*:\s*([^;]+)/i)
  if (textAlignMatch) target.align = textAlignMatch[1].trim()
  const lineHeightMatch = styleStr.match(/line-height\s*:\s*([^;]+)/i)
  if (lineHeightMatch) target.lineHeight = lineHeightMatch[1].trim()
  const indentMatch = styleStr.match(/padding-left\s*:\s*([^;]+)/i)
  if (indentMatch) {
    const px = parseInt(indentMatch[1], 10)
    target.indent = Math.round(px / 24)
  }
  const sizeMatch = styleStr.match(/font-size\s*:\s*([^;]+)/i)
  if (sizeMatch) {
    const px = parseInt(sizeMatch[1], 10)
    target.fontSize = pxToFontSize(px)
  }
  const familyMatch = styleStr.match(/font-family\s*:\s*([^;]+)/i)
  if (familyMatch) target.fontFamily = familyMatch[1].trim()
}

function pxToFontSize(px) {
  if (px <= 12) return 'xs'
  if (px <= 14) return 'sm'
  if (px <= 15) return 'base'
  if (px <= 18) return 'lg'
  return 'xl'
}

function fontSizeToPx(size) {
  return { xs: 12, sm: 14, base: 15, lg: 18, xl: 22 }[size] || 15
}
```

注意:把 `fontSizeToPx` 单独导出,后续 Task 2 中 `bodyToHtmlWithStyles` 会用到。

- [ ] **Step 5: 实现 `bodyToHtmlWithStyles`**

在 `bodyToHtmlWithStyles` 函数体内替换为:

```js
export function bodyToHtmlWithStyles(body, styleOverrides) {
  const baseHtml = bodyToHtml(body || '')
  if (!styleOverrides || (!styleOverrides.blocks && !styleOverrides.inlines)) {
    return baseHtml
  }
  return applyStyleOverrides(baseHtml, styleOverrides)
}
```

- [ ] **Step 6: 实现 `applyStyleOverrides`**

替换 `applyStyleOverrides` 占位为:

```js
export function applyStyleOverrides(html, styleOverrides) {
  if (!html || !styleOverrides) return html || ''
  const wrap = document.createElement('div')
  wrap.innerHTML = html

  const blockEls = Array.from(wrap.children)
  if (blockEls.length === 0) return html

  // 块级样式
  if (styleOverrides.blocks) {
    Object.entries(styleOverrides.blocks).forEach(([idx, style]) => {
      const el = blockEls[parseInt(idx, 10)]
      if (!el) return
      applyBlockStyle(el, style)
    })
  }

  // 行内样式:按 blockIndex 应用到该 block 的所有文本节点
  if (Array.isArray(styleOverrides.inlines) && styleOverrides.inlines.length > 0) {
    const byBlock = {}
    styleOverrides.inlines.forEach(inline => {
      if (!byBlock[inline.block]) byBlock[inline.block] = []
      byBlock[inline.block].push(inline)
    })
    Object.entries(byBlock).forEach(([idx, list]) => {
      const el = blockEls[parseInt(idx, 10)]
      if (!el) return
      applyInlineStyle(el, list)
    })
  }

  return wrap.innerHTML
}

function applyBlockStyle(el, style) {
  if (style.align) {
    el.style.textAlign = style.align
    el.setAttribute('align', style.align)
  }
  if (style.lineHeight) {
    el.style.lineHeight = String(style.lineHeight)
  }
  if (typeof style.indent === 'number' && style.indent > 0) {
    el.style.paddingLeft = `${style.indent * 24}px`
  }
  if (style.fontSize) {
    el.style.fontSize = `${fontSizeToPx(style.fontSize)}px`
  }
  if (style.fontFamily) {
    el.style.fontFamily = style.fontFamily
  }
}

function applyInlineStyle(blockEl, inlineList) {
  // 取出 block 的纯文本,逐字符扫描,定位每个 inline 区间并包 span
  const text = blockEl.textContent
  if (!text) return
  // 合并/裁剪区间到 [0, text.length] 内
  const ranges = inlineList
    .map(i => ({ start: Math.max(0, i.start), end: Math.min(text.length, i.end), styles: i.styles }))
    .filter(r => r.end > r.start)
    .sort((a, b) => a.start - b.start)
  if (ranges.length === 0) return
  // 简单分片:按 ranges 切割,每个分片包一个 span
  const points = new Set([0, text.length])
  ranges.forEach(r => { points.add(r.start); points.add(r.end) })
  const sortedPoints = Array.from(points).sort((a, b) => a - b)
  const segments = []
  for (let i = 0; i < sortedPoints.length - 1; i++) {
    const s = sortedPoints[i], e = sortedPoints[i + 1]
    if (e <= s) continue
    const segmentStyles = ranges
      .filter(r => r.start <= s && r.end >= e)
      .reduce((acc, r) => Object.assign(acc, r.styles), {})
    segments.push({ text: text.slice(s, e), styles: segmentStyles })
  }
  blockEl.innerHTML = segments.map(seg => wrapInline(seg.text, seg.styles)).join('')
}

function wrapInline(text, styles) {
  if (Object.keys(styles).length === 0) return escapeHtml(text)
  const styleStr = buildStyleString(styles)
  if (styles.bold) text = `<strong>${text}</strong>`
  if (styles.italic) text = `<em>${text}</em>`
  if (styles.underline) text = `<u>${text}</u>`
  if (styles.strike) text = `<s>${text}</s>`
  if (styles.code) text = `<code>${text}</code>`
  if (styleStr) text = `<span style="${styleStr}">${text}</span>`
  return text
}

function buildStyleString(styles) {
  const parts = []
  if (styles.color) parts.push(`color: ${styles.color}`)
  if (styles.backgroundColor) parts.push(`background-color: ${styles.backgroundColor}`)
  return parts.join('; ')
}

function escapeHtml(text) {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}
```

- [ ] **Step 7: 用 Node 自测往返一致性**

Run:
```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/web && \
  node --input-type=module -e "
    import { JSDOM } from 'jsdom';
    const dom = new JSDOM('<!DOCTYPE html><html><body></body></html>');
    global.document = dom.window.document;
    global.Node = dom.window.Node;
    const { bodyToHtmlWithStyles, htmlToBodyWithStyles } = await import('./src/utils/articleBlocks.js');
    const body = '【小标题】\n正文段落';
    const overrides = { blocks: { 0: { align: 'center' } }, inlines: [{ block: 1, start: 0, end: 2, styles: { bold: true, color: '#ff2442' } }] };
    const html = bodyToHtmlWithStyles(body, overrides);
    const result = htmlToBodyWithStyles(html);
    console.log('original body:', JSON.stringify(body));
    console.log('round-trip body:', JSON.stringify(result.body));
    console.log('blocks:', JSON.stringify(result.styleOverrides.blocks));
    console.log('inlines:', JSON.stringify(result.styleOverrides.inlines));
  " 2>&1 | head -40
```

Expected: `round-trip body` 与 `original body` 完全一致;`blocks` 中存在 `0.align = 'center'`;`inlines` 至少包含一个 bold/颜色样式。

如果 `jsdom` 未安装,临时安装:
```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/web && \
  npm install --no-save --no-audit --silent jsdom
```

- [ ] **Step 8: 提交完整实现**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo && \
  git add project/user/web/src/utils/articleBlocks.js && \
  git commit -m "feat(articleBlocks): 实现 bodyToHtmlWithStyles/htmlToBodyWithStyles/applyStyleOverrides"
```

---

## Task 2: 适配 `articleStorage.js` 兼容 `styleOverrides`

**Files:**
- Modify: `project/user/web/src/utils/articleStorage.js`

**Interfaces:**
- Consumes: 现有 `loadCurrentArticle` / `saveCurrentArticle` / `syncArticleToQueue`
- Produces: 同样的函数签名,但 `loadCurrentArticle` 读到的 article 中 `styleOverrides` 字段被规范化为 `{ blocks: {}, inlines: [] }`(缺失或异常时降级)

- [ ] **Step 1: 新增规范化函数**

在 `articleStorage.js` 顶部 `CURRENT_ARTICLE_KEY` 之后新增:

```js
function normalizeStyleOverrides(value) {
  const empty = { blocks: {}, inlines: [] }
  if (!value || typeof value !== 'object') return empty
  const blocks = (value.blocks && typeof value.blocks === 'object') ? value.blocks : {}
  const inlines = Array.isArray(value.inlines) ? value.inlines.filter(i =>
    i && typeof i.block === 'number' && typeof i.start === 'number' && typeof i.end === 'number' && i.styles && typeof i.styles === 'object'
  ) : []
  return { blocks, inlines }
}
```

- [ ] **Step 2: 在 `loadCurrentArticle` 中规范化**

替换 `loadCurrentArticle` 函数为:

```js
export function loadCurrentArticle() {
  try {
    const raw = localStorage.getItem(CURRENT_ARTICLE_KEY)
    if (!raw) return null
    const article = JSON.parse(raw)
    if (article && typeof article === 'object') {
      article.styleOverrides = normalizeStyleOverrides(article.styleOverrides)
    }
    return article
  } catch (e) {
    console.error('load current article failed', e)
    return null
  }
}
```

- [ ] **Step 3: 在 `saveCurrentArticle` 中规范化**

替换 `saveCurrentArticle` 函数为:

```js
export function saveCurrentArticle(article) {
  try {
    const safe = {
      ...article,
      styleOverrides: normalizeStyleOverrides(article && article.styleOverrides)
    }
    localStorage.setItem(CURRENT_ARTICLE_KEY, JSON.stringify(safe))
    return true
  } catch (e) {
    console.error('save current article failed', e)
    return false
  }
}
```

- [ ] **Step 4: 提交**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo && \
  git add project/user/web/src/utils/articleStorage.js && \
  git commit -m "feat(articleStorage): 兼容 styleOverrides 字段读写"
```

---

## Task 3: 重构 `EditIndex.vue` 工具栏 UI

**Files:**
- Modify: `project/user/web/src/views/console/EditIndex.vue`

**Interfaces:**
- Consumes: `bodyToHtmlWithStyles`、`htmlToBodyWithStyles`、`loadCurrentArticle`、`saveCurrentArticle`、`syncArticleToQueue`
- Produces: `<styleOverrides>` 数据(内部维护在 `styleOverridesRef` 响应式变量),并通过 `a-button-group` / `a-dropdown` / `a-color-picker` 暴露工具栏 UI

> 本任务先做 UI 骨架与状态接线,真正写回 `styleOverrides` 的逻辑由 Task 4 完成。

- [ ] **Step 1: 引入辅助函数与新增响应式状态**

替换 `<script setup>` 顶部 `import` 与状态声明为:

```js
import { ref, onMounted, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { loadCurrentArticle, saveCurrentArticle, syncArticleToQueue } from '@/utils/articleStorage.js'
import { bodyToHtmlWithStyles, htmlToBodyWithStyles } from '@/utils/articleBlocks.js'

const router = useRouter()
const originalArticle = ref(null)
const title = ref('')
const editorRef = ref(null)
const styleOverridesRef = ref({ blocks: {}, inlines: [] })
const activeFormats = reactive({
  bold: false, italic: false, underline: false, strike: false, code: false,
  align: '', lineHeight: '', indent: '', fontSize: '', fontFamily: '',
  color: '', backgroundColor: ''
})
const toolbarTab = ref('inline') // inline | paragraph | list
const collapsed = ref(false) // 1024px 以下折叠
```

- [ ] **Step 2: 改写 `onMounted` 使用富文本渲染**

替换 `onMounted` 为:

```js
onMounted(() => {
  const article = loadCurrentArticle()
  if (!article) return
  originalArticle.value = JSON.parse(JSON.stringify(article))
  title.value = article.title || ''
  styleOverridesRef.value = article.styleOverrides || { blocks: {}, inlines: [] }
  // 等下一帧让 editorRef 渲染
  setTimeout(() => {
    if (editorRef.value) {
      editorRef.value.innerHTML = bodyToHtmlWithStyles(article.body, styleOverridesRef.value)
    }
  }, 0)
  window.addEventListener('resize', onResize)
  onResize()
})

const onResize = () => {
  collapsed.value = window.innerWidth <= 1024
}
```

并在 `import` 后追加 `import { onUnmounted } from 'vue'`,在 `onMounted` 后追加:

```js
onUnmounted(() => {
  window.removeEventListener('resize', onResize)
})
```

- [ ] **Step 3: 改写工具栏模板(在 `.edit-toolbar` 内)**

把现有 `<div class="edit-toolbar">...</div>` 整段替换为:

```html
<div class="edit-toolbar">
  <a-button-group size="small">
    <a-button :type="activeFormats.bold ? 'primary' : 'default'" @click="applyInline('bold')" title="加粗"><strong>B</strong></a-button>
    <a-button :type="activeFormats.italic ? 'primary' : 'default'" @click="applyInline('italic')" title="斜体"><em>I</em></a-button>
    <a-button v-if="!collapsed" :type="activeFormats.underline ? 'primary' : 'default'" @click="applyInline('underline')" title="下划线"><u>U</u></a-button>
    <a-button v-if="!collapsed" :type="activeFormats.strike ? 'primary' : 'default'" @click="applyInline('strike')" title="删除线"><s>S</s></a-button>
    <a-button v-if="!collapsed" :type="activeFormats.code ? 'primary' : 'default'" @click="applyInline('code')" title="行内代码">{}</a-button>
  </a-button-group>

  <a-button-group v-if="!collapsed" size="small" class="toolbar-group">
    <a-dropdown trigger="click">
      <a-button :type="activeFormats.color ? 'primary' : 'default'">字体色</a-button>
      <template #overlay>
        <div class="color-picker-panel">
          <div v-for="c in presetColors" :key="c" class="color-swatch" :style="{ background: c }" @click="applyColor('color', c)"></div>
          <input type="color" :value="activeFormats.color || '#1a1a1a'" @input="applyColor('color', $event.target.value)" class="color-native" />
        </div>
      </template>
    </a-dropdown>
    <a-dropdown trigger="click">
      <a-button :type="activeFormats.backgroundColor ? 'primary' : 'default'">背景色</a-button>
      <template #overlay>
        <div class="color-picker-panel">
          <div v-for="c in presetColors" :key="c" class="color-swatch" :style="{ background: c }" @click="applyColor('backgroundColor', c)"></div>
          <input type="color" :value="activeFormats.backgroundColor || '#fff0f2'" @input="applyColor('backgroundColor', $event.target.value)" class="color-native" />
        </div>
      </template>
    </a-dropdown>
  </a-button-group>

  <a-dropdown v-if="!collapsed">
    <a-button size="small">字号 {{ activeFormats.fontSize || '默认' }}</a-button>
    <template #overlay>
      <a-menu @click="(e) => applyBlockStyle('fontSize', e.key)">
        <a-menu-item key="xs">极小</a-menu-item>
        <a-menu-item key="sm">小</a-menu-item>
        <a-menu-item key="base">正常</a-menu-item>
        <a-menu-item key="lg">大</a-menu-item>
        <a-menu-item key="xl">极大</a-menu-item>
      </a-menu>
    </template>
  </a-dropdown>

  <a-dropdown v-if="!collapsed">
    <a-button size="small">字体 {{ fontFamilyLabel }}</a-button>
    <template #overlay>
      <a-menu @click="(e) => applyBlockStyle('fontFamily', e.key)">
        <a-menu-item key="system">系统默认</a-menu-item>
        <a-menu-item key="serif">宋体</a-menu-item>
        <a-menu-item key="sans">黑体</a-menu-item>
        <a-menu-item key="kai">楷体</a-menu-item>
      </a-menu>
    </template>
  </a-dropdown>

  <a-button v-if="collapsed" size="small" @click="showMore = !showMore">更多</a-button>
</div>

<div v-if="collapsed && showMore" class="edit-toolbar edit-toolbar-more">
  <a-button-group size="small">
    <a-button :type="activeFormats.align === 'left' ? 'primary' : 'default'" @click="applyBlockStyle('align', 'left')">左对齐</a-button>
    <a-button :type="activeFormats.align === 'center' ? 'primary' : 'default'" @click="applyBlockStyle('align', 'center')">居中</a-button>
    <a-button :type="activeFormats.align === 'right' ? 'primary' : 'default'" @click="applyBlockStyle('align', 'right')">右对齐</a-button>
    <a-button :type="activeFormats.align === 'justify' ? 'primary' : 'default'" @click="applyBlockStyle('align', 'justify')">两端</a-button>
  </a-button-group>
  <a-button-group size="small">
    <a-button @click="applyList('ul')">• 无序</a-button>
    <a-button @click="applyList('ol')">1. 有序</a-button>
  </a-button-group>
  <a-button-group size="small">
    <a-button @click="applyBlock('h2')">小标题</a-button>
    <a-button @click="applyBlock('blockquote')">引用</a-button>
  </a-button-group>
</div>
```

- [ ] **Step 4: 提交 UI 骨架**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo && \
  git add project/user/web/src/views/console/EditIndex.vue && \
  git commit -m "feat(edit): 富文本工具栏三段式 UI 骨架"
```

---

## Task 4: 实现工具栏交互逻辑(写回 `styleOverrides` 与光标高亮)

**Files:**
- Modify: `project/user/web/src/views/console/EditIndex.vue`

**Interfaces:**
- Consumes: `bodyToHtmlWithStyles`、`htmlToBodyWithStyles`
- Produces: 完整的工具栏交互(行内/块级/列表/缩进/清除)

- [ ] **Step 1: 新增预设数据与 `fontFamilyLabel`**

在 `<script setup>` 中 `const collapsed = ref(false)` 后追加:

```js
const presetColors = ['#1a1a1a', '#595959', '#ff2442', '#fa8c16', '#faad14', '#07c160', '#1677ff', '#722ed1']
const fontSizeMap = { xs: '极小', sm: '小', base: '正常', lg: '大', xl: '极大' }
const fontFamilyMap = { system: '系统', serif: '宋体', sans: '黑体', kai: '楷体' }
const showMore = ref(false)
const fontFamilyLabel = computed(() => fontFamilyMap[activeFormats.fontFamily] || '默认')
```

- [ ] **Step 2: 实现 `applyInline` 与 `applyColor`**

在 `clearFormat` 函数之后追加:

```js
const applyInline = (styleKey) => {
  editorRef.value?.focus()
  const cmdMap = { bold: 'bold', italic: 'italic', underline: 'underline', strike: 'strikeThrough', code: 'insertHTML' }
  const cmd = cmdMap[styleKey]
  if (cmd === 'insertHTML') {
    const sel = window.getSelection()
    if (!sel || sel.rangeCount === 0) return
    const text = sel.toString() || 'code'
    document.execCommand('insertHTML', false, `<code>${text}</code>`)
  } else if (cmd) {
    document.execCommand(cmd, false, null)
  }
  syncStyleOverridesFromDom()
  updateActiveFormats()
}

const applyColor = (key, value) => {
  editorRef.value?.focus()
  document.execCommand(key === 'color' ? 'foreColor' : 'hiliteColor', false, value)
  activeFormats[key] = value
  syncStyleOverridesFromDom()
}

const applyBlockStyle = (key, value) => {
  editorRef.value?.focus()
  if (key === 'align') {
    document.execCommand(`justify${value.charAt(0).toUpperCase()}${value.slice(1)}`, false, null)
  } else if (key === 'lineHeight') {
    // 简化处理:通过 insertHTML 包裹当前 block
    wrapCurrentBlockStyle(`line-height: ${value}`)
  } else if (key === 'indent') {
    const delta = parseInt(value, 10) - (parseInt(activeFormats.indent, 10) || 0)
    for (let i = 0; i < Math.abs(delta); i++) {
      document.execCommand(delta > 0 ? 'indent' : 'outdent', false, null)
    }
  } else if (key === 'fontSize') {
    const sizeMap = { xs: '12px', sm: '14px', base: '15px', lg: '18px', xl: '22px' }
    wrapCurrentBlockStyle(`font-size: ${sizeMap[value] || '15px'}`)
  } else if (key === 'fontFamily') {
    const map = { system: 'system-ui, -apple-system, sans-serif', serif: 'serif', sans: 'sans-serif', kai: 'KaiTi, serif' }
    wrapCurrentBlockStyle(`font-family: ${map[value] || map.system}`)
  }
  activeFormats[key] = value
  syncStyleOverridesFromDom()
}

const wrapCurrentBlockStyle = (styleStr) => {
  const sel = window.getSelection()
  if (!sel || sel.rangeCount === 0) return
  const node = sel.anchorNode
  if (!node) return
  let el = node.nodeType === Node.ELEMENT_NODE ? node : node.parentElement
  while (el && el !== editorRef.value && !/^(P|H[1-4]|BLOCKQUOTE|UL|OL|LI)$/.test(el.tagName)) {
    el = el.parentElement
  }
  if (!el || el === editorRef.value) return
  el.setAttribute('style', `${el.getAttribute('style') || ''}${styleStr};`)
}

const applyList = (kind) => {
  editorRef.value?.focus()
  document.execCommand(kind === 'ul' ? 'insertUnorderedList' : 'insertOrderedList', false, null)
  syncStyleOverridesFromDom()
}

const applyBlock = (tag) => {
  editorRef.value?.focus()
  document.execCommand('formatBlock', false, `<${tag}>`)
  syncStyleOverridesFromDom()
}

const syncStyleOverridesFromDom = () => {
  if (!editorRef.value) return
  const { body, styleOverrides } = htmlToBodyWithStyles(editorRef.value.innerHTML)
  styleOverridesRef.value = styleOverrides
  // 不回写 body(避免破坏正在编辑的字符位置),仅在保存时再生成
  return body
}
```

并在顶部 `import` 区域追加 `fontSizeMap` 用到的 `fontSizeToPx`,在 `articleBlocks.js` 末尾 `export` 区域追加 `export { fontSizeToPx }` 不行——保持内部实现,工具栏用 px 字面量即可。

- [ ] **Step 3: 实现 `updateActiveFormats` - 光标高亮**

替换 `updateActiveFormat` 为:

```js
const updateActiveFormats = () => {
  if (!editorRef.value) return
  const node = document.getSelection()?.anchorNode
  if (!node) return
  let el = node.nodeType === Node.ELEMENT_NODE ? node : node.parentElement
  // 重置
  Object.keys(activeFormats).forEach(k => { activeFormats[k] = (typeof activeFormats[k] === 'string' ? '' : false) })
  while (el && el !== editorRef.value) {
    const tag = el.tagName.toLowerCase()
    if (['b', 'strong'].includes(tag)) activeFormats.bold = true
    if (['i', 'em'].includes(tag)) activeFormats.italic = true
    if (tag === 'u') activeFormats.underline = true
    if (['s', 'del', 'strike'].includes(tag)) activeFormats.strike = true
    if (tag === 'code') activeFormats.code = true
    if (['h1', 'h2', 'h3', 'h4'].includes(tag)) {
      activeFormats.align = el.getAttribute('align') || el.style.textAlign || ''
    }
    if (el.style) {
      if (el.style.textAlign) activeFormats.align = el.style.textAlign
      if (el.style.lineHeight) activeFormats.lineHeight = el.style.lineHeight
      if (el.style.paddingLeft) {
        const px = parseInt(el.style.paddingLeft, 10)
        activeFormats.indent = String(Math.round(px / 24))
      }
      if (el.style.color) activeFormats.color = el.style.color
      if (el.style.backgroundColor) activeFormats.backgroundColor = el.style.backgroundColor
      if (el.style.fontSize) activeFormats.fontSize = el.style.fontSize
      if (el.style.fontFamily) activeFormats.fontFamily = el.style.fontFamily
    }
    el = el.parentElement
  }
}
```

同时把模板中 `@keyup="updateActiveFormat"` 改为 `@keyup="updateActiveFormats"`,`@mouseup="updateActiveFormat"` 同样修改。

- [ ] **Step 4: 提交交互逻辑**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo && \
  git add project/user/web/src/views/console/EditIndex.vue && \
  git commit -m "feat(edit): 实现富文本工具栏交互与光标高亮"
```

---

## Task 5: 保存时调用 `htmlToBodyWithStyles` 写入 `styleOverrides`

**Files:**
- Modify: `project/user/web/src/views/console/EditIndex.vue`

- [ ] **Step 1: 改写 `save` 函数**

替换 `save` 函数为:

```js
const save = () => {
  const finalTitle = title.value.trim()
  if (!finalTitle) {
    message.error('标题不能为空')
    return
  }

  if (!editorRef.value) return
  const { body, styleOverrides } = htmlToBodyWithStyles(editorRef.value.innerHTML)

  const article = {
    ...originalArticle.value,
    title: finalTitle,
    body,
    styleOverrides
  }

  if (!saveCurrentArticle(article)) {
    message.error('保存失败，请检查浏览器存储权限')
    return
  }

  syncArticleToQueue(article)
  message.success('内容已保存')
  router.push('/console/preview')
}
```

- [ ] **Step 2: 移除旧的 `bodyHtml` 引用**

搜索 `bodyHtml` 引用,在 `EditIndex.vue` 中:
- 模板里 `v-html="bodyHtml"` 改为去掉(因为我们用 `setTimeout` 写入 `innerHTML`)
- 删除 `const bodyHtml = ref('')` 行
- 删除 `onMounted` 中 `bodyHtml.value = bodyToHtml(article.body)` 行(已被 Task 3 步骤 2 取代)

- [ ] **Step 3: 提交**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo && \
  git add project/user/web/src/views/console/EditIndex.vue && \
  git commit -m "feat(edit): 保存时同时持久化 styleOverrides"
```

---

## Task 6: 编写端到端验证脚本

**Files:**
- Create: `tests/e2e/verify_richtext_toolbar.py`

- [ ] **Step 1: 写脚本骨架**

```python
from playwright.sync_api import sync_playwright

BASE = 'http://localhost:22345'


def test_richtext_toolbar():
    errors = []
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={'width': 1280, 'height': 900})
        page.on('console', lambda msg: errors.append(msg.text) if msg.type == 'error' else None)

        article = {
            'id': 'test-richtext-001',
            'title': '富文本工具栏测试',
            'body': '【小标题】\n正文段落。\n\n> 引用文本\n\n- 列表一\n- 列表二',
            'completedAt': '2026-07-01',
            'wordCount': 100,
            'style': '专业严谨'
        }
        queue = [{
            'id': 'test-richtext-001',
            'status': 'completed',
            'title': article['title'],
            'platform': '微信公众号',
            'wordCount': 100,
            'style': '专业严谨',
            'completedAt': '2026-07-01',
            'content': {'title': article['title'], 'body': article['body']}
        }]

        page.goto(BASE + '/')
        page.wait_for_load_state('networkidle')
        page.evaluate("""(data) => {
            localStorage.setItem('aichuangzuo_current_article', JSON.stringify(data.article));
            localStorage.setItem('aichuangzuo_generation_queue', JSON.stringify(data.queue));
        }""", {'article': article, 'queue': queue})

        # 打开编辑页
        page.goto(BASE + '/console/edit')
        page.wait_for_load_state('networkidle')
        page.wait_for_timeout(500)

        editor = page.locator('.edit-editor').first
        assert editor.count() > 0, 'editor not rendered'

        # 操作 1:选中第一段正文,加下划线
        editor.evaluate("""(el) => {
            const p = el.querySelector('p');
            const range = document.createRange();
            range.selectNodeContents(p);
            const sel = window.getSelection();
            sel.removeAllRanges();
            sel.addRange(range);
        }""")
        page.click('button[title="下划线"]')
        page.wait_for_timeout(200)

        # 操作 2:点工具栏的"居中"
        page.click('button:has-text("居中")')
        page.wait_for_timeout(200)

        # 保存
        page.click('.edit-actions .save')
        page.wait_for_timeout(500)

        # 验证 1:localStorage 中有 styleOverrides
        saved = page.evaluate("() => JSON.parse(localStorage.getItem('aichuangzuo_current_article') || '{}')")
        assert 'styleOverrides' in saved, 'styleOverrides not persisted'
        assert saved['styleOverrides']['blocks'] or saved['styleOverrides']['inlines'], 'styleOverrides is empty'

        # 验证 2:刷新后重新进入编辑页,样式仍存在
        page.goto(BASE + '/console/edit')
        page.wait_for_load_state('networkidle')
        page.wait_for_timeout(500)
        reloaded_html = page.locator('.edit-editor').first.evaluate('(el) => el.innerHTML')
        assert '<u>' in reloaded_html or 'text-decoration' in reloaded_html, 'underline style not reloaded'
        assert 'text-align: center' in reloaded_html or 'text-align:center' in reloaded_html, 'align style not reloaded'

        # 验证 3:旧文章(无 styleOverrides)不报错
        page.evaluate("""() => {
            const old = {
                id: 'old-001', title: '旧文章', body: '正文',
                completedAt: '2026-06-01', wordCount: 10, style: '通用'
            };
            localStorage.setItem('aichuangzuo_current_article', JSON.stringify(old));
        }""")
        page.goto(BASE + '/console/edit')
        page.wait_for_load_state('networkidle')
        page.wait_for_timeout(500)
        assert page.locator('.edit-editor').count() > 0, 'old article failed to load'

        page.screenshot(path='/tmp/verify_richtext_toolbar.png', full_page=True)
        browser.close()
        print('richtext toolbar verification passed')
        if errors:
            print(f'console errors: {len(errors)}')
            for e in errors:
                print(f'  ERROR: {e}')


if __name__ == '__main__':
    test_richtext_toolbar()
```

- [ ] **Step 2: 启动 Vite dev 并跑脚本**

Run(两个终端或先后):
```bash
# 终端 1
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/web && npm run dev
```

```bash
# 终端 2
cd /Users/panyong/aio_project/ai_chuangzuo && \
  python3 tests/e2e/verify_richtext_toolbar.py
```
Expected: 末行打印 `richtext toolbar verification passed`,无 ERROR 日志。

- [ ] **Step 3: 修复不通过项(若需要)**

如果断言失败,根据输出调整 Task 3/4/5 的实现后重新提交并重跑。

- [ ] **Step 4: 提交脚本**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo && \
  git add tests/e2e/verify_richtext_toolbar.py && \
  git commit -m "test(e2e): 富文本工具栏扩展端到端验证"
```

---

## Task 7: 回归现有 `verify_console_content_editing.py`

**Files:**
- Test: `tests/e2e/verify_console_content_editing.py`(无修改,只跑)

- [ ] **Step 1: 跑现有脚本**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo && \
  python3 tests/e2e/verify_console_content_editing.py
```
Expected: 末行 `console content editing verification passed`。

- [ ] **Step 2: 如失败,排查回归原因并修复**

常见原因:`bodyHtml` 引用未清理、`onMounted` 时序问题、编辑页保存后 preview 标题未刷新。按需回到 Task 3/5 微调。
