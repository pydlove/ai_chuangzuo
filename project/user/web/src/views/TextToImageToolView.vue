<template>
  <div class="text2image-page">
    <!-- 子页面返回头 -->
    <MobileSubpageHeader title="文字转图片" />

    <header class="t2i-hero">
      <div class="t2i-hero-deco t2i-hero-deco-lg"></div>
      <div class="t2i-hero-deco t2i-hero-deco-sm"></div>
      <div class="t2i-hero-inner">
        <div class="t2i-hero-text">
          <span class="t2i-hero-badge">文字转图片</span>
          <h1 class="t2i-hero-title">富文本排版，一键导出长图</h1>
          <p class="t2i-hero-subtitle">把文本转换成图片，支持自定义宽度、背景色、背景图与多种格式导出</p>
        </div>
      </div>
    </header>

    <main class="t2i-main">
      <div class="t2i-card">
        <!-- 顶部控制栏 -->
        <div class="t2i-topbar">
          <div class="t2i-control-group">
            <span class="t2i-control-label">宽度</span>
            <a-slider v-model:value="width" :min="320" :max="1200" class="t2i-width-slider" />
            <a-input-number v-model:value="width" :min="320" :max="1200" class="t2i-width-input" />
          </div>
          <div class="t2i-control-group">
            <span class="t2i-control-label">背景颜色</span>
            <input v-model="bgColor" type="color" class="t2i-color-input" />
          </div>
          <div class="t2i-control-group">
            <span class="t2i-control-label">背景图片</span>
            <button class="t2i-btn t2i-btn--small" @click="triggerBgUpload">上传</button>
            <button v-if="bgImage" class="t2i-btn t2i-btn--small" @click="clearBgImage">清除</button>
            <input ref="bgInput" type="file" accept="image/*" class="t2i-file-input" @change="handleBgChange" />
          </div>
          <div class="t2i-control-group t2i-export-group">
            <a-select v-model:value="exportFormat" class="t2i-format-select">
              <a-select-option value="png">.png</a-select-option>
              <a-select-option value="jpeg">.jpeg</a-select-option>
            </a-select>
            <button class="t2i-btn t2i-btn--primary" @click="exportImage">
              <DownloadOutlined />
              下载图片
            </button>
          </div>
        </div>

        <!-- 富文本工具栏 -->
        <div class="t2i-editor-toolbar">
          <a-select v-model:value="blockType" class="t2i-toolbar-select" @change="exec('formatBlock', blockType)">
            <a-select-option value="P">正文</a-select-option>
            <a-select-option value="H1">标题 1</a-select-option>
            <a-select-option value="H2">标题 2</a-select-option>
            <a-select-option value="BLOCKQUOTE">引用</a-select-option>
          </a-select>
          <div class="t2i-toolbar-divider"></div>
          <button class="t2i-icon-btn" title="加粗" @click="exec('bold')"><BoldOutlined /></button>
          <button class="t2i-icon-btn" title="斜体" @click="exec('italic')"><ItalicOutlined /></button>
          <button class="t2i-icon-btn" title="下划线" @click="exec('underline')"><UnderlineOutlined /></button>
          <button class="t2i-icon-btn" title="删除线" @click="exec('strikeThrough')"><StrikethroughOutlined /></button>
          <div class="t2i-toolbar-divider"></div>
          <button class="t2i-icon-btn t2i-icon-btn--color" title="文字颜色">
            <FontColorsOutlined />
            <input v-model="textColor" type="color" @change="exec('foreColor', textColor)" />
          </button>
          <button class="t2i-icon-btn t2i-icon-btn--color" title="背景高亮">
            <BgColorsOutlined />
            <input v-model="highlightColor" type="color" @change="exec('backColor', highlightColor)" />
          </button>
          <div class="t2i-toolbar-divider"></div>
          <a-select v-model:value="fontFamily" class="t2i-toolbar-select" @change="exec('fontName', fontFamily)">
            <a-select-option value="sans-serif">默认字体</a-select-option>
            <a-select-option value="serif">衬线体</a-select-option>
            <a-select-option value="monospace">等宽体</a-select-option>
          </a-select>
          <a-select v-model:value="fontSize" class="t2i-toolbar-select" @change="exec('fontSize', fontSize)">
            <a-select-option value="3">默认字号</a-select-option>
            <a-select-option value="1">小</a-select-option>
            <a-select-option value="5">大</a-select-option>
            <a-select-option value="7">特大</a-select-option>
          </a-select>
          <a-select v-model:value="lineHeight" class="t2i-toolbar-select">
            <a-select-option value="1.4">默认行高</a-select-option>
            <a-select-option value="1.6">1.6</a-select-option>
            <a-select-option value="1.8">1.8</a-select-option>
            <a-select-option value="2.0">2.0</a-select-option>
          </a-select>
          <div class="t2i-toolbar-divider"></div>
          <button class="t2i-icon-btn" title="左对齐" @click="exec('justifyLeft')"><AlignLeftOutlined /></button>
          <button class="t2i-icon-btn" title="居中" @click="exec('justifyCenter')"><AlignCenterOutlined /></button>
          <button class="t2i-icon-btn" title="右对齐" @click="exec('justifyRight')"><AlignRightOutlined /></button>
          <div class="t2i-toolbar-divider"></div>
          <button class="t2i-icon-btn" title="无序列表" @click="exec('insertUnorderedList')"><UnorderedListOutlined /></button>
          <button class="t2i-icon-btn" title="有序列表" @click="exec('insertOrderedList')"><OrderedListOutlined /></button>
          <div class="t2i-toolbar-divider"></div>
          <button class="t2i-icon-btn" title="分割线" @click="exec('insertHorizontalRule')"><MinusOutlined /></button>
          <button class="t2i-icon-btn" title="撤销" @click="exec('undo')"><UndoOutlined /></button>
          <button class="t2i-icon-btn" title="重做" @click="exec('redo')"><RedoOutlined /></button>
        </div>

        <!-- 编辑器 -->
        <div class="t2i-editor-wrap">
          <div
            ref="editorRef"
            class="t2i-editor"
            contenteditable="true"
            :style="editorStyle"
            @input="onInput"
          ></div>
        </div>
      </div>
    </main>

    <AppFooter variant="mobile" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { toPng, toJpeg } from 'html-to-image'
