<template>
  <div class="mobile-watermark">
    <!-- 子页面返回头 -->
    <header class="mw-subpage-header">
      <div class="mw-subpage-back" @click="goBack">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6"></polyline>
        </svg>
        <span>返回</span>
      </div>
      <div class="mw-subpage-title">AI 去水印</div>
    </header>

    <!-- 宣传文案 -->
    <section class="mw-hero">
      <div class="mw-hero__inner">
        <div class="mw-hero__badge">AI 去水印</div>
        <h1 class="mw-hero__title">框选水印，一键消除</h1>
        <p class="mw-hero__desc">本地处理、保护隐私，快速去除图片中的水印与杂物</p>
      </div>
    </section>

    <!-- 工具区 -->
    <main class="mw-tool">
      <div class="mw-tool__inner">
      <!-- 上传 -->
      <div
        v-if="!imageUrl"
        class="mw-upload"
        :class="{ dragging }"
        @click="triggerUpload"
        @dragenter.prevent="dragging = true"
        @dragover.prevent
        @dragleave.prevent="dragging = false"
        @drop.prevent="onDrop"
      >
        <input
          ref="fileInput"
          type="file"
          accept="image/*"
          class="mw-upload__input"
          @change="onFileChange"
        />
        <div class="mw-upload__icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="17 8 12 3 7 8" />
            <line x1="12" y1="3" x2="12" y2="15" />
          </svg>
        </div>
        <div class="mw-upload__title">点击或拖拽上传图片</div>
        <div class="mw-upload__tip">支持 JPG / PNG / WebP，单张不超过 20MB</div>
      </div>

      <!-- 编辑区 -->
      <div v-else class="mw-editor">
        <div class="mw-editor__header">
          <span class="mw-editor__title">框选水印区域</span>
          <button class="mw-editor__reset" @click="resetImage">重新上传</button>
        </div>

        <div
          ref="canvasWrap"
          class="mw-canvas-wrap"
          @mousedown="onMouseDown"
          @mousemove="onMouseMove"
          @mouseup="onMouseUp"
          @mouseleave="onMouseUp"
          @touchstart.prevent="onTouchStart"
          @touchmove.prevent="onTouchMove"
          @touchend.prevent="onTouchEnd"
        >
          <canvas ref="canvas" class="mw-canvas"></canvas>
          <div
            v-if="selection"
            class="mw-selection"
            :style="selectionStyle"
          >
            <span class="mw-selection__label">水印区域</span>
          </div>
        </div>

        <div class="mw-editor__tip">在图片上拖动框选需要去除的水印，然后点击处理</div>

        <button
          class="mw-action-btn"
          :disabled="!selection || processing"
          @click="process"
        >
          <span v-if="processing" class="mw-action-btn__spinner"></span>
          <span>{{ processing ? 'AI 处理中…' : '开始去水印' }}</span>
        </button>
      </div>

      <!-- 结果 -->
      <div v-if="resultUrl" class="mw-result">
        <div class="mw-result__header">
          <span class="mw-result__title">处理结果</span>
        </div>
        <img :src="resultUrl" alt="处理结果" class="mw-result__img" />
        <a
          :href="resultUrl"
          download="爱创作去水印.png"
          class="mw-action-btn mw-action-btn--primary"
          @click="onDownload"
        >下载图片</a>
        <button class="mw-reset-link" @click="resetAll">处理另一张</button>
      </div>
    </div>
    </main>

    <MobileToolFooter />
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import MobileToolFooter from '@/components/common/MobileToolFooter.vue'

const router = useRouter()

const fileInput = ref(null)
const canvas = ref(null)
const canvasWrap = ref(null)
const dragging = ref(false)

const imageFile = ref(null)
const imageUrl = ref('')
const originalImage = ref(null)
const selection = ref(null)
const isSelecting = ref(false)
const selectionStart = ref({ x: 0, y: 0 })
const processing = ref(false)
const resultUrl = ref('')

const selectionStyle = computed(() => {
  if (!selection.value) return {}
  const { x, y, w, h } = selection.value
  return {
    left: `${x}px`,
    top: `${y}px`,
    width: `${w}px`,
    height: `${h}px`
  }
})

function goBack() {
  router.back()
}

