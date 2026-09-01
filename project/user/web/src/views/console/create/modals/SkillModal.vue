<template>
  <a-modal
    v-model:open="styleVisible"
    :footer="null"
    :width="900"
    centered
    class="skill-selector-modal"
  >
    <template #title>
      <div class="modal-title-wrap">
        <div class="modal-title">选择提示词</div>
        <div class="modal-subtitle">选择一套提示词，让 AI 写出你想要的调性</div>
      </div>
    </template>

    <div class="skill-selector-body">
      <div class="prompt-body">
        <div class="prompt-preview-pane">
          <div v-if="currentPrompt" class="prompt-preview">
            <div class="prompt-preview-head">
              <div class="prompt-preview-name">{{ currentPrompt.name }}</div>
              <div class="prompt-preview-meta">
                <span v-if="currentPrompt.desc">{{ currentPrompt.desc }}</span>
                <span v-else-if="typeof currentPrompt.count === 'number'">自定义提示词 · 已用 {{ currentPrompt.count }} 次</span>
                <span v-else-if="currentPrompt.createdAt">学习 · {{ currentPrompt.createdAt.slice(0, 10) }}</span>
                <span v-else-if="currentPrompt.creatorName">by {{ currentPrompt.creatorName }}</span>
                <span v-else>系统预设</span>
              </div>
              <div v-if="parseScopeTags(currentPrompt.scope).length" class="prompt-preview-scope-list">
                <span v-for="tag in parseScopeTags(currentPrompt.scope)" :key="tag" class="prompt-preview-scope">{{ tag }}</span>
              </div>
            </div>
            <div class="prompt-preview-text">{{ currentPrompt.prompt }}</div>
            <div class="prompt-preview-actions">
              <button
                class="prompt-preview-use-btn"
                :disabled="selectedStyleName !== currentPrompt.name"
                @click="applySkillLocal"
              >
                应用
              </button>
              <button class="prompt-preview-view-btn" @click="openPromptModal(currentPrompt)">查看完整</button>
            </div>
          </div>
          <div v-else class="prompt-preview-empty">当前分类暂无提示词</div>
        </div>

        <div class="prompt-list-pane">
          <div class="prompt-tabs">
            <button
              v-for="tab in tabs"
              :key="tab.key"
              :class="['prompt-tab', { active: styleTab === tab.key }]"
              @click="switchTab(tab.key)"
            >
              {{ tab.label }}
            </button>
          </div>

          <div class="prompt-rows">
            <div v-if="styleTab === 'my'" class="prompt-row prompt-row--add" @click="goToSkillsPage">
              <div class="prompt-row-add-icon">+</div>
              <div class="prompt-row-add-text">新建我的提示词</div>
            </div>

            <div
              v-for="skill in currentList"
              :key="skill.bizNo || skill.name"
              :class="['prompt-row', { selected: selectedStyleName === skill.name, offline: isOffline(skill) }]"
              @click="selectSkill(skill)"
            >
              <div class="prompt-row-main">
                <div class="prompt-row-name">{{ skill.name }}</div>
                <div class="prompt-row-desc">{{ promptSummary(skill.prompt) }}</div>
                <div class="prompt-row-meta">
                  <template v-if="styleTab === 'my'">
                    <span>{{ skill.desc || '我的提示词' }}</span>
                    <span class="prompt-row-meta-dot">·</span>
                    <span>已用 {{ skill.count || 0 }} 次</span>
                  </template>
                  <template v-else-if="styleTab === 'learned'">
                    <span>学习 · {{ (skill.createdAt || '').slice(0, 10) }}</span>
                  </template>
                  <template v-else-if="styleTab === 'favorites'">
                    <span :class="['favorite-status-badge', isOffline(skill) ? 'offline' : '']">
                      {{ isOffline(skill) ? '已下架' : 'by ' + skill.creatorName }}
                    </span>
                  </template>
                  <template v-else>
                    <span>{{ skill.desc || '系统预设' }}</span>
                  </template>
                </div>
              </div>
              <div v-if="isOffline(skill)" class="prompt-row-badge">已下架</div>
            </div>

            <div v-if="!currentList.length && styleTab !== 'my'" class="prompt-empty">
              {{ emptyText }}
            </div>
          </div>

          <div v-if="currentTotal > currentPageSize" class="prompt-pagination">
            <a-pagination
              v-model:current="currentPage"
              :page-size="currentPageSize"
              :total="currentTotal"
              size="small"
              @change="onPageChange"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 提示词详情弹框 -->
    <a-modal
      class="skill-prompt-modal"
      :open="promptModalVisible"
      :title="viewingSkill?.name"
      :footer="null"
      :width="560"
      centered
      @cancel="closePromptModal"
    >
      <div v-if="viewingSkill" class="skill-prompt-body">
        <div class="skill-prompt-meta">
          <span v-if="viewingSkill.desc">{{ viewingSkill.desc }}</span>
          <span v-else-if="typeof viewingSkill.count === 'number'">自定义提示词 · 已用 {{ viewingSkill.count }} 次</span>
          <span v-else-if="viewingSkill.createdAt">学习 · {{ viewingSkill.createdAt.slice(0, 10) }}</span>
          <span v-else-if="viewingSkill.creatorName">by {{ viewingSkill.creatorName }}</span>
        </div>
        <div v-if="parseScopeTags(viewingSkill.scope).length" class="skill-prompt-scope-list">
          <span v-for="tag in parseScopeTags(viewingSkill.scope)" :key="tag" class="skill-prompt-scope">{{ tag }}</span>
        </div>
        <div class="skill-prompt-text">{{ viewingSkill.prompt }}</div>
        <div class="skill-prompt-actions">
          <button class="skill-prompt-use-btn" @click="useFromPromptModal">应用</button>
          <button class="skill-prompt-close-btn" @click="closePromptModal">关闭</button>
        </div>
      </div>
    </a-modal>
  </a-modal>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  systemSkills,
  mySkills,
  applySkill,
  learnedSkills,
  loadMySkills,
  loadLearnedSkills,
  loadSystemSkills
} from '@/composables/useSkills.js'
import {
  favoriteSkills,
  loadFavoriteSkills
} from '@/composables/useSkillMarket.js'
import { useCreateForm } from '../useCreateForm.js'