import { message } from 'ant-design-vue'
import MobileSubpageHeader from '@/components/common/MobileSubpageHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import {
  DownloadOutlined,
  BoldOutlined,
  ItalicOutlined,
  UnderlineOutlined,
  StrikethroughOutlined,
  AlignLeftOutlined,
  AlignCenterOutlined,
  AlignRightOutlined,
  UnorderedListOutlined,
  OrderedListOutlined,
  MinusOutlined,
  UndoOutlined,
  RedoOutlined,
  FontColorsOutlined,
  BgColorsOutlined
} from '@ant-design/icons-vue'

const editorRef = ref(null)
const bgInput = ref(null)

const width = ref(860)
const bgColor = ref('#ffffff')
const bgImage = ref('')
const textColor = ref('#1a1a1a')
const highlightColor = ref('#fff566')
const fontFamily = ref('sans-serif')
const fontSize = ref('3')
const lineHeight = ref('1.6')
const blockType = ref('P')
const exportFormat = ref('png')

const defaultContent = `<p>文字转图片演示😊</p>
<p><br></p>
<p>拥有丰富的样式选择</p>
<p>可自由调整宽度、背景色</p>
<p>支持一键导出为长图</p>
`

const editorStyle = computed(() => ({
  width: `${width.value}px`,
  maxWidth: '100%',
  backgroundColor: bgColor.value,
  backgroundImage: bgImage.value ? `url(${bgImage.value})` : 'none',
  backgroundSize: 'cover',
  backgroundPosition: 'center',
  fontFamily: fontFamily.value,
  lineHeight: lineHeight.value
}))

onMounted(() => {
  if (editorRef.value) {
    editorRef.value.innerHTML = defaultContent
  }
})

onBeforeUnmount(() => {
  if (bgImage.value && bgImage.value.startsWith('blob:')) {
    URL.revokeObjectURL(bgImage.value)
  }
})

function onInput() {
  // 保留内容与编辑器同步即可，导出时直接读取 DOM
}

function exec(command, value = null) {
  editorRef.value?.focus()
  if (command === 'fontName' || command === 'foreColor' || command === 'backColor') {
    document.execCommand('styleWithCss', false, true)
  }
  document.execCommand(command, false, value)
}

function triggerBgUpload() {
  bgInput.value?.click()
}

function handleBgChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    message.error('请上传图片文件')
    return
  }
  if (bgImage.value && bgImage.value.startsWith('blob:')) {
    URL.revokeObjectURL(bgImage.value)
  }
  bgImage.value = URL.createObjectURL(file)
  e.target.value = ''
}

function clearBgImage() {
  if (bgImage.value && bgImage.value.startsWith('blob:')) {
    URL.revokeObjectURL(bgImage.value)
  }
  bgImage.value = ''
}

