<template>
  <div class="watermark-remove-tool">
    <a-alert
      message="使用说明"
      description="上传图片后在画布上拖动框选水印区域，点击去除水印即可用周围颜色填充并模糊边缘。所有处理均在浏览器本地完成，不会上传到服务器。"
      type="info"
      show-icon
      style="margin-bottom: 16px"
    />

    <a-row :gutter="24">
      <a-col :span="12">
        <a-form layout="vertical">
          <a-form-item label="选择图片">
            <a-upload-dragger
              :show-upload-list="false"
              :before-upload="handleBeforeUpload"
              accept="image/png,image/jpeg,image/jpg,image/webp"
            >
              <p class="ant-upload-drag-icon">
                <PictureOutlined />
              </p>
              <p class="ant-upload-text">点击或拖拽图片到此处</p>
              <p class="ant-upload-hint">支持 PNG、JPG、WebP，单张不超过 20MB</p>
            </a-upload-dragger>
          </a-form-item>

          <a-form-item>
            <a-space>
              <a-button
                type="primary"
                :disabled="!selection || processing"
                :loading="processing"
                @click="handleProcess"
              >
                去除水印
              </a-button>
              <a-button v-if="imageUrl" danger @click="handleClear">重新上传</a-button>
            </a-space>
          </a-form-item>
        </a-form>

        <div v-if="originalFile" class="info-panel">
          <div>原图尺寸：{{ originalWidth }} × {{ originalHeight }}</div>
          <div>原图大小：{{ formatSize(originalFile.size) }}</div>
        </div>
      </a-col>

      <a-col :span="12">
        <div v-if="imageUrl" class="canvas-panel">
          <div class="panel-title">
            {{ resultUrl ? '原图' : '框选水印区域（在图片上拖动）' }}
          </div>
          <div
            ref="canvasWrap"
            class="canvas-wrap"
            @mousedown="onMouseDown"
            @mousemove="onMouseMove"
            @mouseup="onMouseUp"
            @mouseleave="onMouseUp"
          >
            <canvas ref="canvas" class="canvas"></canvas>
            <div
              v-if="selection && !resultUrl"
              class="selection-box"
              :style="selectionStyle"
            >
              <span class="selection-label">水印区域</span>
            </div>
          </div>
        </div>
        <a-empty v-else description="请先上传图片" />

        <div v-if="resultUrl" class="result-panel">
          <div class="panel-title">处理结果</div>
          <img :src="resultUrl" class="result-image" alt="处理结果" />
          <a-button type="primary" style="margin-top: 12px" @click="handleDownload"
            >下载结果</a-button
          >
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import { PictureOutlined } from '@ant-design/icons-vue'

const canvas = ref(null)
const canvasWrap = ref(null)

const originalFile = ref(null)
const originalImage = ref(null)
const imageUrl = ref('')
const originalWidth = ref(0)
const originalHeight = ref(0)

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

function formatSize(bytes) {
  if (!bytes) return '0 B'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(2)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
}

function handleBeforeUpload(file) {
  if (!file.type.startsWith('image/')) {
    message.warning('请选择图片文件')
    return false
  }
  if (file.size > 20 * 1024 * 1024) {
    message.warning('图片大小不能超过 20MB')
    return false
  }

  releaseUrls()
  originalFile.value = file
  imageUrl.value = URL.createObjectURL(file)
  resultUrl.value = ''
  selection.value = null

  nextTick(() => loadImageToCanvas())
  return false
}

