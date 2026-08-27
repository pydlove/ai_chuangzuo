<template>
  <div class="mobile-qr-code">
    <!-- 子页面返回头 -->
    <header class="mqr-subpage-header">
      <div class="mqr-subpage-back" @click="goBack">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6"></polyline>
        </svg>
        <span>返回</span>
      </div>
      <div class="mqr-subpage-title">二维码生成</div>
    </header>

    <!-- 宣传文案 -->
    <section class="mqr-hero">
      <div class="mqr-hero__inner">
        <div class="mqr-hero__badge">二维码生成</div>
        <h1 class="mqr-hero__title">输入内容，一键生成</h1>
        <p class="mqr-hero__desc">支持网址、文本、联系方式等，生成后可直接下载保存</p>
      </div>
    </section>

    <!-- 生成工具区 -->
    <main class="mqr-tool">
      <div class="mqr-tool__inner">
      <!-- 输入 -->
      <div class="mqr-card">
        <div class="mqr-card__head">
          <label class="mqr-card__label">输入内容</label>
          <button class="mqr-fullscreen-btn" aria-label="全屏输入" @click="openFullscreen">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M8 3H5a2 2 0 0 0-2 2v3" />
              <path d="M21 8V5a2 2 0 0 0-2-2h-3" />
              <path d="M3 16v3a2 2 0 0 0 2 2h3" />
              <path d="M16 21h3a2 2 0 0 0 2-2v-3" />
            </svg>
          </button>
        </div>
        <textarea
          v-model="qrText"
          class="mqr-textarea"
          rows="3"
          placeholder="输入网址或任意文本…"
          maxlength="1000"
        />
        <div class="mqr-textarea__count">{{ qrText.length }} / 1000</div>
      </div>

      <!-- 配置 -->
      <div class="mqr-card">
        <label class="mqr-card__label">样式设置</label>
        <div class="mqr-setting-list">
          <div class="mqr-setting">
            <span class="mqr-setting__name">尺寸</span>
            <div class="mqr-setting__control mqr-setting__control--number">
              <button class="mqr-step-btn" @click="size = Math.max(100, size - 10)">-</button>
              <input v-model.number="size" type="number" min="100" max="1000" class="mqr-number-input" />
              <button class="mqr-step-btn" @click="size = Math.min(1000, size + 10)">+</button>
            </div>
          </div>

          <div class="mqr-setting">
            <span class="mqr-setting__name">纠错级别</span>
            <div class="mqr-setting__control">
              <select v-model="errorLevel" class="mqr-select">
                <option v-for="opt in errorLevelOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
              </select>
            </div>
          </div>

          <div class="mqr-setting">
            <span class="mqr-setting__name">边距</span>
            <div class="mqr-setting__control mqr-setting__control--number">
              <button class="mqr-step-btn" @click="margin = Math.max(0, margin - 1)">-</button>
              <input v-model.number="margin" type="number" min="0" max="10" class="mqr-number-input" />
              <button class="mqr-step-btn" @click="margin = Math.min(10, margin + 1)">+</button>
            </div>
          </div>

          <div class="mqr-setting">
            <span class="mqr-setting__name">背景色</span>
            <div class="mqr-setting__control">
              <label class="mqr-color-picker">
                <input v-model="bgColor" type="color" />
                <span class="mqr-color-picker__preview" :style="{ background: bgColor }"></span>
                <span class="mqr-color-picker__value">{{ bgColor }}</span>
              </label>
            </div>
          </div>

          <div class="mqr-setting mqr-setting--top">
            <span class="mqr-setting__name">logo</span>
            <div class="mqr-setting__control">
              <div
                v-if="!logoUrl"
                class="mqr-logo-upload"
                @click="triggerLogoUpload"
              >
                <input
                  ref="logoInput"
                  type="file"
                  accept="image/*"
                  class="mqr-logo-upload__input"
                  @change="onLogoChange"
                />
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="12" y1="5" x2="12" y2="19" />
                  <line x1="5" y1="12" x2="19" y2="12" />
                </svg>
              </div>
              <div v-else class="mqr-logo-preview">
                <img :src="logoUrl" alt="logo" />
                <button class="mqr-logo-remove" @click="removeLogo">×</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 生成按钮 -->
      <button
        class="mqr-action-btn"
        :disabled="!canGenerate || generating"
        @click="generate"
      >
        <span v-if="generating" class="mqr-action-btn__spinner"></span>
        <span>{{ generating ? '生成中…' : '生成二维码' }}</span>
      </button>

      <!-- 结果 -->
      <div v-if="qrDataUrl" class="mqr-result">
        <div class="mqr-result__title">二维码预览</div>
        <div class="mqr-result__canvas">
          <img :src="qrDataUrl" alt="二维码" class="mqr-result__img" />
        </div>
        <a
          :href="qrDataUrl"
          :download="downloadName"
          class="mqr-action-btn mqr-action-btn--primary"
          @click="onDownload"
        >下载二维码</a>
        <button class="mqr-reset-link" @click="reset">重新生成</button>
      </div>
    </div>
    </main>

    <MobileToolFooter />

    <!-- 全屏输入 -->
    <div v-if="fullscreenOpen" class="mqr-fullscreen">
      <div class="mqr-fullscreen__header">
        <button class="mqr-fullscreen__close" @click="closeFullscreen">取消</button>
        <span class="mqr-fullscreen__title">输入内容</span>
        <button class="mqr-fullscreen__confirm" @click="confirmFullscreen">完成</button>
      </div>
      <textarea
        ref="fullscreenTextarea"
        v-model="fullscreenText"
        class="mqr-fullscreen__textarea"
        placeholder="输入网址或任意文本…"
        maxlength="1000"
      />
      <div class="mqr-fullscreen__count">{{ fullscreenText.length }} / 1000</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import QRCode from 'qrcode'