async function exportImage() {
  if (!editorRef.value) return
  const node = editorRef.value
  const options = { pixelRatio: 2, cacheBust: true }
  try {
    const dataUrl = exportFormat.value === 'jpeg'
      ? await toJpeg(node, { ...options, quality: 0.92, backgroundColor: bgColor.value })
      : await toPng(node, options)

    const a = document.createElement('a')
    a.href = dataUrl
    a.download = `text-image-${Date.now()}.${exportFormat.value}`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    message.success('导出成功')
  } catch (e) {
    message.error('导出失败：' + (e?.message || '未知错误'))
  }
}
</script>

<style scoped>
.text2image-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f8f9fa;
  color: #1a1a1a;
  -webkit-font-smoothing: antialiased;
}

/* Hero */
.t2i-hero {
  position: relative;
  background: linear-gradient(180deg, #FFF5F7 0%, #FFFFFF 100%);
  padding: 32px 24px;
  overflow: hidden;
}
.t2i-hero-inner {
  max-width: 1200px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}
.t2i-hero-text { max-width: 560px; }
.t2i-hero-badge {
  display: inline-block;
  background: linear-gradient(135deg, #FFF0F2, #FFE4E8);
  color: #FF2442;
  font-size: 12px;
  font-weight: 600;
  padding: 5px 14px;
  border-radius: 16px;
  margin-bottom: 14px;
}
.t2i-hero-title {
  font-size: 32px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
}
.t2i-hero-subtitle {
  font-size: 14px;
  color: #8c8c8c;
  margin: 8px 0 0;
}
.t2i-hero-deco {
  position: absolute;
  border-radius: 50%;
  background: #FFE8EC;
}
.t2i-hero-deco-lg {
  width: 200px; height: 200px;
  top: -60px; right: -40px;
}
.t2i-hero-deco-sm {
  width: 80px; height: 80px;
  top: 20px; right: 160px;
}
@media (max-width: 768px) {
  .t2i-hero { padding: 24px 16px; }
  .t2i-hero-title { font-size: 26px; }
  .t2i-hero-subtitle { font-size: 13px; }
}

/* 主内容 */
.t2i-main {
  flex: 1;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 24px 16px;
}
.t2i-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}
@media (max-width: 768px) {
  .t2i-card { padding: 16px; }
  .t2i-main { padding: 16px; }
}

/* 顶部控制栏 */
.t2i-topbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px 24px;
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}
.t2i-control-group {
  display: flex;
  align-items: center;
  gap: 10px;
}
.t2i-control-label {
  font-size: 13px;
  color: #595959;
  white-space: nowrap;
}
.t2i-width-slider {
  width: 180px;
}
.t2i-width-slider:deep(.ant-slider-track) {
  background: #FF2442;
}
.t2i-width-slider:deep(.ant-slider-handle::after) {
  box-shadow: 0 0 0 2px #FF2442;
}
.t2i-width-input {
  width: 72px;
}
.t2i-color-input {
  width: 32px;
  height: 32px;
  padding: 0;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: none;
  cursor: pointer;
}
.t2i-format-select {
  width: 90px;
}
.t2i-export-group {
  margin-left: auto;
}
.t2i-file-input {
  display: none;
}

/* 按钮 */
.t2i-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 6px 14px;
  border-radius: 6px;
  border: 1px solid #d9d9d9;
  background: #fff;
  color: #595959;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}
.t2i-btn:hover {
  border-color: #FF2442;
  color: #FF2442;
}
.t2i-btn--primary {
  background: linear-gradient(135deg, #FF4D6F 0%, #FF2442 100%);
  border-color: transparent;
  color: #fff;
  box-shadow: 0 4px 12px rgba(255, 36, 66, 0.25);
}
.t2i-btn--primary:hover {
  background: linear-gradient(135deg, #e61e3a 0%, #c91a33 100%);
  color: #fff;
}
.t2i-btn--small {
  padding: 4px 10px;
  font-size: 12px;
}

/* 富文本工具栏 */
.t2i-editor-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  padding: 10px 12px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  margin-bottom: 12px;
}
.t2i-icon-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: #fff;
  color: #595959;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.2s;
}
.t2i-icon-btn:hover {
  border-color: #FF2442;
  color: #FF2442;
}
.t2i-icon-btn--color {
  overflow: hidden;
}
.t2i-icon-btn--color input[type="color"] {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}
.t2i-toolbar-select {
  min-width: 96px;
}
.t2i-toolbar-select:deep(.ant-select-selector) {
  border-radius: 6px;
}
.t2i-toolbar-select:deep(.ant-select-focused .ant-select-selector) {
  border-color: #FF2442 !important;
  box-shadow: 0 0 0 2px rgba(255, 36, 66, 0.15) !important;
}
.t2i-toolbar-divider {
  width: 1px;
  height: 20px;
  background: #e8e8e8;
  margin: 0 2px;
}

