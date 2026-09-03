<template>
  <div class="mobile-watermark">
    <!-- 子页面返回头 -->
    <MobileSubpageHeader title="AI 去/加水印" />

    <!-- 宣传文案 -->
    <section class="mw-hero">
      <div class="mw-hero__inner">
        <div class="mw-hero__badge">AI 去/加水印</div>
        <h1 class="mw-hero__title">
          {{ mode === 'remove' ? '框选水印，一键消除' : '添加专属水印，保护作品' }}
        </h1>
        <p class="mw-hero__desc">
          {{ mode === 'remove'
            ? '本地处理、保护隐私，快速去除图片中的水印与杂物'
            : '自定义文字、位置与透明度，一键为图片添加个人水印' }}
        </p>
      </div>
    </section>

    <!-- 工具区 -->
    <main class="mw-tool">
      <div class="mw-tool__inner">
        <!-- 模式切换 -->
        <div class="mw-mode-tabs">
          <button
            :class="['mw-mode-tab', { active: mode === 'remove' }]"
            @click="setMode('remove')"
          >
            去水印
          </button>
          <button
            :class="['mw-mode-tab', { active: mode === 'add' }]"
            @click="setMode('add')"
          >
            加水印
          </button>
        </div>

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
            <span class="mw-editor__title">
              {{ mode === 'remove' ? '框选水印区域' : '设置水印参数' }}
            </span>
            <button class="mw-editor__reset" @click="resetImage">重新上传</button>
          </div>

          <div
            ref="canvasWrap"
            class="mw-canvas-wrap"
            :onMousedown="mode === 'remove' ? onMouseDown : undefined"
            :onMousemove="mode === 'remove' ? onMouseMove : undefined"
            :onMouseup="mode === 'remove' ? onMouseUp : undefined"
            :onMouseleave="mode === 'remove' ? onMouseUp : undefined"
            :onTouchstart="mode === 'remove' ? onTouchStart : undefined"
            :onTouchmove="mode === 'remove' ? onTouchMove : undefined"
            :onTouchend="mode === 'remove' ? onTouchEnd : undefined"
          >
            <canvas ref="canvas" class="mw-canvas"></canvas>
            <div
              v-if="mode === 'remove' && selection"
              class="mw-selection"
              :style="selectionStyle"
            >
              <span class="mw-selection__label">水印区域</span>
            </div>
          </div>

          <!-- 去水印提示与按钮 -->
          <template v-if="mode === 'remove'">
            <div class="mw-editor__tip">在图片上拖动框选需要去除的水印，然后点击处理</div>
            <button
              class="mw-action-btn"
              :disabled="!selection || processing"
              @click="processRemove"
            >
              <span v-if="processing" class="mw-action-btn__spinner"></span>
              <span>{{ processing ? 'AI 处理中…' : '开始去水印' }}</span>
            </button>
          </template>

          <!-- 加水印表单 -->
          <template v-else>
            <div class="mw-add-form">
              <div class="mw-form-item">
                <label>水印文字</label>
                <input
                  v-model="watermarkText"
                  type="text"
                  maxlength="30"
                  placeholder="例如：@爱创作"
                />
              </div>

              <div class="mw-form-row">
                <div class="mw-form-item">
                  <label>位置</label>
                  <select v-model="watermarkPosition">
                    <option value="bottom-right">右下角</option>
                    <option value="bottom-left">左下角</option>
                    <option value="top-right">右上角</option>
                    <option value="top-left">左上角</option>
                    <option value="center">居中</option>
                    <option value="tile">平铺</option>
                  </select>
                </div>

                <div class="mw-form-item mw-form-item--color">
                  <label>颜色</label>
                  <div class="mw-color-options">
                    <button
                      v-for="c in watermarkColors"
                      :key="c.value"
                      type="button"
                      :class="{ active: watermarkColor === c.value }"
                      :style="{ background: c.value }"
                      :title="c.label"
                      @click="watermarkColor = c.value"
                    ></button>
                  </div>
                </div>
              </div>

              <div class="mw-form-item">
                <label>字号 {{ watermarkFontSize }}px</label>
                <input
                  v-model.number="watermarkFontSize"
                  type="range"
                  min="12"
                  max="120"
                />
              </div>

              <div class="mw-form-item">
                <label>透明度 {{ Math.round(watermarkOpacity * 100) }}%</label>
                <input
                  v-model.number="watermarkOpacity"
                  type="range"
                  min="0.05"
                  max="1"
                  step="0.05"
                />
              </div>

              <button
                class="mw-action-btn"
                :disabled="!watermarkText.trim() || processing"
                @click="processAdd"
              >
                <span v-if="processing" class="mw-action-btn__spinner"></span>
                <span>{{ processing ? '生成中…' : '生成带水印图片' }}</span>
              </button>
            </div>
          </template>
        </div>

        <!-- 结果 -->
        <div v-if="resultUrl" class="mw-result">
          <div class="mw-result__header">
            <span class="mw-result__title">处理结果</span>
          </div>
          <img :src="resultUrl" alt="处理结果" class="mw-result__img" />
          <a
            :href="resultUrl"
            :download="`爱创作工坊${mode === 'remove' ? '去水印' : '加水印'}.png`"
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
import { ref, computed, nextTick, watch } from 'vue'
import { message } from 'ant-design-vue'
import MobileToolFooter from '@/components/common/MobileToolFooter.vue'
import MobileSubpageHeader from '@/components/common/MobileSubpageHeader.vue'