import MobileToolFooter from '@/components/common/MobileToolFooter.vue'

const router = useRouter()

const qrText = ref('')
const generating = ref(false)
const qrDataUrl = ref('')
const fullscreenOpen = ref(false)
const fullscreenText = ref('')
const fullscreenTextarea = ref(null)

const size = ref(200)
const margin = ref(1)
const errorLevel = ref('Q')
const bgColor = ref('#ffffff')
const logoInput = ref(null)
const logoFile = ref(null)
const logoUrl = ref('')

const errorLevelOptions = [
  { value: 'L', label: 'L 可遮挡 7%' },
  { value: 'M', label: 'M 可遮挡 15%' },
  { value: 'Q', label: 'Q 可遮挡 25%' },
  { value: 'H', label: 'H 可遮挡 30%' }
]

const canGenerate = computed(() => qrText.value.trim().length > 0)
const downloadName = computed(() => {
  const ts = Date.now()
  return `爱创作二维码${ts}.png`
})

function goBack() {
  router.back()
}

function openFullscreen() {
  fullscreenText.value = qrText.value
  fullscreenOpen.value = true
  nextTick(() => {
    fullscreenTextarea.value?.focus()
  })
}

function closeFullscreen() {
  fullscreenOpen.value = false
  fullscreenText.value = ''
}

function confirmFullscreen() {
  qrText.value = fullscreenText.value
  fullscreenOpen.value = false
  fullscreenText.value = ''
}

function triggerLogoUpload() {
  logoInput.value?.click()
}

function onLogoChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    message.error('请选择图片文件')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    message.error('logo 大小不能超过 2MB')
    return
  }
  if (logoUrl.value) URL.revokeObjectURL(logoUrl.value)
  logoFile.value = file
  logoUrl.value = URL.createObjectURL(file)
}

function removeLogo() {
  if (logoUrl.value) URL.revokeObjectURL(logoUrl.value)
  logoFile.value = null
  logoUrl.value = ''
  if (logoInput.value) logoInput.value.value = ''
}

