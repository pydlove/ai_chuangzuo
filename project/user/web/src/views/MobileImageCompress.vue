<template>
  <div class="mobile-image-compress">
    <MobileSubpageHeader title="图片压缩" />

    <!-- Header 宣传文案 -->
    <section class="mic-hero">
      <div class="mic-hero__inner">
        <div class="mic-hero__badge">图片压缩</div>
        <h1 class="mic-hero__title">一键压缩，更快发布</h1>
        <p class="mic-hero__desc">本地完成、不上传服务器，压缩后可直接下载使用</p>
      </div>
    </section>

    <!-- 压缩工具区 -->
    <main class="mic-tool">
      <div class="mic-tool__inner">
      <!-- 上传 -->
      <div
        v-if="!originalUrl"
        class="mic-upload"
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
          class="mic-upload__input"
          @change="onFileChange"
        />
        <div class="mic-upload__icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="17 8 12 3 7 8" />
            <line x1="12" y1="3" x2="12" y2="15" />
          </svg>
        </div>
        <div class="mic-upload__title">点击或拖拽上传图片</div>
        <div class="mic-upload__tip">支持 JPG / PNG / WebP，单张不超过 20MB</div>
      </div>

      <!-- 已上传预览 -->
      <div v-else class="mic-preview-card">
        <div class="mic-preview-card__header">
          <span class="mic-preview-card__title">原图</span>
          <button class="mic-preview-card__reset" @click="reset">重新上传</button>
        </div>
        <img :src="originalUrl" alt="原图预览" class="mic-preview-card__img" />
        <div class="mic-preview-card__meta">
          <span>{{ originalFileName }}</span>
          <span>{{ formatSize(originalSize) }}</span>
        </div>
      </div>

      <!-- 压缩设置 -->
      <div v-if="originalUrl" class="mic-settings">
        <div class="mic-settings__title">压缩强度</div>
        <div class="mic-settings__options">
          <button
            v-for="opt in qualityOptions"
            :key="opt.key"
            class="mic-settings__option"
            :class="{ active: activeQuality === opt.key }"
            @click="activeQuality = opt.key"
          >
            <span class="mic-settings__option-name">{{ opt.name }}</span>
            <span class="mic-settings__option-desc">{{ opt.desc }}</span>
          </button>
        </div>
      </div>

      <!-- 压缩按钮 -->
      <button
        v-if="originalUrl && !compressedUrl"
        class="mic-action-btn"
        :disabled="compressing"
        @click="compress"
      >
        <span v-if="compressing" class="mic-action-btn__spinner"></span>
        <span>{{ compressing ? '压缩中…' : '开始压缩' }}</span>
      </button>

      <!-- 压缩结果 -->
      <div v-if="compressedUrl" class="mic-result">
        <div class="mic-result__header">
          <span class="mic-result__title">压缩完成</span>
          <span class="mic-result__ratio">-{{ compressionRatio }}%</span>
        </div>
        <img :src="compressedUrl" alt="压缩后预览" class="mic-result__img" />
        <div class="mic-result__meta">
          <div class="mic-result__stat">
            <span class="mic-result__stat-label">原图大小</span>
            <span class="mic-result__stat-value">{{ formatSize(originalSize) }}</span>
          </div>
          <div class="mic-result__stat">
            <span class="mic-result__stat-label">压缩后</span>
            <span class="mic-result__stat-value mic-result__stat-value--highlight">{{ formatSize(compressedSize) }}</span>
          </div>
        </div>
        <a
          :href="compressedUrl"
          :download="compressedFileName"
          class="mic-action-btn mic-action-btn--primary"
          @click="onDownload"
        >下载图片</a>
        <button class="mic-reset-link" @click="reset">压缩另一张</button>
      </div>
    </div>
    </main>

    <MobileToolFooter />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import MobileToolFooter from '@/components/common/MobileToolFooter.vue'

const router = useRouter()

const fileInput = ref(null)
const dragging = ref(false)

const originalFile = ref(null)
const originalUrl = ref('')
const originalSize = ref(0)
const originalFileName = ref('')

const compressing = ref(false)
const compressedFile = ref(null)
const compressedUrl = ref('')
const compressedSize = ref(0)
const compressedFileName = ref('')

const activeQuality = ref('standard')

const qualityOptions = [
  { key: 'light', name: '省流', desc: '最小体积', maxWidth: 1080, targetKB: 80 },
  { key: 'standard', name: '标准', desc: '平衡清晰', maxWidth: 1440, targetKB: 200 },
  { key: 'high', name: '高清', desc: '画质优先', maxWidth: 1920, targetKB: 500 }
]

