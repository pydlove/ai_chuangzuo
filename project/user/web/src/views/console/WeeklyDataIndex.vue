<template>
  <div class="weekly-data-page">
    <div class="weekly-data-page-body">
      <div v-if="articlesLoading" class="weekly-data-loading">
        <a-spin tip="加载中..." />
      </div>
      <template v-else-if="weeklyArticles.length">
        <div class="weekly-data-summary">
          本周共发布 <strong>{{ validWeeklyArticles.length }}</strong> 篇，总阅读量 <strong>{{ totalWeeklyReads }}</strong>
        </div>
        <div class="weekly-data-list">
          <div
            v-for="(item, index) in weeklyArticles"
            :key="index"
            class="weekly-data-item"
          >
            <div class="weekly-data-article-title">{{ item.title || '未命名创作' }}</div>
            <a-input-number v-model:value="item.reads" placeholder="阅读量" :min="0" class="weekly-data-reads" />
          </div>
        </div>
        <div class="weekly-data-actions">
          <a-button type="primary" block class="weekly-data-save-btn" :loading="weeklyLoading" @click="saveWeeklyData">保存</a-button>
        </div>
      </template>
      <div v-else class="weekly-data-empty">
        <a-empty description="请先生成文章" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Empty } from 'ant-design-vue'
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { getWeeklyArticles, saveWeeklyArticles } from '@/api/workbench.js'
import { listGenerationTasks } from '@/api/generation.js'

const router = useRouter()

const weeklyArticles = reactive([])
const weeklyLoading = ref(false)
const articlesLoading = ref(false)

const validWeeklyArticles = computed(() =>
  weeklyArticles.filter(item => (item.title || '').trim())
)

const totalWeeklyReads = computed(() => {
  return validWeeklyArticles.value.reduce((sum, item) => sum + (Number(item.reads) || 0), 0)
})

async function loadData() {
  articlesLoading.value = true
  try {
    const [weeklyRes, generationRes] = await Promise.all([
      getWeeklyArticles().catch(() => ({ data: [] })),
      listGenerationTasks({ page: 1, pageSize: 100 }).catch(() => ({ list: [] }))
    ])

    const savedList = weeklyRes?.data || []
    const savedMap = new Map(savedList.map(item => [item.title, item.reads ?? 0]))

    const oneWeek = 7 * 24 * 60 * 60 * 1000
    const now = Date.now()
    const tasks = generationRes?.list || []
    const thisWeekArticles = tasks
      .filter(item => {
        const ts = item.createdAt ? new Date(item.createdAt).getTime() : 0
        return ts && now - ts <= oneWeek && item.status === 2
      })
      .map(item => ({
        title: item.title || '未命名创作',
        reads: savedMap.get(item.title) ?? 0
      }))

    weeklyArticles.splice(0, weeklyArticles.length, ...thisWeekArticles)
  } catch (err) {
    console.warn('加载本周数据失败', err)
  } finally {
    articlesLoading.value = false
  }
}

async function saveWeeklyData() {
  const payload = weeklyArticles
    .map(item => ({ title: (item.title || '').trim(), reads: Number(item.reads) || 0 }))
    .filter(item => item.title)
  if (!payload.length) {
    message.warning('请先生成文章')
    return
  }
  weeklyLoading.value = true
  try {
    await saveWeeklyArticles({ articles: payload })
    message.success('本周数据已保存')
    router.back()
  } catch (err) {
    message.error(err?.message || '保存失败')
  } finally {
    weeklyLoading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.weekly-data-page {
  min-height: 100%;
  background: var(--color-bg-page);
}

.weekly-data-mobile-header {
  display: none;
}

.weekly-data-page-body {
  padding: var(--space-lg);
}

.weekly-data-summary {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-md) var(--space-lg);
  background: linear-gradient(135deg, var(--color-primary-bg) 0%, #fff 100%);
  border: 1px solid var(--color-primary-light);
  border-radius: var(--radius-xl);
  font-size: var(--font-body);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-lg);
}
.weekly-data-summary::before {
  content: '';
  width: 4px;
  height: 20px;
  border-radius: 2px;
  background: var(--color-primary);
  flex-shrink: 0;
}
.weekly-data-summary strong {
  color: var(--color-primary);
  font-weight: 700;
  margin: 0 2px;
}
.weekly-data-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
}
.weekly-data-item {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: 0;
  background: transparent;
  border: none;
  border-radius: 0;
}
.weekly-data-item:focus-within {
  border-color: transparent;
  box-shadow: none;
}
.weekly-data-title,
.weekly-data-reads {
  border-radius: var(--radius-md);
}
.weekly-data-title :deep(.ant-input),
.weekly-data-reads :deep(.ant-input-number-input) {
  border-radius: var(--radius-md);
}
.weekly-data-title :deep(.ant-input:focus),
.weekly-data-title :deep(.ant-input-focused),
.weekly-data-reads :deep(.ant-input-number-focused) {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px var(--color-primary-bg);
}
.weekly-data-title {
  flex: 1;
  min-width: 0;
}
.weekly-data-article-title {
  flex: 1;
  min-width: 0;
  font-size: var(--font-body);
  color: var(--color-text-primary);
  font-weight: 500;
  line-height: 1.5;
}
.weekly-data-reads {
  width: 140px;
  flex-shrink: 0;
}
.weekly-data-remove {
  flex-shrink: 0;
  padding: 0 8px;
}
.weekly-data-empty {
  padding: 48px 16px;
}
.weekly-data-loading {
  padding: 64px 16px;
  display: flex;
  justify-content: center;
}
.weekly-data-reads :deep(.ant-input-number-handler-wrap) {
  border-radius: 0 var(--radius-md) var(--radius-md) 0;
}
.weekly-data-actions {
  display: flex;
  justify-content: space-between;
  gap: var(--space-sm);
}
.weekly-data-actions .ant-btn-dashed {
  border-radius: var(--radius-md);
}

@media (max-width: 768px) {
  .weekly-data-page {
    background: var(--color-bg-card);
  }
  .weekly-data-page-body {
    padding: 16px 12px calc(16px + env(safe-area-inset-bottom));
  }
  .weekly-data-summary {
    margin-bottom: var(--space-md);
    font-size: var(--font-small);
  }
  .weekly-data-item {
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-sm);
  }
  .weekly-data-reads {
    width: 100%;
  }
  .weekly-data-actions {
    flex-direction: column;
    gap: var(--space-sm);
  }
  .weekly-data-actions .ant-btn-dashed,
  .weekly-data-actions .ant-btn-primary {
    height: 42px;
    border-radius: 12px;
    font-weight: 600;
  }
}
</style>

<style>
:global(.weekly-data-page .weekly-data-save-btn.ant-btn-primary) {
  background-color: var(--color-primary, #FF2442);
  border-color: var(--color-primary, #FF2442);
  color: #fff;
}
:global(.weekly-data-page .weekly-data-save-btn.ant-btn-primary:hover),
:global(.weekly-data-page .weekly-data-save-btn.ant-btn-primary:focus) {
  background-color: var(--color-primary-hover, #e61e3a);
  border-color: var(--color-primary-hover, #e61e3a);
  color: #fff;
}
:global(.weekly-data-page .weekly-data-save-btn.ant-btn-primary[disabled]) {
  color: rgba(255, 255, 255, 0.6);
  background-color: rgba(255, 36, 66, 0.4);
  border-color: transparent;
}
</style>
