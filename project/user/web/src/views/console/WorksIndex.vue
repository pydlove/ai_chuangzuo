<template>
  <div class="works-index">
    <MobileConsoleHero
      title="我的作品"
      desc="管理已生成的文章和草稿，随时查看、编辑或导出。"
      logo-url="/assets/images/我的作品logo-v1.png"
      image-url="/assets/images/我的作品宣传图-v1.png"
    />

    <div class="works-header">
      <h2 class="works-title">我的作品</h2>

      <div class="works-filter-bar">
        <input
          v-model="searchKeyword"
          type="text"
          class="works-search-input"
          placeholder="搜索标题关键词"
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
          placeholder="提示词"
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
          @click="switchTab('works')"
        >
          已生成
        </button>
        <button
          :class="['works-tab', { active: activeTab === 'drafts' }]"
          @click="switchTab('drafts')"
        >
          草稿箱
        </button>
      </div>
    </div>

    <!-- 已生成列表 -->
    <div v-if="activeTab === 'works'" class="works-list">
      <div v-if="worksList.length === 0 && !searchKeyword.trim()" class="works-empty">
        <EmptyState title="还没有生成的文章" description="创作你的第一篇文章，开启 AI 创作之旅" action-text="去创作" action-to="/console/workbench" />
      </div>
      <div v-else-if="worksList.length === 0 && searchKeyword.trim()" class="works-empty">
        <EmptyState title="未找到匹配的作品" description="换个关键词试试，或清空筛选条件" action-text="清空筛选" :action-handler="clearFilters" />
      </div>
      <div v-else-if="filteredWorks.length === 0" class="works-empty">
        <EmptyState title="未找到匹配的作品" description="当前筛选条件下没有作品，调整后再试" action-text="清空筛选" :action-handler="clearFilters" />
      </div>
      <div v-else class="work-cards">
        <div v-for="work in filteredWorks" :key="work.id" class="work-card">
          <div class="work-card__header">
            <div class="work-card__content" @click="openArticle(work.id)">
              <div class="work-title">{{ work.title }}</div>
              <div class="work-meta">
                <span>{{ work.platformName }}</span>
                <span>·</span>
                <span>{{ work.wordCount }} 字</span>
                <span>·</span>
                <span>{{ formatDateTime(work.completedAt) }}</span>
              </div>
            </div>
            <button
              class="work-card__menu"
              aria-label="更多操作"
              @click.stop="openActionSheet(work)"
            >
              <MoreOutlined />
            </button>
          </div>
          <div class="work-actions">
            <button class="work-action-btn" @click="openArticle(work.id)">查看</button>
            <button class="work-action-btn outline" @click="exportWorkWord(work)">导出word</button>
            <button class="work-action-btn outline" @click="editWork(work.id)">编辑内容</button>
            <button class="work-action-btn" @click="deleteWork(work)">删除</button>
          </div>
        </div>
      </div>
      <div v-if="worksTotal > 0" class="works-pagination">
        <a-pagination
          :current="worksPage"
          :page-size="worksPageSize"
          :total="worksTotal"
          show-size-changer
          show-total
          :page-size-options="['10', '20', '50']"
          @change="onWorksPageChange"
          @showSizeChange="onWorksPageSizeChange"
        />
      </div>
    </div>

    <!-- 草稿箱 -->
    <div v-if="activeTab === 'drafts'" class="drafts-list">
      <div v-if="draftsList.length === 0 && !searchKeyword.trim()" class="works-empty">
        <EmptyState title="草稿箱是空的" description="暂存草稿会出现在这里，方便继续编辑" action-text="去创作" action-to="/console/workbench" />
      </div>
      <div v-else-if="draftsList.length === 0 && searchKeyword.trim()" class="works-empty">
        <EmptyState title="未找到匹配的草稿" description="换个关键词试试，或清空筛选条件" action-text="清空筛选" :action-handler="clearFilters" />
      </div>
      <div v-else-if="filteredDrafts.length === 0" class="works-empty">
        <EmptyState title="未找到匹配的草稿" description="当前筛选条件下没有草稿，调整后再试" action-text="清空筛选" :action-handler="clearFilters" />
      </div>
      <div v-else class="work-cards">
        <div v-for="draft in filteredDrafts" :key="draft.id" class="work-card draft-card">
          <div class="work-title">{{ draft.title }}</div>
          <div class="work-meta">
            <span>{{ draft.platformName }}</span>
            <span>·</span>
            <span>{{ draft.wordCount }} 字</span>
            <span>·</span>
            <span>保存于 {{ formatDateTime(draft.savedAt) }}</span>
          </div>
          <div class="work-actions">
            <button class="work-action-btn primary" @click="resumeDraft(draft.id)">继续编辑</button>
            <button class="work-action-btn" @click="deleteDraft(draft)">删除</button>
          </div>
        </div>
      </div>
      <div v-if="draftsTotal > 0" class="works-pagination">
        <a-pagination
          :current="draftsPage"
          :page-size="draftsPageSize"
          :total="draftsTotal"
          show-size-changer
          show-total
          :page-size-options="['10', '20', '50']"
          @change="onDraftsPageChange"
          @showSizeChange="onDraftsPageSizeChange"
        />
      </div>
    </div>

    <!-- 移动端作品操作面板 -->
    <teleport to="body">
      <div
        v-if="actionSheetVisible"
        class="work-action-sheet-mask"
        @click="closeActionSheet"
      >
        <div class="work-action-sheet" @click.stop>
          <div class="work-action-sheet__handle"></div>
          <div v-if="activeWork" class="work-action-sheet__title">
            {{ activeWork.title }}
          </div>
          <button class="work-action-sheet__item" @click="onActionSheetExport">
            <FileWordOutlined class="work-action-sheet__icon" />
            <span>导出 Word</span>
          </button>
          <button class="work-action-sheet__item" @click="onActionSheetEdit">
            <EditOutlined class="work-action-sheet__icon" />
            <span>编辑内容</span>
          </button>
          <button class="work-action-sheet__item work-action-sheet__item--danger" @click="onActionSheetDelete">
            <DeleteOutlined class="work-action-sheet__icon" />
            <span>删除</span>
          </button>
          <button class="work-action-sheet__item work-action-sheet__item--cancel" @click="closeActionSheet">
            取消
          </button>
        </div>
      </div>
    </teleport>

    <!-- 微信内导出：公开下载链接弹窗 -->
    <a-modal
      v-model:open="exportLinkModalVisible"
      title="微信内请用浏览器下载"
      :footer="null"
      :closable="true"
      :mask-closable="true"
      width="360px"
      class="export-link-modal"
      @cancel="closeExportLinkModal"
    >
      <div class="export-link-body">
        <p class="export-link-tip">微信内无法直接下载文件，请复制下方链接，并在系统浏览器中打开下载。</p>
        <div class="export-link-url">{{ exportLinkUrl }}</div>
        <div class="export-link-actions">
          <button class="export-link-btn primary" @click="copyExportLink">复制下载链接</button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { MoreOutlined, FileWordOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { useWorks } from '@/composables/useWorks.js'
import { useDrafts } from '@/composables/useDrafts.js'
import { useConfirm } from '@/composables/useConfirm.js'
import MobileConsoleHero from '@/components/MobileConsoleHero.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { getArticle, deleteArticle as deleteArticleApi, getExportToken, downloadArticleWord } from '@/api/article.js'
import { getDraft, deleteDraft as deleteDraftApi } from '@/api/draft.js'
import { STORAGE_KEYS } from '@/constants/storage.js'
import { formatDateTime } from '@/utils/format.js'
import { useIsMobile } from '@/composables/useMobile.js'
import { PLATFORM_OPTIONS, PLATFORM_NAME_MAP } from '@/utils/platform.js'

const route = useRoute()
const router = useRouter()
const { confirm } = useConfirm()
const isMobile = useIsMobile()

const isWechat = /MicroMessenger/i.test(navigator.userAgent) && /Mobile/i.test(navigator.userAgent)

const copyToClipboard = async (text) => {
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text)
      return true
    }
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.left = '-9999px'
    textarea.style.top = '0'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.focus()
    textarea.select()
    const result = document.execCommand('copy')
    document.body.removeChild(textarea)
    return result
  } catch (e) {
    return false
  }
}

