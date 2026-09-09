<template>
  <div class="plan-gallery-content">
    <div v-if="loading && !items.length" class="gallery-loading">
      <a-spin />
    </div>

    <a-empty v-else-if="!items.length" description="还没有运营方案，快去制定你的专属方案吧" />

    <template v-else>
      <div class="gallery-list">
        <div v-for="item in items" :key="item.id" class="gallery-card">
          <div class="gallery-card-head">
            <span class="gallery-platform">{{ item.platformName || '未知平台' }}</span>
            <span v-if="item.isRecommendedByAi === 1" class="gallery-ai-tag">AI 推荐</span>
            <button class="gallery-copy" type="button" @click="handleCopy(item)">
              <CopyOutlined /> 复制
            </button>
          </div>
          <div class="gallery-niche">{{ item.nicheName || '—' }}</div>
          <div v-if="item.personaName" class="gallery-persona">人设：{{ item.personaName }}</div>
          <div v-if="item.pillars?.length" class="gallery-pillars">
            <span v-for="(p, idx) in item.pillars" :key="idx" class="gallery-pillar">
              {{ p.name }}<template v-if="p.percent"> {{ p.percent }}%</template>
            </span>
          </div>
        </div>
      </div>

      <div class="gallery-footer">
        <span class="gallery-count">已展示 {{ items.length }} / {{ total }} 个方案</span>
        <a-button
          v-if="items.length < total"
          type="primary"
          size="small"
          :loading="loading"
          @click="loadMore"
        >
          加载更多
        </a-button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { CopyOutlined } from '@ant-design/icons-vue'
import { fetchPlanGallery } from '@/api/selfMediaPlan.js'
import { useCopy } from '@/composables/useCopy.js'

const PAGE_SIZE = 20

const items = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)

const fetchPage = async (targetPage) => {
  loading.value = true
  try {
    const res = await fetchPlanGallery(targetPage, PAGE_SIZE)
    const data = res.data || {}
    const records = data.records || []
    if (targetPage === 1) {
      items.value = records
    } else {
      const seen = new Set(items.value.map((i) => i.id))
      items.value = [...items.value, ...records.filter((i) => !seen.has(i.id))]
    }
    total.value = data.total || 0
    page.value = data.current || targetPage
  } catch (e) {
    message.error(e?.msg || '加载运营方案失败')
  } finally {
    loading.value = false
  }
}

const loadMore = () => {
  fetchPage(page.value + 1)
}

const { copy: copyPlan } = useCopy({
  successText: '方案已复制',
  errorText: '复制失败，请长按手动复制'
})

function buildPlanText(item) {
  const lines = [
    `平台：${item.platformName || '未知平台'}`,
    `方向：${item.nicheName || '—'}`
  ]
  if (item.personaName) {
    lines.push(`人设：${item.personaName}`)
  }
  if (item.pillars?.length) {
    lines.push(`内容支柱：${item.pillars.map((p) => `${p.name}${p.percent ? ` ${p.percent}%` : ''}`).join(' / ')}`)
  }
  return lines.join('\n')
}

function handleCopy(item) {
  copyPlan(buildPlanText(item)).catch(() => {})
}

onMounted(() => fetchPage(1))
</script>

<style scoped>
.plan-gallery-content :deep(.ant-btn-primary) {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.plan-gallery-content :deep(.ant-btn-primary:hover),
.plan-gallery-content :deep(.ant-btn-primary:focus) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.plan-gallery-content :deep(.ant-btn-primary:active) {
  background: var(--color-primary-active);
  border-color: var(--color-primary-active);
}

.gallery-loading {
  display: flex;
  justify-content: center;
  padding: 48px 0;
}

.gallery-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.gallery-card {
  background: #F8F9FC;
  border-radius: 10px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.gallery-card-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.gallery-copy {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: transparent;
  color: var(--color-primary);
  font-size: 12px;
  line-height: 1.4;
  padding: 2px 4px;
  border-radius: 4px;
  cursor: pointer;
  flex-shrink: 0;
}

.gallery-copy:hover {
  background: rgba(255, 36, 66, 0.08);
}

.gallery-copy :deep(svg) {
  font-size: 12px;
}

.gallery-platform {
  font-size: 11px;
  color: #FF2442;
  background: #FFF0F3;
  border-radius: 4px;
  padding: 2px 8px;
  line-height: 1.4;
}

.gallery-ai-tag {
  font-size: 11px;
  color: #722ED1;
  background: #F9F0FF;
  border-radius: 4px;
  padding: 2px 8px;
  line-height: 1.4;
}

.gallery-niche {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.4;
}

.gallery-persona {
  font-size: 12px;
  color: #666;
  line-height: 1.4;
}

.gallery-pillars {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.gallery-pillar {
  font-size: 11px;
  color: #595959;
  background: #fff;
  border: 1px solid #EBEDF0;
  border-radius: 4px;
  padding: 2px 8px;
  line-height: 1.4;
}

.gallery-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
}

.gallery-count {
  font-size: 12px;
  color: #999;
}

body[data-theme="dark"] .gallery-card {
  background: #1f1f1f;
}

body[data-theme="dark"] .gallery-niche {
  color: #f0f0f0;
}

body[data-theme="dark"] .gallery-persona,
body[data-theme="dark"] .gallery-count {
  color: #737373;
}

body[data-theme="dark"] .gallery-pillar {
  background: #141414;
  border-color: #303030;
  color: #a6a6a6;
}

body[data-theme="dark"] .gallery-platform {
  background: rgba(255, 36, 66, 0.15);
  color: #FF4D6D;
}

body[data-theme="dark"] .gallery-copy:hover {
  background: rgba(255, 77, 109, 0.12);
}

@media (max-width: 768px) {
  /* 每个方案做成独立卡片 */
  .gallery-list {
    gap: 12px;
  }

  .gallery-card {
    background: #fff;
    border-radius: 12px;
    padding: 14px;
    gap: 8px;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  }

  .gallery-niche {
    font-size: 16px;
  }

  .gallery-copy {
    padding: 4px 8px;
  }

  body[data-theme="dark"] .gallery-card {
    background: #1f1f1f;
    box-shadow: none;
    border: 1px solid #303030;
  }
}
</style>
