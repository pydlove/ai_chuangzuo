<template>
  <div class="works-index">
    <div class="works-header">
      <h2 class="works-title">我的作品</h2>

      <div class="works-filter-bar">
        <a-input-search
          v-model:value="searchKeyword"
          placeholder="搜索标题关键词"
          class="works-search"
          allow-clear
        />
        <a-select
          v-model:value="selectedPlatforms"
          mode="multiple"
          class="works-filter-select"
          placeholder="平台"
          :max-tag-count="1"
          :options="platformOptions.map(p => ({ value: p.key, label: p.label }))"
          allow-clear
        />
        <a-select
          v-model:value="selectedStyles"
          mode="multiple"
          class="works-filter-select"
          placeholder="风格"
          :max-tag-count="1"
          :options="styleOptions.map(s => ({ value: s.key, label: s.label }))"
          allow-clear
        />
        <a-radio-group v-model:value="timeRange" class="works-filter-time">
          <a-radio-button v-for="opt in timeRangeOptions" :key="opt.key" :value="opt.key">
            {{ opt.label }}
          </a-radio-button>
        </a-radio-group>
      </div>

      <div class="works-tabs">
        <button
          :class="['works-tab', { active: activeTab === 'works' }]"
          @click="activeTab = 'works'"
        >
          已生成
        </button>
        <button
          :class="['works-tab', { active: activeTab === 'drafts' }]"
          @click="activeTab = 'drafts'"
        >
          草稿箱
        </button>
      </div>
    </div>

    <!-- 已生成列表 -->
    <div v-if="activeTab === 'works'" class="works-list">
      <div v-if="worksList.length === 0" class="works-empty">
        <a-empty description="还没有生成的文章">
          <button class="empty-btn" @click="$router.push('/console/create')">去创作</button>
        </a-empty>
      </div>
      <div v-else-if="filteredWorks.length === 0" class="works-empty">
        <a-empty description="未找到匹配的作品">
          <button class="empty-btn" @click="clearFilters">清空筛选</button>
        </a-empty>
      </div>
      <div v-else class="work-cards">
        <div v-for="work in filteredWorks" :key="work.id" class="work-card">
          <div class="work-title">{{ work.title }}</div>
          <div class="work-meta">
            <span>{{ work.platformName }}</span>
            <span>·</span>
            <span>{{ work.wordCount }} 字</span>
            <span>·</span>
            <span>{{ formatDate(work.completedAt) }}</span>
          </div>
          <div class="work-actions">
            <a-button
              type="primary"
              class="primary-btn"
              @click="openArticle(work.id)"
            >
              导出&生成贴图
            </a-button>
            <button class="work-action-btn" @click="editWork(work.id)">编辑内容</button>
            <button class="work-action-btn danger" @click="deleteWork(work.id)">删除</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 草稿箱 -->
    <div v-if="activeTab === 'drafts'" class="drafts-list">
      <div v-if="draftsList.length === 0" class="works-empty">
        <a-empty description="草稿箱是空的">
          <button class="empty-btn" @click="$router.push('/console/create')">去创作</button>
        </a-empty>
      </div>
      <div v-else-if="filteredDrafts.length === 0" class="works-empty">
        <a-empty description="未找到匹配的草稿">
          <button class="empty-btn" @click="clearFilters">清空筛选</button>
        </a-empty>
      </div>
      <div v-else class="work-cards">
        <div v-for="draft in filteredDrafts" :key="draft.id" class="work-card draft-card">
          <div class="work-title">{{ draft.title }}</div>
          <div class="work-meta">
            <span>{{ draft.platformName }}</span>
            <span>·</span>
            <span>{{ draft.wordCount }} 字</span>
            <span>·</span>
            <span>保存于 {{ formatDate(draft.savedAt) }}</span>
          </div>
          <div class="work-actions">
            <button class="work-action-btn primary" @click="resumeDraft(draft.id)">继续编辑</button>
            <button class="work-action-btn" @click="deleteDraft(draft.id)">删除</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useWorks } from '@/composables/useWorks.js'