const { styleVisible } = useCreateForm()
const router = useRouter()

const styleTab = ref('my')
const selectedStyleName = ref(null)
const promptModalVisible = ref(false)
const viewingSkill = ref(null)

const pageSizeOptions = ['8', '12', '16']

// 各 tab 局部分页状态
const myPage = ref(1)
const myPageSize = ref(12)
const myTotal = ref(0)
const myList = ref([])

const systemPage = ref(1)
const systemPageSize = ref(12)
const systemTotal = ref(0)
const systemList = ref([])

const learnedPage = ref(1)
const learnedPageSize = ref(12)
const learnedTotal = ref(0)
const learnedList = ref([])

const favoritePage = ref(1)
const favoritePageSize = ref(12)
const favoriteTotal = ref(0)
const favoriteList = ref([])

const tabs = [
  { key: 'my', label: '我的' },
  { key: 'learned', label: '学习' },
  { key: 'favorites', label: '收藏' },
  { key: 'system', label: '系统' }
]

const tabState = {
  my: { page: myPage, pageSize: myPageSize, total: myTotal, list: myList, load: loadMySkillsTab },
  learned: { page: learnedPage, pageSize: learnedPageSize, total: learnedTotal, list: learnedList, load: loadLearnedSkillsTab },
  favorites: { page: favoritePage, pageSize: favoritePageSize, total: favoriteTotal, list: favoriteList, load: loadFavoriteSkillsTab },
  system: { page: systemPage, pageSize: systemPageSize, total: systemTotal, list: systemList, load: loadSystemSkillsTab }
}

const currentList = computed(() => tabState[styleTab.value].list.value)
const currentTotal = computed(() => tabState[styleTab.value].total.value)
const currentPage = computed({
  get: () => tabState[styleTab.value].page.value,
  set: (val) => { tabState[styleTab.value].page.value = val }
})
const currentPageSize = computed(() => tabState[styleTab.value].pageSize.value)