function triggerUpload() {
  fileInput.value?.click()
}

function onDrop(e) {
  dragging.value = false
  const file = e.dataTransfer?.files?.[0]
  if (file) handleFile(file)
}

function onFileChange(e) {
  const file = e.target.files?.[0]
  if (file) handleFile(file)
}

function handleFile(file) {
  if (!file.type.startsWith('image/')) {
    message.error('请选择图片文件')
    return
  }
  if (file.size > 20 * 1024 * 1024) {
    message.error('图片大小不能超过 20MB')
    return
  }
  resetAll()
  imageFile.value = file
  imageUrl.value = URL.createObjectURL(file)
  nextTick(() => loadImageToCanvas())
}

function loadImageToCanvas() {
  if (!canvas.value || !imageUrl.value) return
  const img = new Image()
  img.onload = () => {
    originalImage.value = img
    renderCanvas()
  }
  img.onerror = () => message.error('图片加载失败')
  img.src = imageUrl.value
}

function renderCanvas() {
  if (!canvas.value || !originalImage.value || !canvasWrap.value) return
  const ctx = canvas.value.getContext('2d')
  const wrapRect = canvasWrap.value.getBoundingClientRect()
  const maxW = wrapRect.width
  const img = originalImage.value
  const scale = Math.min(1, maxW / img.naturalWidth)
  const width = Math.floor(img.naturalWidth * scale)
  const height = Math.floor(img.naturalHeight * scale)

  canvas.value.width = width
  canvas.value.height = height
  canvas.value.style.width = width + 'px'
  canvas.value.style.height = height + 'px'
  ctx.drawImage(img, 0, 0, width, height)
}

function getCanvasPoint(e) {
  const rect = canvas.value.getBoundingClientRect()
  const clientX = e.touches ? e.touches[0].clientX : e.clientX
  const clientY = e.touches ? e.touches[0].clientY : e.clientY
  return {
    x: clientX - rect.left,
    y: clientY - rect.top
  }
}

function onMouseDown(e) {
  if (!canvas.value) return
  const p = getCanvasPoint(e)
  isSelecting.value = true
  selectionStart.value = p
  selection.value = { x: p.x, y: p.y, w: 0, h: 0 }
}

function onMouseMove(e) {
  if (!isSelecting.value || !canvas.value) return
  const p = getCanvasPoint(e)
  updateSelection(p)
}

function onMouseUp() {
  isSelecting.value = false
}

function onTouchStart(e) {
  onMouseDown(e)
}

function onTouchMove(e) {
  onMouseMove(e)
}

function onTouchEnd() {
  onMouseUp()
}

function updateSelection(end) {
  const start = selectionStart.value
  const x = Math.min(start.x, end.x)
  const y = Math.min(start.y, end.y)
  const w = Math.abs(end.x - start.x)
  const h = Math.abs(end.y - start.y)
  selection.value = { x, y, w, h }
}

function process() {
  if (!selection.value || !canvas.value) return
  processing.value = true

  // 简单模拟：把选区用周围采样色填充并轻微模糊
  setTimeout(() => {
    try {
      const ctx = canvas.value.getContext('2d')
      const { x, y, w, h } = selection.value
      if (w < 5 || h < 5) {
        message.error('选区太小，请重新框选')
        processing.value = false
        return
      }

      // 先重绘原图
      ctx.drawImage(originalImage.value, 0, 0, canvas.value.width, canvas.value.height)

      // 采样选区周围颜色
      const border = 4
      const sx = Math.max(0, x - border)
      const sy = Math.max(0, y - border)
      const sw = Math.min(canvas.value.width - sx, w + border * 2)
      const sh = Math.min(canvas.value.height - sy, h + border * 2)
      const borderData = ctx.getImageData(sx, sy, sw, sh)
      const avg = averageColor(borderData.data)

      // 填充选区
      ctx.fillStyle = `rgba(${avg.r},${avg.g},${avg.b},1)`
      ctx.fillRect(x, y, w, h)

      // 轻微模糊选区边缘
      blurRect(ctx, x, y, w, h, 2)

      resultUrl.value = canvas.value.toDataURL('image/png')
      selection.value = null
      message.success('处理完成')
    } catch (err) {
      message.error('处理失败，请重试')
    } finally {
      processing.value = false
    }
  }, 400)
}