import { useDrafts } from '@/composables/useDrafts.js'
import { getArticle, deleteArticle as deleteArticleApi } from '@/api/article.js'
import { getDraft, deleteDraft as deleteDraftApi } from '@/api/draft.js'

const route = useRoute()
const router = useRouter()

const activeTab = ref('works')

const platformOptions = [
  { key: 'wechat', label: '微信公众号' },
  { key: 'xiaohongshu', label: '小红书' },
  { key: 'toutiao', label: '今日头条' },
  { key: 'baijiahao', label: '百家号' },
  { key: 'douyin', label: '抖音图文' },
  { key: 'zhihu', label: '知乎' }
]

const styleOptions = [
  { key: '产品评测', label: '产品评测' },
  { key: '情感散文', label: '情感散文' },
  { key: '职场干货', label: '职场干货' },
  { key: '营销文案', label: '营销文案' },
  { key: '年度总结', label: '年度总结' },
  { key: '知识科普', label: '知识科普' },
  { key: '热点评论', label: '热点评论' },
  { key: '故事叙事', label: '故事叙事' }
]

const timeRangeOptions = [
  { key: 'all', label: '全部' },
  { key: '7', label: '近7天' },
  { key: '30', label: '近30天' },
  { key: '90', label: '近90天' }
]

const searchKeyword = ref('')
const selectedPlatforms = ref([])
const selectedStyles = ref([])
const timeRange = ref('all')

const { articles: worksList, load: loadWorks } = useWorks()
const { drafts: draftsList, load: loadDrafts } = useDrafts()

const normalizeItems = (items, type) => {
  return items.map(item => {
    if (type === 'draft') {
      return {
        id: item.bizNo,
        title: item.title,
        platformName: item.platformName,
        styleName: item.styleName,
        wordCount: item.wordCount,
        savedAt: item.savedAt,
        date: item.savedAt ? new Date(item.savedAt) : null,
        raw: item
      }
    }
    return {
      id: item.bizNo,
      title: item.title,
      platformName: item.platformName,
      styleName: item.styleName,
      wordCount: item.wordCount,
      completedAt: item.completedAt,
      date: item.completedAt ? new Date(item.completedAt) : null,
      raw: item
    }
  })
}

const isWithinDays = (date, days) => {
  if (!date) return false
  const now = new Date()
  const diff = (now - date) / (1000 * 60 * 60 * 24)
  return diff <= days
}

const platformMap = {
  wechat: '微信公众号',
  xiaohongshu: '小红书',
  toutiao: '今日头条',
  baijiahao: '百家号',
  douyin: '抖音图文',
  zhihu: '知乎'
}

onMounted(async () => {
  try {
    await Promise.all([loadWorks(), loadDrafts()])
  } catch (e) {
    console.warn('加载作品/草稿失败', e)
  }
  if (route.query.tab === 'drafts') {
    activeTab.value = 'drafts'
  }
})

const matchesFilters = (item) => {
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.trim().toLowerCase()
    if (!item.title.toLowerCase().includes(kw)) {
      return false
    }
  }

  if (selectedPlatforms.value.length > 0) {
    const selectedLabels = selectedPlatforms.value.map(k => platformMap[k])
    if (!selectedLabels.includes(item.platformName)) {
      return false
    }
  }

  if (selectedStyles.value.length > 0) {
    if (!selectedStyles.value.includes(item.styleName)) {
      return false
    }
  }

  if (timeRange.value !== 'all') {
    const days = parseInt(timeRange.value, 10)
    if (!isWithinDays(item.date, days)) {
      return false
    }
  }

  return true
}

const filteredWorks = computed(() => {
  return normalizeItems(worksList.value, 'work').filter(matchesFilters)
})

const filteredDrafts = computed(() => {
  return normalizeItems(draftsList.value, 'draft').filter(matchesFilters)
})