function loadImageToCanvas() {
  if (!canvas.value || !imageUrl.value) return
  const img = new Image()
  img.onload = () => {
    originalImage.value = img
    originalWidth.value = img.naturalWidth
    originalHeight.value = img.naturalHeight
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
  canvas.value.style.width = `${width}px`
  canvas.value.style.height = `${height}px`
  ctx.drawImage(img, 0, 0, width, height)
}

function getCanvasPoint(e) {
  const rect = canvas.value.getBoundingClientRect()
  return {
    x: e.clientX - rect.left,
    y: e.clientY - rect.top
  }
}

function onMouseDown(e) {
  if (!canvas.value || resultUrl.value) return
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

function updateSelection(end) {
  const start = selectionStart.value
  const x = Math.min(start.x, end.x)
  const y = Math.min(start.y, end.y)
  const w = Math.abs(end.x - start.x)
  const h = Math.abs(end.y - start.y)
  selection.value = { x, y, w, h }
}

function handleProcess() {
  if (!selection.value || !canvas.value || !originalImage.value) return

  const { x, y, w, h } = selection.value
  if (w < 5 || h < 5) {
    message.warning('选区太小，请重新框选')
    return
  }

  processing.value = true
  setTimeout(() => {
    try {
      const ctx = canvas.value.getContext('2d')
      ctx.drawImage(originalImage.value, 0, 0, canvas.value.width, canvas.value.height)

      const border = 4
      const sx = Math.max(0, x - border)
      const sy = Math.max(0, y - border)
      const sw = Math.min(canvas.value.width - sx, w + border * 2)
      const sh = Math.min(canvas.value.height - sy, h + border * 2)
      const borderData = ctx.getImageData(sx, sy, sw, sh)
      const avg = averageColor(borderData.data)

      ctx.fillStyle = `rgba(${avg.r},${avg.g},${avg.b},1)`
      ctx.fillRect(x, y, w, h)

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
  let r = 0
  let g = 0
  let b = 0
  let count = 0
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
        let r = 0
        let g = 0
        let b = 0
        let count = 0
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

function handleDownload() {
  if (!resultUrl.value || !originalFile.value) return
  const link = document.createElement('a')
  link.href = resultUrl.value
  const name = originalFile.value.name.replace(/\.[^.]+$/, '') || 'image'
  link.download = `${name}_no_watermark.png`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

function handleClear() {
  releaseUrls()
  originalFile.value = null
  originalImage.value = null
  imageUrl.value = ''
  originalWidth.value = 0
  originalHeight.value = 0
  selection.value = null
  isSelecting.value = false
  resultUrl.value = ''
}

function releaseUrls() {
  if (imageUrl.value) {
    URL.revokeObjectURL(imageUrl.value)
  }
}

onUnmounted(releaseUrls)
</script>

<style scoped>
.watermark-remove-tool {
  max-width: 1200px;
}
.info-panel {
  background: #fafafa;
  border-radius: 8px;
  padding: 12px 16px;
  font-size: 13px;
  color: #595959;
  line-height: 1.8;
}
.canvas-panel,
.result-panel {
  margin-bottom: 16px;
}
.panel-title {
  font-weight: 500;
  margin-bottom: 8px;
  color: #262626;
}
.canvas-wrap {
  position: relative;
  display: inline-block;
  max-width: 100%;
  border-radius: 8px;
  overflow: hidden;
  background-image:
    linear-gradient(45deg, #e0e0e0 25%, transparent 25%),
    linear-gradient(-45deg, #e0e0e0 25%, transparent 25%),
    linear-gradient(45deg, transparent 75%, #e0e0e0 75%),
    linear-gradient(-45deg, transparent 75%, #e0e0e0 75%);
  background-size: 16px 16px;
  background-position: 0 0, 0 8px, 8px -8px, -8px 0;
  background-color: #ffffff;
}
.canvas {
  display: block;
  max-width: 100%;
  cursor: crosshair;
}
.selection-box {
  position: absolute;
  border: 2px dashed #ff4d4f;
  background: rgba(255, 77, 79, 0.12);
  border-radius: 4px;
  pointer-events: none;
}
.selection-label {
  position: absolute;
  top: -20px;
  left: 0;
  font-size: 12px;
  color: #ff4d4f;
  background: #fff;
  padding: 1px 6px;
  border-radius: 4px;
  white-space: nowrap;
}
.result-image {
  max-width: 100%;
  border-radius: 8px;
  display: block;
}
</style>