const compressionRatio = computed(() => {
  if (!originalSize.value || !compressedSize.value) return 0
  return Math.round(((originalSize.value - compressedSize.value) / originalSize.value) * 100)
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

  reset()
  originalFile.value = file
  originalSize.value = file.size
  originalFileName.value = file.name
  originalUrl.value = URL.createObjectURL(file)
}

function reset() {
  if (originalUrl.value) URL.revokeObjectURL(originalUrl.value)
  if (compressedUrl.value) URL.revokeObjectURL(compressedUrl.value)
  originalFile.value = null
  originalUrl.value = ''
  originalSize.value = 0
  originalFileName.value = ''
  compressedFile.value = null
  compressedUrl.value = ''
  compressedSize.value = 0
  compressedFileName.value = ''
  activeQuality.value = 'standard'
  if (fileInput.value) fileInput.value.value = ''
}

function formatSize(bytes) {
  if (bytes === 0) return '0 B'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
}

function compress() {
  if (!originalFile.value) return
  const option = qualityOptions.find((o) => o.key === activeQuality.value) || qualityOptions[1]

  compressing.value = true
  compressImage(originalFile.value, option)
    .then((result) => {
      compressedFile.value = result.file
      compressedSize.value = result.file.size
      compressedUrl.value = URL.createObjectURL(result.file)
      const baseName = originalFileName.value.replace(/\.[^.]+$/, '')
      compressedFileName.value = `${baseName}_compressed.jpg`
    })
    .catch((err) => {
      message.error(err.message || '压缩失败，请重试')
    })
    .finally(() => {
      compressing.value = false
    })
}

function compressImage(file, option) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onerror = () => reject(new Error('文件读取失败'))
    reader.onload = (e) => {
      const img = new Image()
      img.onerror = () => reject(new Error('图片加载失败'))
      img.onload = () => {
        try {
          const canvas = document.createElement('canvas')
          const ctx = canvas.getContext('2d')
          let { width, height } = img

          if (width > option.maxWidth || height > option.maxWidth) {
            const scale = option.maxWidth / Math.max(width, height)
            width = Math.floor(width * scale)
            height = Math.floor(height * scale)
          }

          canvas.width = width
          canvas.height = height
          ctx.fillStyle = '#fff'
          ctx.fillRect(0, 0, width, height)
          ctx.drawImage(img, 0, 0, width, height)

          const targetBytes = option.targetKB * 1024
          let quality = 0.92
          const minQuality = 0.25
          const step = 0.06

          const tryCompress = () => {
            canvas.toBlob(
              (blob) => {
                if (!blob) {
                  reject(new Error('图片压缩失败'))
                  return
                }
                if (blob.size <= targetBytes || quality <= minQuality) {
                  const compressed = new File([blob], file.name.replace(/\.[^.]+$/, '.jpg'), { type: 'image/jpeg' })
                  resolve({ file: compressed, width, height })
                  return
                }
                quality = Math.max(minQuality, quality - step)
                tryCompress()
              },
              'image/jpeg',
              quality
            )
          }

          tryCompress()
        } catch (err) {
          reject(err)
        }
      }
      img.src = e.target.result
    }
    reader.readAsDataURL(file)
  })
}

function onDownload() {
  message.success('已开始下载')
}
</script>

<style scoped>
.mobile-image-compress {
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  background: #f8f9fa;
  color: #1a1a1a;
  -webkit-font-smoothing: antialiased;
}