const activeTab = ref('works')

const platformOptions = PLATFORM_OPTIONS

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

const { articles: worksList, total: worksTotal, load: loadWorks } = useWorks()
const { drafts: draftsList, total: draftsTotal, load: loadDrafts } = useDrafts()

const worksPage = ref(1)
const worksPageSize = ref(10)
const draftsPage = ref(1)
const draftsPageSize = ref(10)

const buildListParams = () => ({
  keyword: searchKeyword.value.trim() || undefined
})

const loadWorksList = async () => {
  await loadWorks({
    page: worksPage.value,
    pageSize: worksPageSize.value,
    ...buildListParams()
  })
}

const loadDraftsList = async () => {
  await loadDrafts({
    page: draftsPage.value,
    pageSize: draftsPageSize.value,
    ...buildListParams()
  })
}

const onWorksPageChange = (page) => {
  worksPage.value = page
  loadWorksList()
}

const onWorksPageSizeChange = (_, size) => {
  worksPageSize.value = size
  worksPage.value = 1
  loadWorksList()
}

const onDraftsPageChange = (page) => {
  draftsPage.value = page
  loadDraftsList()
}

const onDraftsPageSizeChange = (_, size) => {
  draftsPageSize.value = size
  draftsPage.value = 1
  loadDraftsList()
}

