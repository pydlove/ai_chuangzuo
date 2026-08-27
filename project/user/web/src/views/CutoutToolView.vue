<template>
  <div class="cutout-tool-page">
    <!-- 子页面返回头：与二维码生成等二级页保持一致 -->
    <header class="cutout-subpage-header">
      <div class="cutout-subpage-back" @click="goBack">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6"></polyline>
        </svg>
        <span>返回</span>
      </div>
      <div class="cutout-subpage-title">AI 抠图</div>
    </header>

    <header class="cutout-hero">
      <div class="cutout-hero-deco cutout-hero-deco-lg"></div>
      <div class="cutout-hero-deco cutout-hero-deco-sm"></div>
      <div class="cutout-hero-inner">
        <div class="cutout-hero-text">
          <span class="cutout-hero-badge">AI 抠图</span>
          <h1 class="cutout-hero-title">一键抠图，背景透明</h1>
          <p class="cutout-hero-subtitle">上传图片，快速去除背景，下载透明 PNG</p>
        </div>
      </div>
    </header>

    <main class="cutout-main">
      <div class="cutout-card">
        <div class="cutout-workspace">
          <!-- 左侧：上传与原图 -->
          <div class="cutout-panel">
            <div class="cutout-panel-head">
              <span class="cutout-panel-title">原图</span>
              <button class="cutout-btn cutout-btn--primary" @click="triggerUpload">
                <UploadOutlined />
                上传图片
              </button>
              <input
                ref="fileInput"
                type="file"
                accept="image/*"
                class="cutout-file-input"
                @change="handleFileChange"
              />
            </div>
            <div
              class="cutout-canvas-wrap"
              :class="{ 'is-empty': !originalSrc }"
              @click="!originalSrc && triggerUpload()"
              @dragover.prevent="dragOver = true"
              @dragleave.prevent="dragOver = false"
              @drop.prevent="handleDrop"
            >
              <template v-if="originalSrc">
                <canvas ref="sourceCanvas" class="cutout-canvas" @click.stop="handleCanvasClick"></canvas>
                <div class="cutout-canvas-tip">点击图片可重新选择背景取样点</div>
              </template>
              <div v-else class="cutout-upload-placeholder" :class="{ 'is-drag': dragOver }">
                <InboxOutlined class="cutout-upload-icon" />
                <div class="cutout-upload-title">
                  <span class="upload-title-desktop">点击或拖拽图片到此处</span>
                  <span class="upload-title-mobile">点击上传图片</span>
                </div>
                <div class="cutout-upload-desc">支持 JPG、PNG 格式，建议主体清晰、背景纯色</div>
              </div>
            </div>
          </div>

          <!-- 右侧：结果 -->
          <div class="cutout-panel">
            <div class="cutout-panel-head">
              <span class="cutout-panel-title">抠图结果</span>
              <div class="cutout-panel-actions">
                <button
                  class="cutout-btn cutout-btn--primary"
                  :disabled="!originalSrc || processing"
                  @click="doCutout"
                >
                  <ScissorOutlined />
                  {{ processing ? '处理中' : '抠图' }}
                </button>
                <button class="cutout-btn" :disabled="!resultSrc" @click="downloadResult">
                  <DownloadOutlined />
                  下载
                </button>
              </div>
            </div>
            <div
              class="cutout-canvas-wrap cutout-canvas-wrap--result"
              :class="{ 'is-empty': !resultSrc }"
            >
              <canvas v-show="resultSrc" ref="resultCanvas" class="cutout-canvas"></canvas>
              <button
                v-if="resultSrc"
                class="cutout-result-close"
                aria-label="清空结果"
                @click="clearResult"
              >
                ×
              </button>
              <div v-if="!resultSrc" class="cutout-result-placeholder">
                <PictureOutlined class="cutout-upload-icon" />
                <div class="cutout-upload-title">处理后的图片将显示在这里</div>
                <div class="cutout-upload-desc">点击上方「抠图」按钮开始处理</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 工具参数 -->
        <div v-if="originalSrc" class="cutout-controls">
          <div class="cutout-control-row">
            <span class="cutout-control-label">背景取样色</span>
            <div class="cutout-color-wrap">
              <input
                v-model="sampleColor"
                type="color"
                class="cutout-color-input"
                @change="onColorInputChange"
              />
              <span class="cutout-color-value">{{ sampleColor }}</span>
            </div>
            <button class="cutout-btn cutout-btn--small" @click="autoPickBackground">自动取样</button>
          </div>
          <div class="cutout-control-row">
            <span class="cutout-control-label">容差</span>
            <a-slider v-model:value="tolerance" :min="0" :max="120" class="cutout-slider" />
            <span class="cutout-control-value">{{ tolerance }}</span>
          </div>
          <div class="cutout-control-row">
            <span class="cutout-control-label">边缘羽化</span>
            <a-slider v-model:value="feather" :min="0" :max="8" class="cutout-slider" />
            <span class="cutout-control-value">{{ feather }}px</span>
          </div>
          <div class="cutout-control-tip">
            提示：上传图片后点击原图上的背景区域可手动选取背景色，调整容差可控制去除范围。
          </div>
        </div>
      </div>
    </main>

    <MobileToolFooter />
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import {
  UploadOutlined,
  DownloadOutlined,
  ScissorOutlined,
  InboxOutlined,
  PictureOutlined
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import MobileToolFooter from '@/components/common/MobileToolFooter.vue'

const router = useRouter()

const fileInput = ref(null)
const sourceCanvas = ref(null)
const resultCanvas = ref(null)

const originalSrc = ref('')
const resultSrc = ref('')
const processing = ref(false)
const dragOver = ref(false)

const sampleColor = ref('#ffffff')
const tolerance = ref(45)
const feather = ref(2)

let sourceImage = null
let sourceCtx = null
let resultCtx = null

function goBack() {
  router.back()
}

function triggerUpload() {
  fileInput.value?.click()
}

function handleFileChange(e) {
  const file = e.target.files?.[0]
  if (file) loadFile(file)
  e.target.value = ''
}

function handleDrop(e) {
  dragOver.value = false
  const file = e.dataTransfer.files?.[0]
  if (file && file.type.startsWith('image/')) {
    loadFile(file)
  } else {
    message.error('请上传图片文件')
  }
}

function loadFile(file) {
  if (!file.type.startsWith('image/')) {
    message.error('仅支持图片文件')
    return
  }
  if (file.size > 20 * 1024 * 1024) {
    message.error('图片大小不能超过 20MB')
    return
  }

  // 重新上传时清空上一次结果
  if (resultSrc.value) clearResult()

  const reader = new FileReader()
  reader.onload = (e) => {
    originalSrc.value = e.target.result
    nextTick(() => initSourceCanvas())
  }
  reader.readAsDataURL(file)
}

function initSourceCanvas() {
  if (!sourceCanvas.value || !originalSrc.value) return
  const canvas = sourceCanvas.value
  sourceCtx = canvas.getContext('2d', { willReadFrequently: true })

  sourceImage = new Image()
  sourceImage.crossOrigin = 'anonymous'
  sourceImage.onload = () => {
    const maxWidth = 720
    const scale = Math.min(1, maxWidth / sourceImage.width)
    canvas.width = Math.floor(sourceImage.width * scale)
    canvas.height = Math.floor(sourceImage.height * scale)
    sourceCtx.drawImage(sourceImage, 0, 0, canvas.width, canvas.height)
    autoPickBackground()
  }
  sourceImage.src = originalSrc.value
}

function handleCanvasClick(e) {
  const canvas = sourceCanvas.value
  if (!canvas || !sourceCtx) return
  const rect = canvas.getBoundingClientRect()
  const scaleX = canvas.width / rect.width
  const scaleY = canvas.height / rect.height
  const x = Math.floor((e.clientX - rect.left) * scaleX)
  const y = Math.floor((e.clientY - rect.top) * scaleY)
  const pixel = sourceCtx.getImageData(x, y, 1, 1).data
  sampleColor.value = rgbToHex(pixel[0], pixel[1], pixel[2])
}

function autoPickBackground() {
  if (!sourceCtx || !sourceCanvas.value) return
  const canvas = sourceCanvas.value
  // 取四个角的颜色均值作为背景样本
  const sampleSize = 10
  const samples = []
  const corners = [
    [0, 0],
    [canvas.width - sampleSize, 0],
    [0, canvas.height - sampleSize],
    [canvas.width - sampleSize, canvas.height - sampleSize]
  ]
  corners.forEach(([x, y]) => {
    if (x >= 0 && y >= 0) {
      samples.push(sourceCtx.getImageData(x, y, sampleSize, sampleSize).data)
    }
  })
  const avg = averageColor(samples)
  sampleColor.value = rgbToHex(avg[0], avg[1], avg[2])
}

function averageColor(samples) {
  let r = 0, g = 0, b = 0, count = 0
  samples.forEach((data) => {
    for (let i = 0; i < data.length; i += 4) {
      r += data[i]
      g += data[i + 1]
      b += data[i + 2]
      count++
    }
  })
  return count ? [Math.round(r / count), Math.round(g / count), Math.round(b / count)] : [255, 255, 255]
}

function rgbToHex(r, g, b) {
  return '#' + [r, g, b].map((v) => v.toString(16).padStart(2, '0')).join('')
}

function hexToRgb(hex) {
  const n = parseInt(hex.replace('#', ''), 16)
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255]
}