const fileInput = ref(null)
const canvas = ref(null)
const canvasWrap = ref(null)
const dragging = ref(false)

const mode = ref('remove')
const imageFile = ref(null)
const imageUrl = ref('')
const originalImage = ref(null)
const selection = ref(null)
const isSelecting = ref(false)
const selectionStart = ref({ x: 0, y: 0 })
const processing = ref(false)
const resultUrl = ref('')

const watermarkText = ref('@爱创作')
const watermarkPosition = ref('bottom-right')
const watermarkFontSize = ref(24)
const watermarkOpacity = ref(0.5)
const watermarkColor = ref('#ffffff')
const watermarkColors = [
  { value: '#ffffff', label: '白色' },
  { value: '#000000', label: '黑色' },
  { value: '#ff2442', label: '红色' },
  { value: '#8c8c8c', label: '灰色' }
]

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

function setMode(next) {
  if (mode.value === next) return
  mode.value = next
  selection.value = null
  isSelecting.value = false
  resultUrl.value = ''
  nextTick(() => {
    if (originalImage.value) {
      renderCanvas()
    }
  })
}

watch([watermarkText, watermarkPosition, watermarkFontSize, watermarkOpacity, watermarkColor], () => {
  if (mode.value === 'add' && originalImage.value) {
    renderCanvas()
  }
})

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

  if (mode.value === 'add') {
    drawWatermark(ctx, width, height)
  }
}

