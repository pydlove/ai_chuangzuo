<template>
  <div class="tool-management">
    <a-card :bordered="false">
      <a-page-header title="工具管理" sub-title="常用的运营辅助工具" style="padding-left: 0; padding-top: 0" />

      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="watermark" tab="去水印">
          <ImageWatermarkRemoveTool />
        </a-tab-pane>
        <a-tab-pane key="image" tab="图片压缩">
          <a-alert
            message="使用说明"
            description="上传 PNG 等带透明背景的图片，压缩后仍会保留透明通道。默认输出 WebP（体积小且支持透明），也可切换为 PNG。压缩比例通过缩放图片尺寸实现。"
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
                    <p class="ant-upload-hint">支持 PNG、JPG、WebP，透明背景将保留为 PNG</p>
                  </a-upload-dragger>
                </a-form-item>

                <a-form-item label="输出格式">
                  <a-radio-group v-model:value="outputFormat">
                    <a-radio value="image/webp">WebP（推荐，体积小）</a-radio>
                    <a-radio value="image/png">PNG（无损）</a-radio>
                  </a-radio-group>
                </a-form-item>

                <a-form-item label="压缩比例">
                  <a-slider v-model:value="scalePercent" :min="10" :max="100" :step="5" />
                  <div class="scale-hint">{{ scalePercent }}%（按原尺寸缩放）</div>
                </a-form-item>

                <a-form-item>
                  <a-space>
                    <a-button type="primary" :disabled="!result" @click="handleDownload">
                      <DownloadOutlined /> 下载压缩后的图片
                    </a-button>
                    <a-button v-if="result" danger @click="handleClear">清空</a-button>
                  </a-space>
                </a-form-item>
              </a-form>
            </a-col>

            <a-col :span="12">
              <div v-if="originalFile" class="preview-panel">
                <a-row :gutter="16">
                  <a-col :span="12">
                    <div class="preview-title">原图</div>
                    <div class="preview-image-wrap">
                      <img :src="originalUrl" class="preview-image" alt="原图" />
                    </div>
                    <div class="preview-meta">
                      尺寸：{{ originalWidth }} × {{ originalHeight }}<br />
                      大小：{{ formatSize(originalFile.size) }}
                    </div>
                  </a-col>
                  <a-col :span="12">
                    <div class="preview-title">压缩后</div>
                    <div class="preview-image-wrap">
                      <img v-if="result" :src="result.url" class="preview-image" alt="压缩后" />
                      <div v-else class="preview-placeholder">正在处理…</div>
                    </div>
                    <div v-if="result" class="preview-meta">
                      尺寸：{{ result.width }} × {{ result.height }}<br />
                      格式：{{ result.format === 'image/webp' ? 'WebP' : 'PNG' }}<br />
                      大小：{{ formatSize(result.compressedSize) }}<br />
                      {{ savingPercent }}
                    </div>
                  </a-col>
                </a-row>
              </div>
              <a-empty v-else description="请先上传图片" />
            </a-col>
          </a-row>
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup>
import { ref, watch, computed, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import { PictureOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import { compressImage, formatSize } from '@/utils/imageCompress.js'
import ImageWatermarkRemoveTool from '@/components/ImageWatermarkRemoveTool.vue'

const activeTab = ref('watermark')
const scalePercent = ref(80)
const outputFormat = ref('image/webp')
const originalFile = ref(null)
const originalUrl = ref('')
const originalWidth = ref(0)
const originalHeight = ref(0)
const result = ref(null)
const processing = ref(false)

const savingPercent = computed(() => {
  if (!result.value || !originalFile.value) return ''
  const ratio = (result.value.compressedSize - originalFile.value.size) / originalFile.value.size
  if (ratio > 0) {
    return `体积增加 ${(ratio * 100).toFixed(1)}%`
  }
  return `节省 ${(Math.abs(ratio) * 100).toFixed(1)}%`
})

async function handleBeforeUpload(file) {
  if (!file.type.startsWith('image/')) {
    message.warning('请选择图片文件')
    return false
  }
  releaseUrls()

  originalFile.value = file
  originalUrl.value = URL.createObjectURL(file)
  result.value = null

  await loadOriginalSize()
  await compress()

  return false
}

function loadOriginalSize() {
  return new Promise((resolve) => {
    const img = new Image()
    img.onload = () => {
      originalWidth.value = img.width
      originalHeight.value = img.height
      resolve()
    }
    img.onerror = () => resolve()
    img.src = originalUrl.value
  })
}

async function compress() {
  if (!originalFile.value) return
  processing.value = true
  try {
    if (result.value) {
      URL.revokeObjectURL(result.value.url)
    }
    const scale = scalePercent.value / 100
    result.value = await compressImage(originalFile.value, scale, outputFormat.value)
  } catch (e) {
    message.error(e?.message || '压缩失败')
    result.value = null
  } finally {
    processing.value = false
  }
}

watch([scalePercent, outputFormat], () => {
  compress()
})

function handleDownload() {
  if (!result.value) return
  const link = document.createElement('a')
  link.href = result.value.url
  const name = originalFile.value.name.replace(/\.[^.]+$/, '') || 'image'
  link.download = `${name}_compressed.${result.value.extension}`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

function handleClear() {
  releaseUrls()
  originalFile.value = null
  originalUrl.value = ''
  originalWidth.value = 0
  originalHeight.value = 0
  result.value = null
}

function releaseUrls() {
  if (originalUrl.value) {
    URL.revokeObjectURL(originalUrl.value)
  }
  if (result.value?.url) {
    URL.revokeObjectURL(result.value.url)
  }
}

onUnmounted(releaseUrls)
</script>

<style scoped>
.tool-management {
  max-width: 1200px;
}
.preview-panel {
  background: #fafafa;
  border-radius: 8px;
  padding: 16px;
}
.preview-title {
  font-weight: 500;
  margin-bottom: 8px;
  color: #262626;
}
.preview-image-wrap {
  width: 100%;
  height: 240px;
  background-image:
    linear-gradient(45deg, #e0e0e0 25%, transparent 25%),
    linear-gradient(-45deg, #e0e0e0 25%, transparent 25%),
    linear-gradient(45deg, transparent 75%, #e0e0e0 75%),
    linear-gradient(-45deg, transparent 75%, #e0e0e0 75%);
  background-size: 16px 16px;
  background-position: 0 0, 0 8px, 8px -8px, -8px 0;
  background-color: #ffffff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}
.preview-placeholder {
  color: #8c8c8c;
}
.preview-meta {
  margin-top: 8px;
  font-size: 13px;
  color: #595959;
  line-height: 1.6;
}
.scale-hint {
  font-size: 13px;
  color: #8c8c8c;
  margin-top: -8px;
}
</style>