const clearFilters = () => {
  searchKeyword.value = ''
  selectedPlatforms.value = []
  selectedStyles.value = []
  timeRange.value = 'all'
}

const formatDate = (dateStr) => {
  const d = new Date(dateStr)
  const month = d.getMonth() + 1
  const day = d.getDate()
  const hour = d.getHours().toString().padStart(2, '0')
  const min = d.getMinutes().toString().padStart(2, '0')
  return `${month}月${day}日 ${hour}:${min}`
}

const resumeDraft = async (bizNo) => {
  try {
    const draft = await getDraft(bizNo)
    if (!draft) return
    // create 页 onMounted 优先读取 aichuangzuo_current_article
    localStorage.setItem('aichuangzuo_current_article', JSON.stringify({
      customTitle: draft.customTitle,
      customRequirement: draft.customRequirement,
      platform: draft.platform,
      wordCount: draft.wordCount,
      style: draft.style,
      template: draft.template,
      fromDraft: true
    }))
    router.push('/console/create')
  } catch (e) {
    console.warn('加载草稿失败', e)
  }
}

const deleteDraft = async (bizNo) => {
  try {
    await deleteDraftApi(bizNo)
    draftsList.value = draftsList.value.filter((item) => item.bizNo !== bizNo)
  } catch (e) {
    console.warn('删除草稿失败', e)
  }
}

const deleteWork = async (bizNo) => {
  try {
    await deleteArticleApi(bizNo)
    worksList.value = worksList.value.filter((item) => item.bizNo !== bizNo)
  } catch (e) {
    console.warn('删除作品失败', e)
  }
}

const openArticle = async (bizNo) => {
  try {
    const article = await getArticle(bizNo)
    if (!article) return
    // preview 页读取 aichuangzuo_current_article，结构是 {title, body, style, ...}
    localStorage.setItem('aichuangzuo_current_article', JSON.stringify({
      title: article.title,
      body: article.body,
      style: article.style,
      platform: article.platform,
      styleOverrides: article.styleOverrides
    }))
    router.push('/console/preview')
  } catch (e) {
    console.warn('加载作品失败', e)
  }
}

const editWork = async (bizNo) => {
  try {
    const article = await getArticle(bizNo)
    if (!article) return
    localStorage.setItem('aichuangzuo_current_article', JSON.stringify({
      id: article.bizNo,
      title: article.title,
      body: article.body,
      wordCount: article.wordCount,
      completedAt: article.completedAt,
      style: article.style,
      platform: article.platform,
      template: article.template,
      styleOverrides: article.styleOverrides
    }))
    router.push('/console/edit')
  } catch (e) {
    console.warn('加载作品失败', e)
  }
}
</script>

<style scoped>
.works-index {
  height: 100%;
  padding: 24px;
  overflow-y: auto;
}

.works-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.works-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
}

.works-tabs {
  display: flex;
  gap: 4px;
  background: #f5f5f5;
  padding: 4px;
  border-radius: 8px;
}

.works-tab {
  padding: 8px 16px;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.works-tab.active {
  background: #fff;
  color: #1a1a1a;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.works-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 14px;
  color: #8c8c8c;
  margin-bottom: 16px;
}

.empty-btn {
  padding: 8px 20px;
  background: #ff2442;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.empty-btn:hover {
  background: #e61e3a;
}

.work-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.work-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  padding: 16px;
}

.draft-card {
  border-color: #ffd1d9;
  background: #fff0f2;
}

.work-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.work-meta {
  display: flex;
  gap: 8px;
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 12px;
}

.work-actions {
  display: flex;
  gap: 8px;
}

.work-action-btn {
  padding: 6px 12px;
  border: 1px solid #d9d9d9;
  background: #fff;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.work-action-btn:hover {
  border-color: #ff2442;
  color: #ff2442;
}

.work-action-btn.primary {
  background: #ff2442;
  border-color: #ff2442;
  color: #fff;
}

