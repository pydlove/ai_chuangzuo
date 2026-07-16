<template>
  <div class="edit-index">
    <div class="edit-header">
      <button class="back-btn" @click="$router.push('/console/works')">
        ← 返回
      </button>
      <h2 class="edit-title">编辑内容</h2>
    </div>

    <div v-if="!originalArticle" class="edit-empty">
      <a-empty description="暂无文章内容">
        <button class="empty-btn" @click="$router.push('/console/create')">去创作</button>
      </a-empty>
    </div>

    <div v-else class="edit-form">
      <input
        v-model="title"
        type="text"
        class="edit-title-input"
        placeholder="输入文章标题"
      />

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
              <div class="color-picker-panel" @mousedown.prevent>
                <div v-for="c in presetColors" :key="c" class="color-swatch" :style="{ background: c }" @click="applyColor('color', c)"></div>
                <input type="color" :value="activeFormats.color || '#1a1a1a'" @input="applyColor('color', $event.target.value)" class="color-native" />
              </div>
            </template>
          </a-dropdown>
          <a-dropdown trigger="click">
            <a-button :type="activeFormats.backgroundColor ? 'primary' : 'default'">背景色</a-button>
            <template #overlay>
              <div class="color-picker-panel" @mousedown.prevent>
                <div v-for="c in presetColors" :key="c" class="color-swatch" :style="{ background: c }" @click="applyColor('backgroundColor', c)"></div>
                <input type="color" :value="activeFormats.backgroundColor || '#fff0f2'" @input="applyColor('backgroundColor', $event.target.value)" class="color-native" />
              </div>
            </template>
          </a-dropdown>
        </a-button-group>

        <a-dropdown v-if="!collapsed">
          <a-button size="small">字号 {{ fontSizeLabel }}</a-button>
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

        <a-button-group size="small" class="toolbar-group">
          <a-button :type="activeFormats.align === 'left' ? 'primary' : 'default'" @click="applyBlockStyle('align', 'left')">左对齐</a-button>
          <a-button :type="activeFormats.align === 'center' ? 'primary' : 'default'" @click="applyBlockStyle('align', 'center')">居中</a-button>
          <a-button :type="activeFormats.align === 'right' ? 'primary' : 'default'" @click="applyBlockStyle('align', 'right')">右对齐</a-button>
          <a-button :type="activeFormats.align === 'justify' ? 'primary' : 'default'" @click="applyBlockStyle('align', 'justify')">两端</a-button>
        </a-button-group>

        <a-button-group size="small" class="toolbar-group">
          <a-button @click="applyList('ul')">• 无序</a-button>
          <a-button @click="applyList('ol')">1. 有序</a-button>
        </a-button-group>

        <a-button-group size="small" class="toolbar-group">
          <a-button @click="applyBlock('p')">正文</a-button>
          <a-button @click="applyBlock('h2')">小标题</a-button>
          <a-button @click="applyBlock('blockquote')">引用</a-button>
        </a-button-group>
      </div>

      <div
        ref="editorRef"
        class="edit-editor"
        contenteditable="true"
        @paste="onPaste"
        @keyup="updateActiveFormats"
        @mouseup="updateActiveFormats"
      />
    </div>

    <div class="edit-actions">
      <button class="cancel" @click="cancel">取消</button>
      <button class="save" @click="save">保存修改</button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { getArticle, updateArticle } from '@/api/article.js'
import { bodyToHtmlWithStyles, htmlToBodyWithStyles, stripLeadingTitle } from '@/utils/articleBlocks.js'

const router = useRouter()
const route = useRoute()
const originalArticle = ref(null)
const title = ref('')
const editorRef = ref(null)
const styleOverridesRef = ref({ blocks: {}, inlines: [] })
const activeFormats = reactive({
  bold: false, italic: false, underline: false, strike: false, code: false,
  align: '', lineHeight: '', indent: '', fontSize: '', fontFamily: '',
  color: '', backgroundColor: ''
})
const collapsed = ref(false)
const showMore = ref(false)