async function generate() {
  const text = qrText.value.trim()
  if (!text) {
    message.error('请输入要生成二维码的内容')
    return
  }

  generating.value = true
  try {
    const baseDataUrl = await QRCode.toDataURL(text, {
      errorCorrectionLevel: errorLevel.value,
      margin: margin.value,
      width: size.value,
      color: { dark: '#1a1a1a', light: bgColor.value }
    })

    if (logoUrl.value) {
      qrDataUrl.value = await compositeLogo(baseDataUrl, logoUrl.value)
    } else {
      qrDataUrl.value = baseDataUrl
    }
  } catch (err) {
    message.error('二维码生成失败，请检查内容后重试')
  } finally {
    generating.value = false
  }
}

function compositeLogo(qrDataUrl, logoSrc) {
  return new Promise((resolve, reject) => {
    const canvas = document.createElement('canvas')
    const ctx = canvas.getContext('2d')
    const qrImg = new Image()
    const logoImg = new Image()

    qrImg.onerror = () => reject(new Error('二维码图片加载失败'))
    logoImg.onerror = () => reject(new Error('logo 加载失败'))

    qrImg.onload = () => {
      const s = qrImg.naturalWidth
      canvas.width = s
      canvas.height = s
      ctx.drawImage(qrImg, 0, 0, s, s)

      logoImg.onload = () => {
        const logoSize = Math.round(s * 0.2)
        const x = (s - logoSize) / 2
        const y = (s - logoSize) / 2
        const padding = Math.round(logoSize * 0.12)

        ctx.fillStyle = bgColor.value || '#ffffff'
        ctx.fillRect(x - padding, y - padding, logoSize + padding * 2, logoSize + padding * 2)

        ctx.drawImage(logoImg, x, y, logoSize, logoSize)
        resolve(canvas.toDataURL('image/png'))
      }
      logoImg.src = logoSrc
    }
    qrImg.src = qrDataUrl
  })
}

function reset() {
  qrText.value = ''
  qrDataUrl.value = ''
  size.value = 200
  margin.value = 1
  errorLevel.value = 'Q'
  bgColor.value = '#ffffff'
  removeLogo()
}

function onDownload() {
  message.success('已开始下载')
}
</script>