function onColorInputChange() {
  // 颜色改变后可自动重跑，如果需要的话
}

function colorDistance(c1, c2) {
  // 简单的欧氏距离，感知上更均匀的 RGB 距离
  const r = (c1[0] + c2[0]) / 2
  const dr = c1[0] - c2[0]
  const dg = c1[1] - c2[1]
  const db = c1[2] - c2[2]
  return Math.sqrt((2 + r / 256) * dr * dr + 4 * dg * dg + (2 + (255 - r) / 256) * db * db)
}

function doCutout() {
  if (!sourceCtx || !sourceCanvas.value || !resultCanvas.value) return
  processing.value = true

  // 使用 setTimeout 让 loading 状态先渲染
  setTimeout(() => {
    try {
      const canvas = sourceCanvas.value
      const width = canvas.width
      const height = canvas.height
      const srcData = sourceCtx.getImageData(0, 0, width, height)
      const dstData = sourceCtx.createImageData(width, height)
      const src = srcData.data
      const dst = dstData.data

      const bg = hexToRgb(sampleColor.value)
      const tol = tolerance.value
      const f = feather.value

      for (let y = 0; y < height; y++) {
        for (let x = 0; x < width; x++) {
          const idx = (y * width + x) * 4
          const r = src[idx]
          const g = src[idx + 1]
          const b = src[idx + 2]
          const a = src[idx + 3]

          const dist = colorDistance([r, g, b], bg)
          let alpha = a

          if (dist <= tol) {
            alpha = 0
          } else if (f > 0 && dist <= tol + f * 25) {
            // 边缘羽化过渡
            alpha = Math.min(255, Math.max(0, ((dist - tol) / (f * 25)) * a))
          }

          dst[idx] = r
          dst[idx + 1] = g
          dst[idx + 2] = b
          dst[idx + 3] = Math.round(alpha)
        }
      }

      const out = resultCanvas.value
      out.width = width
      out.height = height
      resultCtx = out.getContext('2d')
      resultCtx.putImageData(dstData, 0, 0)
      resultSrc.value = out.toDataURL('image/png')
      message.success('抠图完成')
    } catch (e) {
      message.error('抠图失败：' + (e?.message || '未知错误'))
    } finally {
      processing.value = false
    }
  }, 50)
}