const normalizeItems = (items, type) => {
  return items.map(item => {
    if (type === 'draft') {
      return {
        id: item.bizNo,
        title: item.title,
        platformName: item.platformName,
        skillName: item.skillName,
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
      skillName: item.skillName,
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

onMounted(async () => {
  if (route.query.tab === 'drafts') {
    activeTab.value = 'drafts'
  }
  try {
    if (activeTab.value === 'works') {
      await loadWorksList()
    } else {
      await loadDraftsList()
    }
  } catch (e) {
    // 忽略初始化加载异常
  }
})

const matchesFilters = (item) => {
  if (selectedPlatforms.value.length > 0) {
    const selectedLabels = selectedPlatforms.value.map(k => PLATFORM_NAME_MAP[k])
    if (!selectedLabels.includes(item.platformName)) {
      return false
    }
  }

  if (selectedStyles.value.length > 0) {
    if (!selectedStyles.value.includes(item.skillName)) {
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

const clearFilters = async () => {
  searchKeyword.value = ''
  selectedPlatforms.value = []
  selectedStyles.value = []
  timeRange.value = 'all'
  if (activeTab.value === 'works') {
    worksPage.value = 1
    await loadWorksList()
  } else {
    draftsPage.value = 1
    await loadDraftsList()
  }
}

let searchTimer = null
const debouncedSearch = () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(async () => {
    if (activeTab.value === 'works') {
      worksPage.value = 1
      await loadWorksList()
    } else {
      draftsPage.value = 1
      await loadDraftsList()
    }
  }, 300)
}

watch(searchKeyword, debouncedSearch)

const switchTab = async (tab) => {
  if (activeTab.value === tab) return
  activeTab.value = tab
  if (tab === 'works') {
    worksPage.value = 1
    await loadWorksList()
  } else {
    draftsPage.value = 1
    await loadDraftsList()
  }
}

const resumeDraft = async (bizNo) => {
  try {
    const draft = await getDraft(bizNo)
    if (!draft) return
    // create 页 onMounted 优先读取 CURRENT_ARTICLE
    localStorage.setItem(STORAGE_KEYS.CURRENT_ARTICLE, JSON.stringify({
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
    // 忽略草稿加载异常
  }
}

// 删除操作统一二次确认
const deleteDraft = (draft) => {
  confirm({
    title: '删除草稿',
    content: `确定要删除草稿「${draft.title}」吗？删除后不可恢复。`,
    okText: '删除',
    cancelText: '取消',
    danger: true,
    onOk: async () => {
      try {
        await deleteDraftApi(draft.id)
        draftsList.value = draftsList.value.filter((item) => item.bizNo !== draft.id)
      } catch (e) {
        // 删除失败已在 confirm 中处理，不再额外提示
      }
    }
  })
}

const deleteWork = (work) => {
  confirm({
    title: '删除作品',
    content: `确定要删除作品「${work.title}」吗？删除后不可恢复。`,
    okText: '删除',
    cancelText: '取消',
    danger: true,
    onOk: async () => {
      try {
        await deleteArticleApi(work.id)
        worksList.value = worksList.value.filter((item) => item.bizNo !== work.id)
      } catch (e) {
        // 删除失败已在 confirm 中处理，不再额外提示
      }
    }
  })
}

const activeWork = ref(null)
const actionSheetVisible = ref(false)

const openActionSheet = (work) => {
  activeWork.value = work
  actionSheetVisible.value = true
}

const closeActionSheet = () => {
  actionSheetVisible.value = false
  activeWork.value = null
}

const exportLinkModalVisible = ref(false)
const exportLinkUrl = ref('')
const exportLinkBizNo = ref('')

const closeExportLinkModal = () => {
  exportLinkModalVisible.value = false
  exportLinkUrl.value = ''
  exportLinkBizNo.value = ''
}

const buildExportLink = (token) => {
  return `${window.location.origin}/api/v1/public/articles/export/${token}`
}

const copyExportLink = async () => {
  const copied = await copyToClipboard(exportLinkUrl.value)
  if (copied) {
    message.success('下载链接已复制')
  } else {
    message.warning('复制失败，请长按链接手动复制')
  }
}

const requestExportToken = async (bizNo) => {
  const token = await getExportToken(bizNo)
  exportLinkBizNo.value = bizNo
  exportLinkUrl.value = buildExportLink(token)
  exportLinkModalVisible.value = true
}

const onActionSheetExport = () => {
  if (activeWork.value) {
    exportWorkWord(activeWork.value)
  }
  closeActionSheet()
}

const onActionSheetEdit = () => {
  if (activeWork.value) {
    editWork(activeWork.value.id)
  }
  closeActionSheet()
}

const onActionSheetDelete = () => {
  if (activeWork.value) {
    deleteWork(activeWork.value)
  }
  closeActionSheet()
}

const openArticle = (bizNo) => {
  router.push(`/console/preview/${bizNo}`)
}

const exportWorkWord = async (work) => {
  try {
    // 微信内置浏览器不支持 a.download 下载，使用后端临时公开链接
    if (isWechat) {
      await requestExportToken(work.id)
      return
    }

    await downloadArticleWord(work.id, work.title)

    if (isMobile.value) {
      message.success('Word 已导出，请从浏览器下载管理或通知栏查看')
    } else {
      message.success('Word 导出成功')
    }
  } catch (e) {
    message.error('导出失败，请稍后重试')
  }
}

const editWork = (bizNo) => {
  router.push(`/console/edit/${bizNo}`)
}
</script>

<style scoped>
.works-index {
  width: 100%;
  height: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 32px;
  overflow-y: auto;
  box-sizing: border-box;
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

.work-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.work-card__content {
  flex: 1;
  min-width: 0;
}

.work-card__menu {
  display: none;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: none;
  background: transparent;
  border-radius: 8px;
  color: #8c8c8c;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.work-card__menu:hover {
  background: #f5f5f5;
  color: #262626;
}

.work-card__menu:active {
  background: #ebebeb;
}

body[data-theme="dark"] .work-card__menu {
  color: #a6a6a6;
}

body[data-theme="dark"] .work-card__menu:hover {
  background: #2a2a2a;
  color: #f0f0f0;
}

body[data-theme="dark"] .work-card__menu:active {
  background: #333;
}

.works-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

body[data-theme="dark"] .works-pagination {
  border-top-color: #303030;
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

.work-action-btn.outline {
  background: #fff;
  border-color: #ff2442;
  color: #ff2442;
}

.work-action-btn.outline:hover {
  background: #fff0f2;
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

.works-filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  margin: 0 24px;
}

.works-search-input {
  width: 220px;
  height: 40px;
  padding: 0 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
  background: #fff;
  outline: none;
}

.works-search-input:focus {
  border-color: #ff2442;
  box-shadow: 0 0 0 2px rgba(255, 36, 66, 0.15);
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

body[data-theme="dark"] .work-action-btn.outline {
  background: transparent;
  border-color: var(--color-primary, #ff2442);
  color: var(--color-primary, #ff2442);
}

body[data-theme="dark"] .work-action-btn.outline:hover {
  background: rgba(255, 36, 66, 0.15);
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

body[data-theme="dark"] .works-search-input {
  background: #2a2a2a;
  border-color: #434343;
  color: #f0f0f0;
}

body[data-theme="dark"] .works-search-input::placeholder {
  color: #737373;
}

body[data-theme="dark"] .works-search-input:focus {
  border-color: #ff2442;
  box-shadow: 0 0 0 2px rgba(255, 36, 66, 0.15);
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

/* ============ 移动端底部操作面板 ============ */
.work-action-sheet-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 2000;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  animation: action-sheet-fade-in 0.2s ease;
}

.work-action-sheet {
  background: #fff;
  border-radius: 20px 20px 0 0;
  padding: 12px 16px calc(16px + env(safe-area-inset-bottom));
  animation: action-sheet-slide-up 0.25s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.work-action-sheet__handle {
  width: 36px;
  height: 4px;
  background: #e0e0e0;
  border-radius: 2px;
  margin: 0 auto 16px;
}

.work-action-sheet__title {
  font-size: 14px;
  font-weight: 500;
  color: #8c8c8c;
  text-align: center;
  padding: 0 12px 12px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.work-action-sheet__item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  padding: 14px 16px;
  border: none;
  background: transparent;
  border-radius: 12px;
  font-size: 16px;
  color: #1a1a1a;
  text-align: center;
  cursor: pointer;
  transition: background 0.15s;
}

.work-action-sheet__icon {
  font-size: 18px;
}

.work-action-sheet__item:active {
  background: #f5f5f5;
}

.work-action-sheet__item--danger {
  color: #ff2442;
}

.work-action-sheet__item--cancel {
  margin-top: 8px;
  background: #f5f5f5;
  color: #595959;
  font-weight: 500;
}

.work-action-sheet__item--cancel .work-action-sheet__icon {
  display: none;
}

.work-action-sheet__item--cancel:active {
  background: #e8e8e8;
}

@keyframes action-sheet-fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes action-sheet-slide-up {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

/* 深色模式 */
body[data-theme="dark"] .work-action-sheet {
  background: #1f1f1f;
}

body[data-theme="dark"] .work-action-sheet__handle {
  background: #434343;
}

body[data-theme="dark"] .work-action-sheet__title {
  color: #a6a6a6;
}

body[data-theme="dark"] .work-action-sheet__item {
  color: #f0f0f0;
}

body[data-theme="dark"] .work-action-sheet__item:active {
  background: #2a2a2a;
}

body[data-theme="dark"] .work-action-sheet__item--danger {
  color: #ff4d6f;
}

body[data-theme="dark"] .work-action-sheet__item--cancel {
  background: #2a2a2a;
  color: #a6a6a6;
}

body[data-theme="dark"] .work-action-sheet__item--cancel:active {
  background: #333;
}

/* ============ 微信导出：公开下载链接弹窗 ============ */
.export-link-body {
  padding: 4px 4px 8px;
}

.export-link-tip {
  font-size: 14px;
  line-height: 1.6;
  color: #595959;
  margin: 0 0 12px;
}

.export-link-url {
  font-size: 13px;
  line-height: 1.5;
  color: #1a1a1a;
  word-break: break-all;
  background: #f5f5f5;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 16px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.export-link-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.export-link-btn {
  width: 100%;
  padding: 11px 16px;
  border: 1px solid #d9d9d9;
  background: #fff;
  border-radius: 8px;
  font-size: 14px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.export-link-btn.primary {
  background: #ff2442;
  border-color: #ff2442;
  color: #fff;
}

.export-link-btn.primary:hover {
  background: #e61e3a;
  border-color: #e61e3a;
}

.export-link-btn:hover {
  border-color: #ff2442;
  color: #ff2442;
}

/* 深色模式 */
body[data-theme="dark"] .export-link-tip {
  color: #a6a6a6;
}

body[data-theme="dark"] .export-link-url {
  background: #2a2a2a;
  color: #f0f0f0;
}

body[data-theme="dark"] .export-link-btn {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .export-link-btn.primary {
  background: var(--color-primary, #ff2442);
  border-color: var(--color-primary, #ff2442);
  color: #fff;
}

body[data-theme="dark"] .export-link-btn.primary:hover {
  background: #ff4d6f;
  border-color: #ff4d6f;
}

body[data-theme="dark"] .export-link-btn:hover {
  color: #f0f0f0;
  border-color: #ff4d6f;
}

/* ============ 移动端：搜索栏换行 + 各控件自适应宽度 ============
   桌面端保持单行布局；≤768px 时：
   - header 改为纵向排列，标题 / 筛选 / tabs 各自一行
   - 筛选栏允许换行，搜索框占满宽度
   - 平台 / skills 两个下拉各占 50%（间距减半）
   - 时间范围单选独占一行，按钮平均分布
*/
@media (max-width: 768px) {
  .works-index {
    padding: 0 12px 12px;
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

  .works-title {
    display: none;
  }

  .works-tabs {
    align-self: flex-end;
  }

  .works-filter-bar {
    flex-wrap: wrap;
    margin: 0;
    gap: 10px;
  }

  .works-search-input {
    flex: 1 1 100%;
    width: 100%;
    min-width: 0;
    max-width: none;
    height: 44px;
    padding: 0 12px;
    border: 1px solid #d9d9d9;
    border-radius: 8px;
    font-size: 14px;
    box-sizing: border-box;
    background: #fff;
    outline: none;
  }

  .works-search-input:focus {
    border-color: #ff2442;
  }

  .works-filter-select {
    display: none;
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

  .work-card:not(.draft-card) .work-actions {
    display: none;
  }

  .work-card__menu {
    display: inline-flex;
  }

  .work-card__content {
    cursor: pointer;
  }

  .work-card__content:active {
    opacity: 0.85;
  }

  .draft-card .work-actions {
    flex-wrap: nowrap;
    justify-content: flex-end;
    gap: 8px;
  }

  .work-actions .work-action-btn,
  .work-actions .work-action-btn.outline {
    flex: 0 0 auto;
    width: auto;
    min-width: 0;
    font-size: 12px;
    padding: 4px 10px;
    border: none;
    background: transparent;
    color: #595959;
  }

  .work-actions .work-action-btn.outline {
    color: #ff2442;
  }

  .work-actions .work-action-btn:hover {
    background: rgba(0, 0, 0, 0.04);
    color: #595959;
  }

  .work-actions .work-action-btn.outline:hover {
    background: rgba(255, 36, 66, 0.08);
    color: #ff2442;
  }

  body[data-theme="dark"] .work-actions .work-action-btn,
  body[data-theme="dark"] .work-actions .work-action-btn.outline {
    background: transparent;
    color: #a6a6a6;
  }

  body[data-theme="dark"] .work-actions .work-action-btn.outline {
    color: #ff4d6f;
  }

  body[data-theme="dark"] .work-actions .work-action-btn:hover {
    background: rgba(255, 255, 255, 0.06);
    color: #f0f0f0;
  }

  body[data-theme="dark"] .work-actions .work-action-btn.outline:hover {
    background: rgba(255, 77, 111, 0.12);
    color: #ff4d6f;
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