.work-action-btn.primary:hover {
  background: #e61e3a;
  border-color: #e61e3a;
}

.primary-btn {
  background: #ff2442;
  border-color: #ff2442;
  box-shadow: 0 2px 0 rgba(255, 36, 66, 0.1) !important;
}

.primary-btn:hover {
  background: #e61e3a !important;
  border-color: #e61e3a !important;
}

.primary-btn:active {
  background: #cc1832 !important;
  border-color: #cc1832 !important;
}

.works-filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  margin: 0 24px;
}

.works-search {
  width: 220px;
}

.works-search :deep(.ant-input-affix-wrapper-focused),
.works-search :deep(.ant-input:focus) {
  border-color: #ff2442 !important;
  box-shadow: 0 0 0 2px rgba(255, 36, 66, 0.15) !important;
}

.works-filter-select {
  min-width: 120px;
}

.works-filter-select :deep(.ant-select-selector) {
  border-radius: 6px !important;
}

.works-filter-select :deep(.ant-select-focused .ant-select-selector),
.works-filter-select :deep(.ant-select-open .ant-select-selector) {
  border-color: #ff2442 !important;
  box-shadow: 0 0 0 2px rgba(255, 36, 66, 0.15) !important;
}

.works-filter-time :deep(.ant-radio-button-wrapper-checked) {
  color: #ff2442 !important;
  border-color: #ff2442 !important;
}

.works-filter-time :deep(.ant-radio-button-wrapper-checked::before) {
  background-color: #ff2442 !important;
}

.works-filter-time {
  display: flex;
  flex-shrink: 0;
}