function drawWatermark(ctx, width, height) {
  const text = watermarkText.value.trim()
  if (!text) return

  const fontSize = Math.max(12, Math.min(120, watermarkFontSize.value))
  ctx.save()
  ctx.font = `${fontSize}px -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif`
  ctx.globalAlpha = Math.max(0.05, Math.min(1, watermarkOpacity.value))
  ctx.fillStyle = watermarkColor.value
  ctx.textBaseline = 'middle'

  const padding = Math.max(12, fontSize * 0.5)
  const metrics = ctx.measureText(text)
  const textWidth = metrics.width
  const textHeight = fontSize

  const positions = {
    'bottom-right': [
      { x: width - textWidth - padding, y: height - textHeight }
    ],
    'bottom-left': [
      { x: padding, y: height - textHeight }
    ],
    'top-right': [
      { x: width - textWidth - padding, y: textHeight }
    ],
    'top-left': [
      { x: padding, y: textHeight }
    ],
    'center': [
      { x: (width - textWidth) / 2, y: height / 2 }
    ]
  }

  if (watermarkPosition.value === 'tile') {
    const gapX = textWidth + padding * 2
    const gapY = textHeight + padding * 2
    ctx.translate(width / 2, height / 2)
    ctx.rotate(-Math.PI / 6)
    const startX = -width
    const startY = -height
    const endX = width
    const endY = height
    for (let y = startY; y < endY; y += gapY) {
      for (let x = startX; x < endX; x += gapX) {
        ctx.fillText(text, x, y)
      }
    }
  } else {
    const list = positions[watermarkPosition.value] || positions['bottom-right']
    list.forEach(p => ctx.fillText(text, p.x, p.y))
  }

  ctx.restore()
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
  if (!canvas.value || mode.value !== 'remove') return
  const p = getCanvasPoint(e)
  isSelecting.value = true
  selectionStart.value = p
  selection.value = { x: p.x, y: p.y, w: 0, h: 0 }
}

function onMouseMove(e) {
  if (!isSelecting.value || !canvas.value || mode.value !== 'remove') return
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

function processRemove() {
  if (!selection.value || !canvas.value || mode.value !== 'remove') return
  processing.value = true

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

function processAdd() {
  if (!canvas.value || mode.value !== 'add') return
  const text = watermarkText.value.trim()
  if (!text) {
    message.error('请输入水印文字')
    return
  }
  processing.value = true
  setTimeout(() => {
    try {
      resultUrl.value = canvas.value.toDataURL('image/png')
      message.success('水印已生成')
    } catch (err) {
      message.error('生成失败，请重试')
    } finally {
      processing.value = false
    }
  }, 300)
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

/* 模式切换 */
.mw-mode-tabs {
  display: flex;
  gap: 8px;
  background: #fff;
  padding: 4px;
  border-radius: 24px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
}
.mw-mode-tab {
  flex: 1 1 0;
  padding: 10px 0;
  border: none;
  border-radius: 20px;
  background: transparent;
  color: #8c8c8c;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  transition: all 0.2s ease;
}
.mw-mode-tab.active {
  background: linear-gradient(135deg, #FF4D6F 0%, #FF2442 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(255, 36, 66, 0.25);
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

/* 加水印表单 */
.mw-add-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.mw-form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.mw-form-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.mw-form-item label {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
}
.mw-form-item input[type="text"],
.mw-form-item select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  font-size: 14px;
  background: #fff;
  color: #1a1a1a;
  outline: none;
  transition: border-color 0.2s ease;
}
.mw-form-item input[type="text"]:focus,
.mw-form-item select:focus {
  border-color: #FF2442;
}
.mw-form-item input[type="range"] {
  width: 100%;
  accent-color: #FF2442;
}
.mw-form-item--color .mw-color-options {
  display: flex;
  gap: 8px;
}
.mw-color-options button {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid transparent;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  transition: transform 0.2s ease;
}
.mw-color-options button.active {
  border-color: #FF2442;
  transform: scale(1.1);
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
body[data-theme="dark"] .mw-hero {
  background: linear-gradient(180deg, #2a1f22 0%, #141414 100%);
}
body[data-theme="dark"] .mw-hero__title,
body[data-theme="dark"] .mw-editor__title,
body[data-theme="dark"] .mw-result__title,
body[data-theme="dark"] .mw-form-item label,
body[data-theme="dark"] .mw-upload__title {
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
body[data-theme="dark"] .mw-result,
body[data-theme="dark"] .mw-mode-tabs {
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
body[data-theme="dark"] .mw-form-item input[type="text"],
body[data-theme="dark"] .mw-form-item select {
  background: #2a2a2a;
  border-color: #3a3a3a;
  color: #e0e0e0;
}
body[data-theme="dark"] .mw-mode-tab {
  color: #a6a6a6;
}
body[data-theme="dark"] .mw-mode-tab.active {
  color: #fff;
}
</style>