/* 子页面返回头：与 console 子页面保持一致 */
.mic-subpage-header {
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
.mic-subpage-back {
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
.mic-subpage-back svg {
  width: 20px;
  height: 20px;
}
.mic-subpage-title {
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

/* Hero / 宣传文案 */
.mic-hero {
  padding: 36px 20px 24px;
  background: linear-gradient(180deg, #FFF5F7 0%, #fff 100%);
  text-align: center;
}
.mic-hero__inner {
  max-width: 960px;
  margin: 0 auto;
}
.mic-hero__badge {
  display: inline-block;
  background: linear-gradient(135deg, #FFF0F2, #FFE4E8);
  color: #FF2442;
  font-size: 12px;
  font-weight: 600;
  padding: 5px 14px;
  border-radius: 16px;
  margin-bottom: 14px;
}
.mic-hero__title {
  font-size: 26px;
  font-weight: 800;
  margin-bottom: 8px;
  color: #1a1a1a;
}
.mic-hero__desc {
  font-size: 15px;
  color: #8c8c8c;
  margin: 0;
}

/* 工具区 */
.mic-tool {
  flex: 1;
  padding: 20px 16px 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.mic-tool__inner {
  width: 100%;
  max-width: 960px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 上传区 */
.mic-upload {
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
.mic-upload.dragging,
.mic-upload:active {
  border-color: #FF2442;
  background: #FFF5F7;
}
.mic-upload__input {
  display: none;
}
.mic-upload__icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #FFF0F2, #FFE4E8);
  color: #FF2442;
  display: flex;
  align-items: center;
  justify-content: center;
}
.mic-upload__icon svg {
  width: 26px;
  height: 26px;
}
.mic-upload__title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}
.mic-upload__tip {
  font-size: 13px;
  color: #8c8c8c;
}

/* 预览卡 */
.mic-preview-card {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
}
.mic-preview-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.mic-preview-card__title {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a1a;
}
.mic-preview-card__reset {
  font-size: 13px;
  color: #FF2442;
  background: transparent;
  border: none;
  cursor: pointer;
}
.mic-preview-card__img {
  width: 100%;
  border-radius: 12px;
  background: #f5f5f5;
  display: block;
}
.mic-preview-card__meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
  font-size: 13px;
  color: #8c8c8c;
}
.mic-preview-card__meta span:first-child {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 压缩设置 */
.mic-settings {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
}
.mic-settings__title {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 12px;
}
.mic-settings__options {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}
.mic-settings__option {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 14px 8px;
  background: #f8f9fa;
  border: 1.5px solid transparent;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.mic-settings__option.active {
  border-color: #FF2442;
  background: #FFF5F7;
}
.mic-settings__option-name {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a1a;
}
.mic-settings__option-desc {
  font-size: 12px;
  color: #8c8c8c;
}
.mic-settings__option.active .mic-settings__option-desc {
  color: #FF2442;
}

/* 按钮 */
.mic-action-btn {
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
.mic-action-btn:hover {
  background: linear-gradient(135deg, #e61e3a 0%, #c91a33 100%);
}
.mic-action-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
.mic-action-btn:active:not(:disabled) {
  transform: scale(0.98);
}
.mic-action-btn--primary {
  text-decoration: none;
}
.mic-action-btn__spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: mic-spin 0.8s linear infinite;
}
@keyframes mic-spin {
  to { transform: rotate(360deg); }
}

/* 结果 */
.mic-result {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.mic-result__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.mic-result__title {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a1a;
}
.mic-result__ratio {
  font-size: 14px;
  font-weight: 700;
  color: #FF2442;
  background: #FFF5F7;
  padding: 4px 10px;
  border-radius: 12px;
}
.mic-result__img {
  width: 100%;
  border-radius: 12px;
  background: #f5f5f5;
  display: block;
}
.mic-result__meta {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.mic-result__stat {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 12px;
}
.mic-result__stat-label {
  font-size: 12px;
  color: #8c8c8c;
}
.mic-result__stat-value {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
}
.mic-result__stat-value--highlight {
  color: #FF2442;
}
.mic-reset-link {
  width: 100%;
  padding: 10px;
  background: transparent;
  border: none;
  color: #8c8c8c;
  font-size: 14px;
  cursor: pointer;
}

/* 暗色主题 */
body[data-theme="dark"] .mobile-image-compress {
  background: #141414;
  color: #e0e0e0;
}
body[data-theme="dark"] .mic-subpage-header {
  background: rgba(20, 20, 20, 0.96);
  border-bottom-color: #2a2a2a;
}
body[data-theme="dark"] .mic-subpage-back {
  color: #a6a6a6;
}
body[data-theme="dark"] .mic-subpage-title {
  color: #e0e0e0;
}
body[data-theme="dark"] .mic-hero {
  background: linear-gradient(180deg, #2a1f22 0%, #141414 100%);
}
body[data-theme="dark"] .mic-hero__title,
body[data-theme="dark"] .mic-preview-card__title,
body[data-theme="dark"] .mic-settings__title,
body[data-theme="dark"] .mic-result__title,
body[data-theme="dark"] .mic-result__stat-value,
body[data-theme="dark"] .mic-settings__option-name {
  color: #e0e0e0;
}
body[data-theme="dark"] .mic-hero__desc,
body[data-theme="dark"] .mic-upload__tip,
body[data-theme="dark"] .mic-preview-card__meta,
body[data-theme="dark"] .mic-result__stat-label,
body[data-theme="dark"] .mic-reset-link {
  color: #a6a6a6;
}
body[data-theme="dark"] .mic-upload,
body[data-theme="dark"] .mic-preview-card,
body[data-theme="dark"] .mic-settings,
body[data-theme="dark"] .mic-result {
  background: #1f1f1f;
}
body[data-theme="dark"] .mic-settings__option {
  background: #2a2a2a;
}
body[data-theme="dark"] .mic-result__stat {
  background: #2a2a2a;
}
</style>