function downloadResult() {
  if (!resultSrc.value) return
  const a = document.createElement('a')
  a.href = resultSrc.value
  a.download = 'cutout-' + Date.now() + '.png'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}

function clearResult() {
  resultSrc.value = ''
  if (resultCanvas.value) {
    const ctx = resultCanvas.value.getContext('2d')
    ctx.clearRect(0, 0, resultCanvas.value.width, resultCanvas.value.height)
  }
}
</script>

<style scoped>
.cutout-tool-page {
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  background: #f8f9fa;
  color: #1a1a1a;
  -webkit-font-smoothing: antialiased;
}

/* 子页面返回头 */
.cutout-subpage-header {
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
  box-sizing: border-box;
}
.cutout-subpage-back {
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
.cutout-subpage-back svg {
  width: 20px;
  height: 20px;
}
.cutout-subpage-title {
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

/* Hero 区 */
.cutout-hero {
  position: relative;
  background: linear-gradient(180deg, #FFF5F7 0%, #FFFFFF 100%);
  padding: 32px 24px;
  overflow: hidden;
}
.cutout-hero-inner {
  max-width: 1200px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: 24px;
}
.cutout-hero-text { max-width: 560px; }
.cutout-hero-badge {
  display: inline-block;
  background: linear-gradient(135deg, #FFF0F2, #FFE4E8);
  color: #FF2442;
  font-size: 12px;
  font-weight: 600;
  padding: 5px 14px;
  border-radius: 16px;
  margin-bottom: 14px;
}
.cutout-hero-title {
  font-size: 32px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
}
.cutout-hero-subtitle {
  font-size: 14px;
  color: #8c8c8c;
  margin: 8px 0 0;
}
.cutout-hero-deco {
  position: absolute;
  border-radius: 50%;
  background: #FFE8EC;
}
.cutout-hero-deco-lg {
  width: 200px; height: 200px;
  top: -60px; right: -40px;
}
.cutout-hero-deco-sm {
  width: 80px; height: 80px;
  top: 20px; right: 160px;
}
@media (max-width: 768px) {
  .cutout-hero { padding: 24px 16px; text-align: center; }
  .cutout-hero-inner { flex-direction: column; align-items: center; }
  .cutout-hero-title { font-size: 26px; }
  .cutout-hero-subtitle { font-size: 13px; }
  .cutout-hero-image { width: 140px; height: 100px; }
}

/* 主内容 */
.cutout-main {
  flex: 1;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 24px 16px;
}
.cutout-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}
@media (max-width: 768px) {
  .cutout-card { padding: 16px; }
  .cutout-main { padding: 16px; }
}

.cutout-workspace {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}
@media (max-width: 991px) {
  .cutout-workspace { grid-template-columns: 1fr; }
}

.cutout-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.cutout-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.cutout-panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}
.cutout-panel-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 自定义按钮（红色主色调） */
.cutout-btn {
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
.cutout-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.cutout-btn:not(:disabled):hover {
  border-color: #FF2442;
  color: #FF2442;
}
.cutout-btn--primary {
  background: linear-gradient(135deg, #FF4D6F 0%, #FF2442 100%);
  border-color: transparent;
  color: #fff;
  box-shadow: 0 4px 12px rgba(255, 36, 66, 0.25);
}
.cutout-btn--primary:not(:disabled):hover {
  background: linear-gradient(135deg, #e61e3a 0%, #c91a33 100%);
  color: #fff;
}
.cutout-btn--small {
  padding: 4px 10px;
  font-size: 12px;
}

.cutout-file-input {
  display: none;
}

.cutout-canvas-wrap {
  flex: 1;
  min-height: 360px;
  border: 1px dashed #d9d9d9;
  border-radius: 12px;
  background: #fafafa;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
  transition: border-color 0.2s, background 0.2s;
}
.cutout-canvas-wrap.is-empty {
  cursor: pointer;
}
.cutout-canvas-wrap.is-empty:hover,
.cutout-canvas-wrap.is-empty.is-drag {
  border-color: #FF2442;
  background: #FFF5F7;
}
.cutout-canvas-wrap--result {
  background:
    conic-gradient(#e8e8e8 25%, #fff 0 50%, #e8e8e8 0 75%, #fff 0);
  background-size: 20px 20px;
}
.cutout-canvas-wrap--result.is-empty {
  background: #fafafa;
}
.cutout-result-close {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 20px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 2;
  transition: background 0.2s;
}
.cutout-result-close:hover {
  background: rgba(0, 0, 0, 0.65);
}
.cutout-canvas {
  max-width: 100%;
  max-height: 480px;
  display: block;
}
.cutout-canvas-tip {
  position: absolute;
  bottom: 8px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 12px;
  color: #8c8c8c;
  background: rgba(255,255,255,0.9);
  padding: 2px 10px;
  border-radius: 9999px;
  pointer-events: none;
}
.cutout-upload-placeholder,
.cutout-result-placeholder {
  text-align: center;
  padding: 24px;
}
.cutout-upload-icon {
  font-size: 48px;
  color: #d9d9d9;
  margin-bottom: 12px;
}
.cutout-upload-title {
  font-size: 15px;
  color: #262626;
  margin-bottom: 4px;
}
.upload-title-mobile { display: none; }
@media (max-width: 768px) {
  .upload-title-desktop { display: none; }
  .upload-title-mobile { display: inline; }
}
.cutout-upload-desc {
  font-size: 12px;
  color: #8c8c8c;
}

/* 控制栏 */
.cutout-controls {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.cutout-control-row {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.cutout-control-label {
  font-size: 14px;
  color: #262626;
  width: 80px;
  flex-shrink: 0;
}
.cutout-control-value {
  font-size: 14px;
  color: #8c8c8c;
  width: 48px;
  text-align: right;
}
.cutout-color-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}
.cutout-color-input {
  width: 36px;
  height: 36px;
  padding: 0;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  background: none;
}
.cutout-color-value {
  font-size: 13px;
  color: #595959;
  font-family: monospace;
}
.cutout-slider {
  flex: 1;
  min-width: 120px;
  max-width: 320px;
}
.cutout-slider :deep(.ant-slider-track) {
  background: #FF2442;
}
.cutout-slider :deep(.ant-slider-handle::after) {
  box-shadow: 0 0 0 2px #FF2442;
}
.cutout-control-tip {
  font-size: 12px;
  color: #8c8c8c;
  background: #fafafa;
  padding: 10px 12px;
  border-radius: 8px;
}

/* 暗色主题 */
body[data-theme="dark"] .cutout-tool-page { background: #141414; color: #e0e0e0; }
body[data-theme="dark"] .cutout-subpage-header {
  background: rgba(20, 20, 20, 0.96);
  border-bottom-color: #2a2a2a;
}
body[data-theme="dark"] .cutout-subpage-title { color: #e0e0e0; }
body[data-theme="dark"] .cutout-subpage-back { color: #a6a6a6; }
body[data-theme="dark"] .cutout-hero {
  background: linear-gradient(180deg, #2a1f22 0%, #1f1f1f 100%);
}
body[data-theme="dark"] .cutout-hero-title { color: #e0e0e0; }
body[data-theme="dark"] .cutout-hero-subtitle { color: #8c8c8c; }
body[data-theme="dark"] .cutout-hero-deco { background: #3a2a2e; }
body[data-theme="dark"] .cutout-card {
  background: #1f1f1f;
  box-shadow: none;
}
body[data-theme="dark"] .cutout-panel-title { color: #e0e0e0; }
body[data-theme="dark"] .cutout-btn {
  background: #1f1f1f;
  border-color: #404040;
  color: #a6a6a6;
}
body[data-theme="dark"] .cutout-btn:not(:disabled):hover {
  border-color: #ff4d6f;
  color: #ff4d6f;
}
body[data-theme="dark"] .cutout-btn--primary {
  background: linear-gradient(135deg, #FF4D6F 0%, #FF2442 100%);
  color: #fff;
}
body[data-theme="dark"] .cutout-btn--primary:not(:disabled):hover {
  background: linear-gradient(135deg, #e61e3a 0%, #c91a33 100%);
  color: #fff;
}
body[data-theme="dark"] .cutout-canvas-wrap {
  background: #262626;
  border-color: #404040;
}
body[data-theme="dark"] .cutout-canvas-wrap.is-empty:hover,
body[data-theme="dark"] .cutout-canvas-wrap.is-empty.is-drag {
  border-color: #ff4d6f;
  background: #2a1f22;
}
body[data-theme="dark"] .cutout-canvas-wrap--result {
  background:
    conic-gradient(#2a2a2a 25%, #1f1f1f 0 50%, #2a2a2a 0 75%, #1f1f1f 0);
  background-size: 20px 20px;
}
body[data-theme="dark"] .cutout-canvas-wrap--result.is-empty {
  background: #262626;
}
body[data-theme="dark"] .cutout-upload-icon { color: #595959; }
body[data-theme="dark"] .cutout-upload-title { color: #e0e0e0; }
body[data-theme="dark"] .cutout-upload-desc { color: #a6a6a6; }
body[data-theme="dark"] .cutout-controls { border-top-color: #303030; }
body[data-theme="dark"] .cutout-control-label { color: #e0e0e0; }
body[data-theme="dark"] .cutout-control-value { color: #a6a6a6; }
body[data-theme="dark"] .cutout-color-value { color: #a6a6a6; }
body[data-theme="dark"] .cutout-control-tip {
  background: #262626;
  color: #a6a6a6;
}
body[data-theme="dark"] .cutout-slider :deep(.ant-slider-track) {
  background: #ff4d6f;
}
body[data-theme="dark"] .cutout-slider :deep(.ant-slider-handle::after) {
  box-shadow: 0 0 0 2px #ff4d6f;
}
</style>
