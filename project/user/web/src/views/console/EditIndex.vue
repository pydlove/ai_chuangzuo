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

      <div
        ref="editorRef"
        class="edit-editor"
        contenteditable="true"
        @paste="onPaste"
        @keyup="updateActiveFormat"
        @mouseup="updateActiveFormat"
        v-html="bodyHtml"
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
const collapsed = ref(false)
const showMore = ref(false)

const presetColors = ['#1a1a1a', '#595959', '#ff2442', '#fa8c16', '#faad14', '#07c160', '#1677ff', '#722ed1']
const fontFamilyMap = { system: '系统', serif: '宋体', sans: '黑体', kai: '楷体' }
const fontSizeMap = { xs: '极小', sm: '小', base: '正常', lg: '大', xl: '极大' }
const fontFamilyLabel = computed(() => fontFamilyMap[activeFormats.fontFamily] || '默认')
const fontSizeLabel = computed(() => fontSizeMap[activeFormats.fontSize] || '默认')

onMounted(() => {
  const article = loadCurrentArticle()
  if (!article) return
  originalArticle.value = JSON.parse(JSON.stringify(article))
  title.value = article.title || ''
  styleOverridesRef.value = article.styleOverrides || { blocks: {}, inlines: [] }
  setTimeout(() => {
    if (editorRef.value) {
      editorRef.value.innerHTML = bodyToHtmlWithStyles(article.body, styleOverridesRef.value)
    }
  }, 0)
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

const updateActiveFormat = () => {
  const editor = editorRef.value
  if (!editor) return
  const node = document.getSelection()?.anchorNode
  if (!node) return
  let el = node.nodeType === Node.ELEMENT_NODE ? node : node.parentElement
  while (el && el !== editor) {
    const tag = el.tagName.toLowerCase()
    if (tag === 'h2') {
      activeFormat.value = 'heading'
      return
    }
    if (tag === 'blockquote') {
      activeFormat.value = 'blockquote'
      return
    }
    if (tag === 'ul' || tag === 'ol' || tag === 'li') {
      activeFormat.value = 'list'
      return
    }
    el = el.parentElement
  }
  activeFormat.value = ''
}

const formatHeading = () => {
  editorRef.value?.focus()
  document.execCommand('formatBlock', false, '<h2>')
  updateActiveFormat()
}

const formatQuote = () => {
  editorRef.value?.focus()
  document.execCommand('formatBlock', false, '<blockquote>')
  updateActiveFormat()
}

const formatList = () => {
  editorRef.value?.focus()
  document.execCommand('insertUnorderedList')
  updateActiveFormat()
}

const formatBold = () => {
  editorRef.value?.focus()
  document.execCommand('bold')
}

const formatItalic = () => {
  editorRef.value?.focus()
  document.execCommand('italic')
}

const clearFormat = () => {
  editorRef.value?.focus()
  document.execCommand('removeFormat')
  document.execCommand('formatBlock', false, '<p>')
  document.execCommand('outdent')
  updateActiveFormat()
}

const save = () => {
  const finalTitle = title.value.trim()
  if (!finalTitle) {
    message.error('标题不能为空')
    return
  }

  const html = editorRef.value?.innerHTML || ''
  const body = htmlToBody(html)

  const article = {
    ...originalArticle.value,
    title: finalTitle,
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
  background: #ff2442;
  color: #fff;
}
</style>