/* 编辑器 */
.t2i-editor-wrap {
  display: flex;
  justify-content: center;
  padding: 24px;
  background: #f8f9fa;
  border: 1px dashed #e0e0e0;
  border-radius: 12px;
  overflow: auto;
}
.t2i-editor {
  min-height: 320px;
  padding: 32px;
  outline: none;
  color: #1a1a1a;
  font-size: 16px;
  line-height: 1.6;
  box-shadow: 0 4px 20px rgba(0,0,0,0.06);
  border-radius: 8px;
  transition: box-shadow 0.2s;
}
.t2i-editor:empty::before {
  content: '在此输入内容...';
  color: #bfbfbf;
}
.t2i-editor:focus {
  box-shadow: 0 4px 24px rgba(255, 36, 66, 0.12);
}
.t2i-editor :deep(h1) {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 16px;
  line-height: 1.3;
}
.t2i-editor :deep(h2) {
  font-size: 22px;
  font-weight: 600;
  margin: 0 0 12px;
  line-height: 1.35;
}
.t2i-editor :deep(p) {
  margin: 0 0 12px;
}
.t2i-editor :deep(blockquote) {
  margin: 0 0 12px;
  padding: 8px 16px;
  border-left: 4px solid #FF2442;
  background: rgba(255, 36, 66, 0.05);
  color: #595959;
}
.t2i-editor :deep(ul), .t2i-editor :deep(ol) {
  margin: 0 0 12px;
  padding-left: 24px;
}
.t2i-editor :deep(hr) {
  border: none;
  border-top: 1px solid #e0e0e0;
  margin: 16px 0;
}

/* 暗色主题 */
body[data-theme="dark"] .text2image-page { background: #141414; color: #e0e0e0; }
body[data-theme="dark"] .t2i-hero {
  background: linear-gradient(180deg, #2a1f22 0%, #1f1f1f 100%);
}
body[data-theme="dark"] .t2i-hero-title { color: #e0e0e0; }
body[data-theme="dark"] .t2i-hero-subtitle { color: #8c8c8c; }
body[data-theme="dark"] .t2i-hero-deco { background: #3a2a2e; }
body[data-theme="dark"] .t2i-hero-badge {
  background: linear-gradient(135deg, #3a2a2e, #2a1f22);
  color: #ff4d6f;
}
body[data-theme="dark"] .t2i-card {
  background: #1f1f1f;
  box-shadow: none;
}
body[data-theme="dark"] .t2i-topbar { border-bottom-color: #303030; }
body[data-theme="dark"] .t2i-control-label { color: #a6a6a6; }
body[data-theme="dark"] .t2i-btn {
  background: #1f1f1f;
  border-color: #404040;
  color: #a6a6a6;
}
body[data-theme="dark"] .t2i-btn:hover {
  border-color: #ff4d6f;
  color: #ff4d6f;
}
body[data-theme="dark"] .t2i-btn--primary {
  background: linear-gradient(135deg, #FF4D6F 0%, #FF2442 100%);
  color: #fff;
}
body[data-theme="dark"] .t2i-btn--primary:hover {
  background: linear-gradient(135deg, #e61e3a 0%, #c91a33 100%);
  color: #fff;
}
body[data-theme="dark"] .t2i-editor-toolbar {
  background: #262626;
  border-color: #404040;
}
body[data-theme="dark"] .t2i-icon-btn {
  background: #1f1f1f;
  color: #a6a6a6;
}
body[data-theme="dark"] .t2i-icon-btn:hover {
  border-color: #ff4d6f;
  color: #ff4d6f;
}
body[data-theme="dark"] .t2i-toolbar-divider { background: #404040; }
body[data-theme="dark"] .t2i-editor-wrap {
  background: #141414;
  border-color: #303030;
}
body[data-theme="dark"] .t2i-editor {
  color: #e0e0e0;
}
body[data-theme="dark"] .t2i-editor :deep(blockquote) {
  background: rgba(255, 77, 111, 0.08);
  border-left-color: #ff4d6f;
}
body[data-theme="dark"] .t2i-editor :deep(hr) {
  border-top-color: #404040;
}
</style>