const currentPrompt = computed(() => {
  const list = currentList.value
  const selected = list.find(s => s.name === selectedStyleName.value)
  if (selected) return selected
  return list[0] || null
})

const emptyText = computed(() => {
  const map = {
    my: '你还没有保存自己的提示词',
    learned: '还没有学习过的提示词，可去「我的提示词」页面学习',
    favorites: '还没有收藏提示词，去提示词市场发现更多好风格',
    system: '系统提示词加载中...'
  }
  return map[styleTab.value] || ''
})

const loadMySkillsTab = async () => {
  const result = await loadMySkills('', myPage.value, myPageSize.value, false)
  myList.value = result.list || []
  myTotal.value = result.total || 0
}

const loadSystemSkillsTab = async () => {
  const result = await loadSystemSkills('', systemPage.value, systemPageSize.value)
  systemList.value = result.list || []
  systemTotal.value = result.total || 0
}

const loadLearnedSkillsTab = async () => {
  const result = await loadLearnedSkills('', learnedPage.value, learnedPageSize.value)
  learnedList.value = result.list || []
  learnedTotal.value = result.total || 0
}

const loadFavoriteSkillsTab = async () => {
  const result = await loadFavoriteSkills('', favoritePage.value, favoritePageSize.value)
  favoriteList.value = result.list || []
  favoriteTotal.value = result.total || 0
}

const switchTab = (key) => {
  styleTab.value = key
  tabState[key].load()
}

const onPageChange = (page) => {
  tabState[styleTab.value].page.value = page
  tabState[styleTab.value].load()
}

const isOffline = (skill) => {
  if (styleTab.value !== 'favorites') return false
  return skill.status && skill.status !== 'approved'
}

const selectSkill = (skill) => {
  if (isOffline(skill)) {
    message.warning('该提示词已下架，无法使用')
    return
  }
  selectedStyleName.value = skill.name
}

const applySkillLocal = () => {
  const s = findSelectedSkill()
  if (!s) return
  if (isOffline(s)) {
    message.warning('该提示词已下架，无法使用')
    return
  }
  applySkill(s)
  styleVisible.value = false
}

const findSelectedSkill = () => {
  const name = selectedStyleName.value
  if (!name) return null
  return systemList.value.find(x => x.name === name)
    || myList.value.find(x => x.name === name)
    || learnedList.value.find(x => x.name === name)
    || favoriteList.value.find(x => x.name === name)
    || systemSkills.value.find(x => x.name === name)
    || mySkills.value.find(x => x.name === name)
    || learnedSkills.value.find(x => x.name === name)
    || favoriteSkills.value.find(x => x.name === name)
    || null
}

const useFromPromptModal = () => {
  if (!viewingSkill.value) return
  if (isOffline(viewingSkill.value)) {
    message.warning('该提示词已下架，无法使用')
    return
  }
  selectedStyleName.value = viewingSkill.value.name
  applySkill(viewingSkill.value)
  promptModalVisible.value = false
  styleVisible.value = false
}

const goToSkillsPage = () => {
  styleVisible.value = false
  router.push('/console/skills')
}

const openPromptModal = (s) => {
  viewingSkill.value = s
  promptModalVisible.value = true
}

const closePromptModal = () => {
  promptModalVisible.value = false
  viewingSkill.value = null
}

const parseScopeTags = (scopeStr) => {
  if (!scopeStr) return []
  return scopeStr.split(/[,，]/).map(t => t.trim()).filter(Boolean)
}

const promptSummary = (prompt) => {
  if (!prompt) return ''
  return prompt.length > 60 ? prompt.slice(0, 60) + '...' : prompt
}