const presetColors = ['#1a1a1a', '#595959', '#ff2442', '#fa8c16', '#faad14', '#07c160', '#1677ff', '#722ed1']
const fontFamilyMap = { system: '系统', serif: '宋体', sans: '黑体', kai: '楷体' }
const fontSizeMap = { xs: '极小', sm: '小', base: '正常', lg: '大', xl: '极大' }
const fontFamilyLabel = computed(() => fontFamilyMap[activeFormats.fontFamily] || '默认')
const fontSizeLabel = computed(() => fontSizeMap[activeFormats.fontSize] || '默认')

onMounted(async () => {
  const bizNo = route.params.bizNo
  if (!bizNo) return
  try {
    const fresh = await getArticle(bizNo)
    if (!fresh) return
    originalArticle.value = { bizNo: fresh.bizNo, title: fresh.title, body: fresh.body }
    title.value = fresh.title || ''
    styleOverridesRef.value = fresh.styleOverrides || { blocks: {}, inlines: [] }
    setTimeout(() => {
      if (editorRef.value) {
        // 旧数据 body 开头可能仍含 title，剥掉再渲染避免编辑器里出现双标题
        const cleanBody = stripLeadingTitle(fresh.body || '', (fresh.title || '').trim())
        editorRef.value.innerHTML = bodyToHtmlWithStyles(cleanBody, styleOverridesRef.value)
      }
    }, 0)
  } catch (e) {
    console.warn('edit 加载 article 失败', e)
    message.error('加载文章失败，请稍后重试')
  }
  window.addEventListener('resize', onResize)
  onResize()
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
})

const onResize = () => {
  collapsed.value = window.innerWidth <= 1024
}

const onPaste = (e) => {
  e.preventDefault()
  const text = (e.clipboardData || window.clipboardData).getData('text/plain')
  document.execCommand('insertText', false, text)
}