function averageColor(data) {
  let r = 0, g = 0, b = 0, count = 0
  for (let i = 0; i < data.length; i += 4) {
    r += data[i]
    g += data[i + 1]
    b += data[i + 2]
    count++
  }
  return {
    r: Math.round(r / count),
    g: Math.round(g / count),
    b: Math.round(b / count)
  }
}

function blurRect(ctx, x, y, w, h, radius) {
  if (radius <= 0) return
  try {
    const imageData = ctx.getImageData(x, y, w, h)
    const data = imageData.data
    const width = imageData.width
    const height = imageData.height
    const output = new Uint8ClampedArray(data)

    for (let py = 0; py < height; py++) {
      for (let px = 0; px < width; px++) {
        let r = 0, g = 0, b = 0, count = 0
        for (let dy = -radius; dy <= radius; dy++) {
          for (let dx = -radius; dx <= radius; dx++) {
            const ny = py + dy
            const nx = px + dx
            if (ny >= 0 && ny < height && nx >= 0 && nx < width) {
              const idx = (ny * width + nx) * 4
              r += data[idx]
              g += data[idx + 1]
              b += data[idx + 2]
              count++
            }
          }
        }
        const idx = (py * width + px) * 4
        output[idx] = r / count
        output[idx + 1] = g / count
        output[idx + 2] = b / count
      }
    }

    for (let i = 0; i < data.length; i++) {
      data[i] = output[i]
    }
    ctx.putImageData(imageData, x, y)
  } catch (e) {
    // 模糊失败不影响主流程
  }
}

function resetImage() {
  if (imageUrl.value) URL.revokeObjectURL(imageUrl.value)
  imageFile.value = null
  imageUrl.value = ''
  originalImage.value = null
  selection.value = null
  resultUrl.value = ''
  if (fileInput.value) fileInput.value.value = ''
}

function resetAll() {
  resetImage()
}

function onDownload() {
  message.success('已开始下载')
}
</script>

<style scoped>
.mobile-watermark {
  min-height: 100vh;
  min-height: 100dvh;
  width: 100%;
  display: flex;
  flex-direction: column;
  background: #f8f9fa;
  color: #1a1a1a;
  -webkit-font-smoothing: antialiased;
}

/* 子页面返回头 */
.mw-subpage-header {
  display: flex;
  align-items: center;
  justify-content: center;
  position: sticky;
  top: 0;
  z-index: 50;
  width: 100%;
  height: 48px;
  padding: 0 12px;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid #f0f0f0;
}
.mw-subpage-back {
  position: absolute;
  left: 12px;
  display: flex;
  align-items: center;
  gap: 2px;
  color: #595959;
  font-size: 14px;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}