// 弹框打开时重置并加载我的提示词
watch(styleVisible, async (open) => {
  if (!open) return
  styleTab.value = 'my'
  selectedStyleName.value = null
  viewingSkill.value = null
  promptModalVisible.value = false

  myPage.value = 1
  myPageSize.value = 12
  systemPage.value = 1
  systemPageSize.value = 12
  learnedPage.value = 1
  learnedPageSize.value = 12
  favoritePage.value = 1
  favoritePageSize.value = 12

  await Promise.all([
    loadMySkillsTab(),
    loadFavoriteSkillsTab(),
    loadSystemSkillsTab(),
    loadLearnedSkillsTab(),
    loadMySkills('', 1, 999),
    loadFavoriteSkills(),
    loadSystemSkills(),
    loadLearnedSkills()
  ])
})
</script>

<style scoped>
.modal-title-wrap {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.modal-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.modal-subtitle {
  font-size: 12px;
  color: var(--color-text-secondary);
  font-weight: 400;
}

.skill-selector-body {
  padding: 8px 0 0;
}

.prompt-body {
  display: flex;
  gap: 16px;
  height: 520px;
}

.prompt-preview-pane {
  flex: 0 0 360px;
  background: #f5f5f5;
  border-radius: 12px;
  overflow: hidden;
  height: 100%;
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.05);
}

.prompt-preview {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
}

.prompt-preview-head {
  flex-shrink: 0;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.prompt-preview-name {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 8px;
  line-height: 1.4;
}

.prompt-preview-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 10px;
}

.prompt-preview-scope-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.prompt-preview-scope {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-primary);
  background: #fff5f7;
  border: 1px solid #ffd1d9;
  padding: 2px 8px;
  border-radius: 6px;
}

.prompt-preview-scope::before {
  content: '#';
  opacity: 0.8;
}

.prompt-preview-text {
  flex: 1;
  min-height: 0;
  padding: 16px;
  font-size: 14px;
  color: #262626;
  line-height: 1.8;
  overflow-y: auto;
  white-space: pre-line;
}

.prompt-preview-actions {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
  background: #fff;
}

.prompt-preview-use-btn {
  padding: 7px 16px;
  background: var(--color-primary);
  border: 1px solid var(--color-primary);
  border-radius: 8px;
  font-size: 14px;
  color: #fff;
  cursor: pointer;
  transition: all 0.2s;
}

.prompt-preview-use-btn:hover:not(:disabled) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.prompt-preview-use-btn:disabled {
  background: rgba(255, 36, 66, 0.35);
  border-color: rgba(255, 36, 66, 0.35);
  color: #fff;
  cursor: not-allowed;
}

.prompt-preview-view-btn {
  padding: 7px 16px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.prompt-preview-view-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-bg);
}

.prompt-preview-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  font-size: 14px;
  color: #8c8c8c;
}