/* 深色模式 */
body[data-theme="dark"] .work-card,
body[data-theme="dark"] .draft-card {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .work-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .work-meta,
body[data-theme="dark"] .works-empty {
  color: #a6a6a6;
}

body[data-theme="dark"] .work-action-btn {
  background: #2a2a2a;
  border-color: #303030;
  color: #a6a6a6;
}

body[data-theme="dark"] .work-action-btn.primary {
  background: var(--color-primary, #ff2442);
  border-color: var(--color-primary, #ff2442);
  color: #fff;
}

body[data-theme="dark"] .work-action-btn.danger {
  background: rgba(255, 77, 79, 0.15);
  border-color: rgba(255, 77, 79, 0.3);
  color: #ff4d4f;
}

body[data-theme="dark"] .works-tabs {
  background: #2a2a2a;
}

body[data-theme="dark"] .works-tab {
  color: #a6a6a6;
}

body[data-theme="dark"] .works-tab:hover {
  color: #f0f0f0;
}

body[data-theme="dark"] .works-tab.active {
  background: #1f1f1f;
  color: #f0f0f0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.4);
}

body[data-theme="dark"] .draft-card {
  background: #2a1f23;
  border-color: #5c2a30;
}

body[data-theme="dark"] .works-search :deep(.ant-input-affix-wrapper),
body[data-theme="dark"] .works-search :deep(.ant-input) {
  background: #2a2a2a !important;
  border-color: #434343 !important;
  color: #f0f0f0 !important;
}

body[data-theme="dark"] .works-search :deep(.ant-input::placeholder) {
  color: #737373 !important;
}

body[data-theme="dark"] .works-search :deep(.ant-input-group-addon) {
  background: #2a2a2a !important;
  border-color: #434343 !important;
  color: #a6a6a6 !important;
}

body[data-theme="dark"] .works-search :deep(.ant-input-search-button) {
  background: #2a2a2a !important;
  border-color: #434343 !important;
  color: #a6a6a6 !important;
}

body[data-theme="dark"] .works-search :deep(.ant-input-search-button:hover) {
  background: #303030 !important;
  border-color: var(--color-primary) !important;
  color: var(--color-primary) !important;
}

body[data-theme="dark"] .works-search :deep(.ant-input-search-icon) {
  color: #a6a6a6 !important;
}

body[data-theme="dark"] .works-search :deep(.ant-input-search-icon:hover) {
  color: var(--color-primary) !important;
}

body[data-theme="dark"] .works-filter-select :deep(.ant-select-selector) {
  background: #2a2a2a !important;
  border-color: #434343 !important;
  color: #f0f0f0 !important;
}

body[data-theme="dark"] .works-filter-select :deep(.ant-select-selection-item) {
  color: #f0f0f0 !important;
}

body[data-theme="dark"] .works-filter-select :deep(.ant-select-selection-placeholder) {
  color: #737373 !important;
}

body[data-theme="dark"] .works-filter-time :deep(.ant-radio-button-wrapper) {
  background: #2a2a2a !important;
  border-color: #434343 !important;
  color: #a6a6a6 !important;
}

body[data-theme="dark"] .works-filter-time :deep(.ant-radio-button-wrapper)::before {
  background-color: #434343 !important;
}

body[data-theme="dark"] .works-filter-time :deep(.ant-radio-button-wrapper-checked)::before {
  background-color: var(--color-primary, #ff2442) !important;
}

body[data-theme="dark"] .works-filter-time :deep(.ant-radio-button-wrapper:hover) {
  color: #f0f0f0 !important;
}

body[data-theme="dark"] :deep(.ant-empty-description) {
  color: #a6a6a6 !important;
}

body[data-theme="dark"] .primary-btn {
  background: var(--color-primary) !important;
  border-color: var(--color-primary) !important;
  color: #fff !important;
}

body[data-theme="dark"] .primary-btn:hover {
  background: var(--color-primary-hover) !important;
  border-color: var(--color-primary-hover) !important;
}

/* ============ 移动端：搜索栏换行 + 各控件自适应宽度 ============
   桌面端保持单行布局；≤768px 时：
   - header 改为纵向排列，标题 / 筛选 / tabs 各自一行
   - 筛选栏允许换行，搜索框占满宽度
   - 平台 / 风格 两个下拉各占 50%（间距减半）
   - 时间范围单选独占一行，按钮平均分布
*/
@media (max-width: 768px) {
  .works-index {
    padding: 12px;
  }

  .works-header {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
    margin-bottom: 12px;
  }

  .works-title {
    font-size: 18px;
  }

  .works-tabs {
    align-self: flex-end;
  }

  .works-filter-bar {
    flex-wrap: wrap;
    margin: 0;
    gap: 8px;
  }

  .works-search {
    flex: 1 1 100%;
    width: 100%;
  }

  .works-filter-select {
    flex: 1 1 calc(50% - 4px);
    min-width: 0;
  }

  .works-filter-time {
    flex: 1 1 100%;
    width: 100%;
    overflow-x: auto;
  }

  .works-filter-time :deep(.ant-radio-button-wrapper) {
    flex: 1 1 0;
    padding: 0 8px;
    text-align: center;
    font-size: 12px;
  }

  /* 暗色下移动端微调 */
  .work-card {
    padding: 12px;
  }

  .work-actions {
    flex-wrap: wrap;
  }

  .work-actions .ant-btn,
  .work-action-btn {
    flex: 1 1 auto;
    min-width: 0;
    font-size: 12px;
    padding: 6px 10px;
  }
}
</style>

<style>
/* 我的作品：Ant 下拉（select）弹层 teleport 到 body，需全局覆盖 */
body[data-theme="dark"] .ant-select-dropdown {
  background: #1f1f1f !important;
  border-color: #303030 !important;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.45) !important;
}

body[data-theme="dark"] .ant-select-item {
  color: #d9d9d9 !important;
}

body[data-theme="dark"] .ant-select-item-option-active,
body[data-theme="dark"] .ant-select-item:hover {
  background: #2a2a2a !important;
}

body[data-theme="dark"] .ant-select-item-option-selected {
  background: rgba(255, 36, 66, 0.15) !important;
  color: var(--color-primary) !important;
}

body[data-theme="dark"] .ant-empty-description {
  color: #a6a6a6 !important;
}
</style>