.mw-subpage-back svg {
  width: 20px;
  height: 20px;
}
.mw-subpage-title {
  width: 100%;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  padding: 0 60px;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

/* Hero */
.mw-hero {
  width: 100%;
  padding: 36px 20px 24px;
  background: linear-gradient(180deg, #FFF5F7 0%, #fff 100%);
  text-align: center;
}
.mw-hero__inner {
  max-width: 960px;
  margin: 0 auto;
}
.mw-hero__badge {
  display: inline-block;
  background: linear-gradient(135deg, #FFF0F2, #FFE4E8);
  color: #FF2442;
  font-size: 12px;
  font-weight: 600;
  padding: 5px 14px;
  border-radius: 16px;
  margin-bottom: 14px;
}
.mw-hero__title {
  font-size: 26px;
  font-weight: 800;
  margin-bottom: 8px;
  color: #1a1a1a;
}
.mw-hero__desc {
  font-size: 15px;
  color: #8c8c8c;
  margin: 0;
}

/* 工具区 */
.mw-tool {
  flex: 1 1 auto;
  width: 100%;
  padding: 20px 16px 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.mw-tool__inner {
  width: 100%;
  max-width: 960px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 上传 */
.mw-upload {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 40px 24px;
  background: #fff;
  border: 2px dashed #e0e0e0;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.mw-upload.dragging,
.mw-upload:active {
  border-color: #FF2442;
  background: #FFF5F7;
}
.mw-upload__input {
  display: none;
}
.mw-upload__icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #FFF0F2, #FFE4E8);
  color: #FF2442;
  display: flex;
  align-items: center;
  justify-content: center;
}
.mw-upload__icon svg {
  width: 26px;
  height: 26px;
}
.mw-upload__title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}
.mw-upload__tip {
  font-size: 13px;
  color: #8c8c8c;
}

/* 编辑器 */
.mw-editor {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.mw-editor__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.mw-editor__title {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a1a;
}
.mw-editor__reset {
  font-size: 13px;
  color: #FF2442;
  background: transparent;
  border: none;
  cursor: pointer;
}
.mw-editor__tip {
  font-size: 13px;
  color: #8c8c8c;
  line-height: 1.5;
}

/* Canvas */
.mw-canvas-wrap {
  position: relative;
  display: inline-block;
  max-width: 100%;
  border-radius: 12px;
  overflow: hidden;
  background: #f5f5f5;
  touch-action: none;
  align-self: center;
}
.mw-canvas {
  display: block;
  max-width: 100%;
}

/* 选区 */
.mw-selection {
  position: absolute;
  border: 2px dashed #FF2442;
  background: rgba(255, 36, 66, 0.12);
  border-radius: 4px;
  pointer-events: none;
}
.mw-selection__label {
  position: absolute;
  top: -20px;
  left: 0;
  font-size: 11px;
  color: #FF2442;
  background: #fff;
  padding: 1px 6px;
  border-radius: 4px;
  white-space: nowrap;
}

/* 按钮 */
.mw-action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  max-width: 400px;
  align-self: center;
  padding: 14px 0;
  border: none;
  border-radius: 24px;
  background: linear-gradient(135deg, #FF4D6F 0%, #FF2442 100%);
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(255, 36, 66, 0.3);
  -webkit-tap-highlight-color: transparent;
}
.mw-action-btn:hover {
  background: linear-gradient(135deg, #e61e3a 0%, #c91a33 100%);
}
.mw-action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.mw-action-btn:active:not(:disabled) {
  transform: scale(0.98);
}
.mw-action-btn--primary {
  text-decoration: none;
}
.mw-action-btn__spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: mw-spin 0.8s linear infinite;
}
@keyframes mw-spin {
  to { transform: rotate(360deg); }
}

/* 结果 */
.mw-result {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.mw-result__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.mw-result__title {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a1a;
}
.mw-result__img {
  width: 100%;
  border-radius: 12px;
  background: #f5f5f5;
  display: block;
}
.mw-reset-link {
  width: 100%;
  padding: 10px;
  background: transparent;
  border: none;
  color: #8c8c8c;
  font-size: 14px;
  cursor: pointer;
}

/* 暗色主题 */
body[data-theme="dark"] .mobile-watermark {
  background: #141414;
  color: #e0e0e0;
}
body[data-theme="dark"] .mw-subpage-header {
  background: rgba(20, 20, 20, 0.96);
  border-bottom-color: #2a2a2a;
}
body[data-theme="dark"] .mw-subpage-back {
  color: #a6a6a6;
}
body[data-theme="dark"] .mw-subpage-title {
  color: #e0e0e0;
}
body[data-theme="dark"] .mw-hero {
  background: linear-gradient(180deg, #2a1f22 0%, #141414 100%);
}
body[data-theme="dark"] .mw-hero__title,
body[data-theme="dark"] .mw-editor__title,
body[data-theme="dark"] .mw-result__title {
  color: #e0e0e0;
}
body[data-theme="dark"] .mw-hero__desc,
body[data-theme="dark"] .mw-editor__tip,
body[data-theme="dark"] .mw-upload__tip,
body[data-theme="dark"] .mw-reset-link {
  color: #a6a6a6;
}
body[data-theme="dark"] .mw-upload,
body[data-theme="dark"] .mw-editor,
body[data-theme="dark"] .mw-result {
  background: #1f1f1f;
}
body[data-theme="dark"] .mw-upload.dragging,
body[data-theme="dark"] .mw-upload:active {
  background: rgba(255, 36, 66, 0.15);
}
body[data-theme="dark"] .mw-canvas-wrap {
  background: #2a2a2a;
}
body[data-theme="dark"] .mw-selection__label {
  background: #1f1f1f;
}
</style>