<style scoped>
.mobile-qr-code {
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
.mqr-subpage-header {
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
.mqr-subpage-back {
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
.mqr-subpage-back svg {
  width: 20px;
  height: 20px;
}
.mqr-subpage-title {
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
.mqr-hero {
  width: 100%;
  padding: 36px 20px 24px;
  background: linear-gradient(180deg, #FFF5F7 0%, #fff 100%);
  text-align: center;
}
.mqr-hero__inner {
  max-width: 960px;
  margin: 0 auto;
}
.mqr-hero__badge {
  display: inline-block;
  background: linear-gradient(135deg, #FFF0F2, #FFE4E8);
  color: #FF2442;
  font-size: 12px;
  font-weight: 600;
  padding: 5px 14px;
  border-radius: 16px;
  margin-bottom: 14px;
}
.mqr-hero__title {
  font-size: 26px;
  font-weight: 800;
  margin-bottom: 8px;
  color: #1a1a1a;
}
.mqr-hero__desc {
  font-size: 15px;
  color: #8c8c8c;
  margin: 0;
}

/* 工具区 */
.mqr-tool {
  flex: 1 1 auto;
  width: 100%;
  padding: 20px 16px 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.mqr-tool__inner {
  width: 100%;
  max-width: 960px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 卡片 */
.mqr-card {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
}
.mqr-card__label {
  display: block;
  font-size: 15px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 12px;
}
.mqr-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.mqr-card__head .mqr-card__label {
  margin-bottom: 0;
}
.mqr-fullscreen-btn {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: none;
  background: #f8f9fa;
  color: #8c8c8c;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.mqr-fullscreen-btn svg {
  width: 16px;
  height: 16px;
}
.mqr-fullscreen-btn:active {
  background: #f0f0f0;
  color: #FF2442;
}

/* 输入 */
.mqr-textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  font-size: 15px;
  line-height: 1.6;
  color: #1a1a1a;
  background: #f8f9fa;
  resize: none;
  outline: none;
  transition: border-color 0.2s ease;
}
.mqr-textarea:focus {
  border-color: #FF2442;
  background: #fff;
}
.mqr-textarea::placeholder {
  color: #bfbfbf;
}
.mqr-textarea__count {
  text-align: right;
  font-size: 12px;
  color: #bfbfbf;
  margin-top: 8px;
}

/* 设置列表 */
.mqr-setting-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.mqr-setting {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.mqr-setting--top {
  align-items: flex-start;
}
.mqr-setting__name {
  font-size: 14px;
  color: #595959;
  flex-shrink: 0;
}
.mqr-setting__control {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-width: 120px;
}
.mqr-setting__control--number {
  gap: 8px;
}

/* 数字输入 */
.mqr-step-btn {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: 1px solid #f0f0f0;
  background: #f8f9fa;
  color: #595959;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  -webkit-tap-highlight-color: transparent;
}
.mqr-step-btn:active {
  background: #f0f0f0;
}
.mqr-number-input {
  width: 70px;
  height: 28px;
  padding: 0 6px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  background: #fff;
  font-size: 14px;
  color: #1a1a1a;
  text-align: center;
  outline: none;
}
.mqr-number-input:focus {
  border-color: #FF2442;
}

/* 下拉框 */
.mqr-select {
  min-width: 140px;
  height: 28px;
  padding: 0 8px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  background: #fff;
  font-size: 14px;
  color: #1a1a1a;
  outline: none;
}
.mqr-select:focus {
  border-color: #FF2442;
}

/* 颜色选择器 */
.mqr-color-picker {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.mqr-color-picker input[type="color"] {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}
.mqr-color-picker__preview {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: 1px solid #f0f0f0;
}
.mqr-color-picker__value {
  font-size: 14px;
  color: #1a1a1a;
  text-transform: uppercase;
}

/* logo 上传 */
.mqr-logo-upload {
  width: 80px;
  height: 80px;
  border: 1.5px dashed #e0e0e0;
  border-radius: 12px;
  background: #f8f9fa;
  color: #8c8c8c;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}
.mqr-logo-upload:active {
  border-color: #FF2442;
  background: #FFF5F7;
  color: #FF2442;
}
.mqr-logo-upload svg {
  width: 24px;
  height: 24px;
}
.mqr-logo-upload__input {
  display: none;
}
.mqr-logo-preview {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 12px;
  overflow: hidden;
  background: #f8f9fa;
}
.mqr-logo-preview img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.mqr-logo-remove {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 14px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
}

/* 按钮 */
.mqr-action-btn {
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
.mqr-action-btn:hover {
  background: linear-gradient(135deg, #e61e3a 0%, #c91a33 100%);
}
.mqr-action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.mqr-action-btn:active:not(:disabled) {
  transform: scale(0.98);
}
.mqr-action-btn--primary {
  text-decoration: none;
}
.mqr-action-btn__spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: mqr-spin 0.8s linear infinite;
}
@keyframes mqr-spin {
  to { transform: rotate(360deg); }
}

/* 结果 */
.mqr-result {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.mqr-result__title {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a1a;
}
.mqr-result__canvas {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 12px;
}
.mqr-result__img {
  width: 180px;
  height: 180px;
  border-radius: 8px;
  background: #fff;
}
.mqr-reset-link {
  width: 100%;
  padding: 10px;
  background: transparent;
  border: none;
  color: #8c8c8c;
  font-size: 14px;
  cursor: pointer;
}

/* 桌面端适配 */
@media (min-width: 769px) {
  .mqr-fullscreen-btn {
    display: none;
  }
  .mqr-result__img {
    width: 240px;
    height: 240px;
  }
  .mqr-setting-list {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
  }
}

/* 全屏输入 */
.mqr-fullscreen {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  flex-direction: column;
  background: #fff;
}
.mqr-fullscreen__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 12px;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid #f0f0f0;
}
.mqr-fullscreen__close,
.mqr-fullscreen__confirm {
  font-size: 15px;
  padding: 6px 10px;
  background: transparent;
  border: none;
  cursor: pointer;
}
.mqr-fullscreen__close {
  color: #595959;
}
.mqr-fullscreen__confirm {
  color: #FF2442;
  font-weight: 600;
}
.mqr-fullscreen__title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}
.mqr-fullscreen__textarea {
  flex: 1;
  width: 100%;
  padding: 16px;
  border: none;
  font-size: 16px;
  line-height: 1.7;
  color: #1a1a1a;
  background: #fff;
  resize: none;
  outline: none;
}
.mqr-fullscreen__textarea::placeholder {
  color: #bfbfbf;
}
.mqr-fullscreen__count {
  padding: 12px 16px 24px;
  text-align: right;
  font-size: 13px;
  color: #8c8c8c;
  background: #fff;
}

/* 暗色主题 */
body[data-theme="dark"] .mobile-qr-code {
  background: #141414;
  color: #e0e0e0;
}
body[data-theme="dark"] .mqr-subpage-header {
  background: rgba(20, 20, 20, 0.96);
  border-bottom-color: #2a2a2a;
}
body[data-theme="dark"] .mqr-subpage-back {
  color: #a6a6a6;
}
body[data-theme="dark"] .mqr-subpage-title {
  color: #e0e0e0;
}
body[data-theme="dark"] .mqr-hero {
  background: linear-gradient(180deg, #2a1f22 0%, #141414 100%);
}
body[data-theme="dark"] .mqr-hero__title,
body[data-theme="dark"] .mqr-card__label,
body[data-theme="dark"] .mqr-result__title,
body[data-theme="dark"] .mqr-color-picker__value,
body[data-theme="dark"] .mqr-number-input,
body[data-theme="dark"] .mqr-select {
  color: #e0e0e0;
}
body[data-theme="dark"] .mqr-hero__desc,
body[data-theme="dark"] .mqr-textarea__count,
body[data-theme="dark"] .mqr-setting__name,
body[data-theme="dark"] .mqr-reset-link {
  color: #a6a6a6;
}
body[data-theme="dark"] .mqr-card,
body[data-theme="dark"] .mqr-result {
  background: #1f1f1f;
}
body[data-theme="dark"] .mqr-textarea {
  background: #2a2a2a;
  border-color: #2a2a2a;
  color: #e0e0e0;
}
body[data-theme="dark"] .mqr-textarea::placeholder {
  color: #595959;
}
body[data-theme="dark"] .mqr-step-btn {
  background: #2a2a2a;
  border-color: #2a2a2a;
  color: #a6a6a6;
}
body[data-theme="dark"] .mqr-number-input,
body[data-theme="dark"] .mqr-select {
  background: #2a2a2a;
  border-color: #2a2a2a;
}
body[data-theme="dark"] .mqr-logo-upload {
  background: #2a2a2a;
  border-color: #303030;
  color: #a6a6a6;
}
body[data-theme="dark"] .mqr-logo-upload:active {
  border-color: #ff4d6f;
  background: rgba(255, 36, 66, 0.15);
}
body[data-theme="dark"] .mqr-fullscreen {
  background: #141414;
}
body[data-theme="dark"] .mqr-fullscreen__header {
  background: rgba(20, 20, 20, 0.96);
  border-bottom-color: #2a2a2a;
}
body[data-theme="dark"] .mqr-fullscreen__close {
  color: #a6a6a6;
}
body[data-theme="dark"] .mqr-fullscreen__confirm {
  color: #ff4d6f;
}
body[data-theme="dark"] .mqr-fullscreen__title,
body[data-theme="dark"] .mqr-fullscreen__textarea {
  color: #e0e0e0;
  background: #141414;
}
body[data-theme="dark"] .mqr-fullscreen__textarea::placeholder {
  color: #595959;
}
body[data-theme="dark"] .mqr-fullscreen__count {
  color: #a6a6a6;
  background: #141414;
}
body[data-theme="dark"] .mqr-fullscreen-btn {
  background: #2a2a2a;
  color: #a6a6a6;
}
body[data-theme="dark"] .mqr-fullscreen-btn:active {
  background: rgba(255, 36, 66, 0.15);
  color: #ff4d6f;
}
body[data-theme="dark"] .mqr-result__canvas {
  background: #2a2a2a;
}
</style>