.prompt-list-pane {
  flex: 1;
  min-width: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.prompt-tabs {
  display: flex;
  gap: 8px;
  padding: 0 0 14px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 14px;
  overflow-x: auto;
  flex-shrink: 0;
}

.prompt-tab {
  padding: 6px 14px;
  border-radius: 16px;
  border: 1px solid #d9d9d9;
  background: #fff;
  color: #595959;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}

.prompt-tab.active {
  border-color: #ff2442;
  background: #fff0f2;
  color: #ff2442;
  font-weight: 600;
}

.prompt-rows {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.prompt-row {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 2px solid #e8e8e8;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 8px;
}

.prompt-row:hover {
  border-color: #ff2442;
  background: #fff0f2;
}

.prompt-row.selected {
  border-color: #ff2442;
  background: #fff0f2;
  box-shadow: 0 0 0 2px rgba(255, 36, 66, 0.25);
}

.prompt-row.offline {
  cursor: not-allowed;
  background: #f5f5f5;
}

.prompt-row.offline:hover {
  border-color: #e8e8e8;
  background: #f5f5f5;
}

.prompt-row.offline .prompt-row-name,
.prompt-row.offline .prompt-row-desc,
.prompt-row.offline .prompt-row-meta {
  opacity: 0.55;
}

.prompt-row--add {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-style: dashed;
  background: #fff;
}

.prompt-row-add-icon {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: #fff0f2;
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
}

.prompt-row-add-text {
  font-size: 14px;
  color: #595959;
  font-weight: 500;
}

.prompt-row-main {
  flex: 1;
  min-width: 0;
}

.prompt-row-name {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 14px;
  margin-bottom: 4px;
  line-height: 1.4;
}

.prompt-row-desc {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.5;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.prompt-row-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #8c8c8c;
}

.prompt-row-meta-dot {
  color: #d9d9d9;
  font-weight: 700;
}

.prompt-row-badge {
  position: absolute;
  top: 4px;
  right: 6px;
  z-index: 1;
  padding: 1px 6px;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 600;
  line-height: 1.4;
  color: #fff;
  background: linear-gradient(135deg, #ff9a4d, #ff2442);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.12);
  pointer-events: none;
}

.favorite-status-badge {
  font-size: 12px;
  color: #8c8c8c;
}

.favorite-status-badge.offline {
  color: #ff4d4f;
  font-weight: 500;
}

.prompt-empty {
  padding: 32px;
  text-align: center;
  color: #8c8c8c;
  font-size: 14px;
}

.prompt-pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  margin-top: 12px;
  flex-shrink: 0;
}

.prompt-pagination :deep(.ant-pagination-item-active) {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}

.prompt-pagination :deep(.ant-pagination-item-active a),
.prompt-pagination :deep(.ant-pagination-item-active button) {
  color: #fff;
}

.prompt-pagination :deep(.ant-pagination-item:hover) {
  border-color: var(--color-primary);
}

.prompt-pagination :deep(.ant-pagination-item:hover a) {
  color: var(--color-primary);
}

@media (max-width: 768px) {
  :global(.skill-selector-modal .ant-modal) {
    width: 100% !important;
    max-width: 100%;
    margin: 0;
    top: auto !important;
    bottom: 0;
    transform: none !important;
    padding: 0;
  }

  :global(.skill-selector-modal .ant-modal-content) {
    border-radius: 20px 20px 0 0;
    height: 88vh;
    display: flex;
    flex-direction: column;
  }

  :global(.skill-selector-modal .ant-modal-header) {
    flex-shrink: 0;
    border-bottom: 1px solid #f0f0f0;
    padding: 14px 18px;
    border-radius: 20px 20px 0 0;
  }

  :global(.skill-selector-modal .ant-modal-body) {
    flex: 1;
    overflow: hidden;
    padding: 12px 18px calc(12px + env(safe-area-inset-bottom));
    display: flex;
    flex-direction: column;
  }

  .skill-selector-body {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
  }

  .prompt-body {
    flex: 1;
    min-height: 0;
    flex-direction: column;
    gap: 12px;
    height: auto;
  }

  .prompt-preview-pane {
    flex: none;
    width: 100%;
    height: 240px;
    order: 2;
  }

  .prompt-preview-text {
    font-size: 13px;
    padding: 12px 14px;
  }

  .prompt-preview-head {
    padding: 12px 14px;
  }

  .prompt-preview-name {
    font-size: 15px;
  }

  .prompt-preview-actions {
    padding: 10px 14px;
  }

  .prompt-preview-use-btn,
  .prompt-preview-view-btn {
    flex: 1;
    padding: 9px 12px;
    border-radius: 10px;
    font-size: 14px;
  }

  .prompt-list-pane {
    flex: none;
    height: auto;
    max-height: calc(100% - 260px);
    order: 1;
  }

  .prompt-tabs {
    margin-bottom: 10px;
    padding-bottom: 8px;
  }

  .prompt-rows {
    display: flex;
    gap: 10px;
    overflow-x: auto;
    overflow-y: hidden;
    padding-right: 0;
    padding-bottom: 4px;
    scrollbar-width: none;
  }

  .prompt-rows::-webkit-scrollbar {
    display: none;
  }

  .prompt-row {
    flex: 0 0 148px;
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
    padding: 12px;
    margin-bottom: 0;
    border-radius: 12px;
  }

  .prompt-row--add {
    flex: 0 0 120px;
    align-items: center;
    justify-content: center;
  }

  .prompt-row-desc {
    white-space: normal;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }

  .prompt-row-meta {
    font-size: 11px;
  }

  .prompt-pagination {
    margin-top: 8px;
    padding-top: 8px;
  }
}
</style>

<style>
/* 提示词详情弹框 teleport 到 body，需非 scoped 全局覆盖 */
.skill-prompt-modal .ant-modal-body {
  max-height: 70vh;
  overflow-y: auto;
}

.skill-prompt-body {
  padding: 8px 0 0;
}

.skill-prompt-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 12px;
}

.skill-prompt-scope-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.skill-prompt-scope {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-primary);
  background: #fff5f7;
  border: 1px solid #ffd1d9;
  padding: 3px 10px;
  border-radius: 6px;
}

.skill-prompt-scope::before {
  content: '#';
  opacity: 0.8;
}

.skill-prompt-text {
  font-size: 14px;
  color: #262626;
  line-height: 1.8;
  background: #fafafa;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 20px;
  white-space: pre-line;
  max-height: 360px;
  overflow-y: auto;
}

.skill-prompt-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.skill-prompt-use-btn {
  padding: 8px 20px;
  background: var(--color-primary);
  border: 1px solid var(--color-primary);
  border-radius: 8px;
  font-size: 14px;
  color: #fff;
  cursor: pointer;
  transition: all 0.2s;
}

.skill-prompt-use-btn:hover {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.skill-prompt-close-btn {
  padding: 8px 20px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.skill-prompt-close-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-bg);
}

@media (max-width: 768px) {
  .skill-prompt-modal .ant-modal {
    width: 100% !important;
    max-width: 100%;
    margin: 0;
    top: auto !important;
    bottom: 0;
    transform: none !important;
    padding: 0;
  }

  .skill-prompt-modal .ant-modal-content {
    border-radius: 20px 20px 0 0;
    height: 82vh;
    display: flex;
    flex-direction: column;
  }

  .skill-prompt-modal .ant-modal-header {
    flex-shrink: 0;
    border-bottom: 1px solid #f0f0f0;
    padding: 14px 18px;
    border-radius: 20px 20px 0 0;
  }

  .skill-prompt-modal .ant-modal-body {
    flex: 1;
    max-height: none;
    overflow: hidden;
    padding: 16px 18px calc(16px + env(safe-area-inset-bottom));
  }

  .skill-prompt-body {
    height: 100%;
    display: flex;
    flex-direction: column;
    padding: 0;
  }

  .skill-prompt-text {
    flex: 1;
    max-height: none;
    overflow-y: auto;
    margin-bottom: 16px;
  }

  .skill-prompt-actions {
    padding-top: 12px;
  }

  .skill-prompt-use-btn,
  .skill-prompt-close-btn {
    flex: 1;
    padding: 12px 20px;
    border-radius: 12px;
  }
}

body[data-theme="dark"] .skill-prompt-modal .ant-modal-content,
body[data-theme="dark"] .skill-prompt-modal .ant-modal-header {
  background: #1f1f1f !important;
  border-color: #303030 !important;
}

body[data-theme="dark"] .skill-prompt-modal .ant-modal-title {
  color: #f0f0f0 !important;
}

body[data-theme="dark"] .skill-prompt-modal .ant-modal-close-x {
  color: #a6a6a6 !important;
}

body[data-theme="dark"] .skill-prompt-modal .ant-modal-close:hover {
  background: #2a2a2a !important;
  color: #f0f0f0 !important;
}

body[data-theme="dark"] .skill-prompt-meta {
  color: #a6a6a6;
}

body[data-theme="dark"] .skill-prompt-scope {
  background: rgba(255, 36, 66, 0.12);
  border-color: rgba(255, 36, 66, 0.25);
  color: #ff6b81;
}

body[data-theme="dark"] .skill-prompt-text {
  background: #141414;
  color: #d9d9d9;
  border: 1px solid #303030;
}

body[data-theme="dark"] .skill-prompt-actions {
  border-top-color: #303030;
}

body[data-theme="dark"] .skill-prompt-use-btn {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

body[data-theme="dark"] .skill-prompt-use-btn:hover {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

body[data-theme="dark"] .skill-prompt-close-btn {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .skill-prompt-close-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: rgba(255, 36, 66, 0.12);
}
</style>