const updateActiveFormats = () => {
  if (!editorRef.value) return
  const node = document.getSelection()?.anchorNode
  if (!node) return
  let el = node.nodeType === Node.ELEMENT_NODE ? node : node.parentElement
  Object.keys(activeFormats).forEach(k => {
    activeFormats[k] = (typeof activeFormats[k] === 'string' ? '' : false)
  })
  while (el && el !== editorRef.value) {
    const tag = el.tagName.toLowerCase()
    if (['b', 'strong'].includes(tag)) activeFormats.bold = true
    if (['i', 'em'].includes(tag)) activeFormats.italic = true
    if (tag === 'u') activeFormats.underline = true
    if (['s', 'del', 'strike'].includes(tag)) activeFormats.strike = true
    if (tag === 'code') activeFormats.code = true
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

const applyInline = (styleKey) => {
  editorRef.value?.focus()
  const cmdMap = { bold: 'bold', italic: 'italic', underline: 'underline', strike: 'strikeThrough' }
  const cmd = cmdMap[styleKey]
  if (styleKey === 'code') {
    const sel = window.getSelection()
    if (!sel || sel.rangeCount === 0) return
    const range = sel.getRangeAt(0)
    if (range.collapsed) return
    // 手动包 <code>：insertHTML 会被 Blink 规范化成 monospace span，保存后 code 样式丢失
    const code = document.createElement('code')
    try {
      range.surroundContents(code)
    } catch (e) {
      code.appendChild(range.extractContents())
      range.insertNode(code)
    }
    sel.removeAllRanges()
    const newRange = document.createRange()
    newRange.selectNodeContents(code)
    sel.addRange(newRange)
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
  const existing = el.getAttribute('style') || ''
  el.setAttribute('style', `${existing}${styleStr};`)
}

const applyBlockStyle = (key, value) => {
  editorRef.value?.focus()
  if (key === 'align') {
    document.execCommand(`justify${value.charAt(0).toUpperCase()}${value.slice(1)}`, false, null)
  } else if (key === 'lineHeight') {
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
  updateActiveFormats()
}

const applyList = (kind) => {
  editorRef.value?.focus()
  document.execCommand(kind === 'ul' ? 'insertUnorderedList' : 'insertOrderedList', false, null)
  syncStyleOverridesFromDom()
  updateActiveFormats()
}

const applyBlock = (tag) => {
  editorRef.value?.focus()
  if (tag === 'p') {
    // 列表项点"正文" = 退出列表；formatBlock 对列表只会套出 <p><ul> 嵌套垃圾
    const node = window.getSelection()?.anchorNode
    const el = node ? (node.nodeType === Node.ELEMENT_NODE ? node : node.parentElement) : null
    const list = el?.closest('ul, ol')
    if (list && editorRef.value?.contains(list)) {
      document.execCommand(list.tagName === 'UL' ? 'insertUnorderedList' : 'insertOrderedList', false, null)
      syncStyleOverridesFromDom()
      updateActiveFormats()
      return
    }
  }
  document.execCommand('formatBlock', false, `<${tag}>`)
  syncStyleOverridesFromDom()
  updateActiveFormats()
}

const syncStyleOverridesFromDom = () => {
  if (!editorRef.value) return
  const { styleOverrides } = htmlToBodyWithStyles(editorRef.value.innerHTML)
  styleOverridesRef.value = styleOverrides
}

const save = async () => {
  const finalTitle = title.value.trim()
  if (!finalTitle) {
    message.error('标题不能为空')
    return
  }

  if (!editorRef.value) return
  const bizNo = originalArticle.value?.bizNo
  if (!bizNo) {
    message.error('文章标识缺失，无法保存')
    return
  }
  const { body, styleOverrides } = htmlToBodyWithStyles(editorRef.value.innerHTML)

  try {
    await updateArticle(bizNo, {
      title: finalTitle,
      body,
      styleOverrides: JSON.stringify(styleOverrides)
    })
    message.success('内容已保存')
    router.push(`/console/preview/${bizNo}`)
  } catch (e) {
    console.warn('edit 保存 article 失败', e)
    message.error('保存失败，请稍后重试')
  }
}

const cancel = () => {
  router.push('/console/works')
}
</script>

<style scoped>
.edit-index {
  padding: 24px;
}

.edit-header {
  display: flex;
  align-items: center;
  gap: 16px;
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
  font-weight: 600;
  line-height: 1.2;
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

.edit-form {
  max-width: 720px;
  margin: 0 auto;
  padding-bottom: 100px;
}

.edit-title-input {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  outline: none;
  margin-bottom: 16px;
  box-sizing: border-box;
}

.edit-title-input::placeholder {
  color: #bfbfbf;
}

.edit-title-input:focus {
  border-color: #ff2442;
  box-shadow: 0 0 0 2px rgba(7, 193, 96, 0.15);
}

.edit-toolbar {
  display: flex;
  gap: 8px;
  padding: 8px 12px;
  background: #f5f5f5;
  border-radius: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
  align-items: center;
}

.edit-toolbar-more {
  margin-top: 8px;
}

.toolbar-group {
  margin-left: 4px;
}

.color-picker-panel {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 8px;
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  width: 160px;
}

.color-swatch {
  width: 22px;
  height: 22px;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
  cursor: pointer;
  transition: transform 0.1s;
}

.color-swatch:hover {
  transform: scale(1.1);
}

.color-native {
  width: 100%;
  height: 28px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  cursor: pointer;
  margin-top: 4px;
}

.toolbar-btn {
  padding: 6px 12px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  transition: all 0.15s;
}

.toolbar-btn:hover {
  border-color: #ff2442;
  color: #ff2442;
}

.toolbar-btn.active {
  background: #ff2442;
  border-color: #ff2442;
  color: #fff;
}

.edit-editor {
  min-height: 400px;
  padding: 20px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 15px;
  line-height: 1.8;
  color: #262626;
  outline: none;
}

.edit-editor:focus {
  border-color: #ff2442;
  box-shadow: 0 0 0 2px rgba(7, 193, 96, 0.15);
}

.edit-editor :deep(h2) {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 24px 0 12px;
}

.edit-editor :deep(blockquote) {
  margin: 16px 0;
  padding: 12px 16px;
  background: #fff0f2;
  border-left: 4px solid #ff2442;
  color: #262626;
}

.edit-editor :deep(ul) {
  margin: 12px 0;
  padding-left: 24px;
}

.edit-editor :deep(li) {
  margin-bottom: 6px;
}

.edit-editor :deep(p) {
  margin: 0 0 12px;
}

.edit-actions {
  position: fixed;
  bottom: 72px;
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

@media (max-width: 768px) {
  .edit-index {
    padding: 16px 12px;
  }

  /* 移动端页脚隐藏，操作条收回贴底 */
  .edit-actions {
    bottom: 24px;
  }
}

.edit-actions .save {
  background: #ff2442;
  color: #fff;
}

/* 深色模式 */
body[data-theme="dark"] .back-btn {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .back-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .edit-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .edit-empty {
  color: #a6a6a6;
}

body[data-theme="dark"] .edit-title-input {
  background: #2a2a2a;
  border-color: #434343;
  color: #f0f0f0;
}

body[data-theme="dark"] .edit-title-input::placeholder {
  color: #737373;
}

body[data-theme="dark"] .edit-title-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(255, 36, 66, 0.15);
}

body[data-theme="dark"] .edit-toolbar {
  background: #2a2a2a;
  border: 1px solid #303030;
}

body[data-theme="dark"] .color-picker-panel {
  background: #1f1f1f;
  border: 1px solid #303030;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.5);
}

body[data-theme="dark"] .color-native {
  background: #2a2a2a;
  border-color: #434343;
}

body[data-theme="dark"] .edit-editor {
  background: #1f1f1f;
  border-color: #434343;
  color: #d9d9d9;
}

body[data-theme="dark"] .edit-editor:focus {
  border-color: var(--color-primary);
}

body[data-theme="dark"] .edit-editor :deep(h2) {
  color: #f0f0f0;
}

body[data-theme="dark"] .edit-editor :deep(blockquote) {
  background: rgba(255, 36, 66, 0.15);
  color: #f0f0f0;
}

body[data-theme="dark"] .edit-editor :deep(code) {
  background: #2a2a2a;
  color: #f0f0f0;
}

body[data-theme="dark"] .edit-actions {
  background: #1f1f1f;
  border-color: #303030;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4);
}

body[data-theme="dark"] .edit-actions .cancel {
  background: #2a2a2a;
  color: #a6a6a6;
}
</style>

<style>
/* 编辑页：Ant 组件被 teleport 到 body 或渲染在子组件根上，需全局覆盖 */
body[data-theme="dark"] .ant-btn {
  background: #2a2a2a !important;
  border-color: #434343 !important;
  color: #d9d9d9 !important;
}

body[data-theme="dark"] .ant-btn:hover {
  border-color: var(--color-primary) !important;
  color: var(--color-primary) !important;
}

body[data-theme="dark"] .ant-btn-primary {
  background: var(--color-primary) !important;
  border-color: var(--color-primary) !important;
  color: #fff !important;
}

body[data-theme="dark"] .ant-btn-primary:hover {
  background: var(--color-primary-hover) !important;
  border-color: var(--color-primary-hover) !important;
  color: #fff !important;
}

body[data-theme="dark"] .ant-dropdown-menu {
  background: #1f1f1f !important;
  border-color: #303030 !important;
}

body[data-theme="dark"] .ant-dropdown-menu-item {
  color: #d9d9d9 !important;
}

body[data-theme="dark"] .ant-dropdown-menu-item:hover {
  background: #2a2a2a !important;
  color: #f0f0f0 !important;
}

body[data-theme="dark"] .ant-dropdown-menu-item-selected {
  background: rgba(255, 36, 66, 0.15) !important;
  color: var(--color-primary) !important;
}
</style>
