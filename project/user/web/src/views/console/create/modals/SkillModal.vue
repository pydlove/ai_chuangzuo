<template>
  <a-modal
    v-model:open="styleVisible"
    :footer="null"
    :width="720"
    centered
    class="style-modal"
  >
    <template #title>
      <div class="modal-title-wrap">
        <div class="modal-title">提示词</div>
        <div class="modal-subtitle">选择一套提示词，让 AI 写出你想要的调性</div>
      </div>
    </template>

    <div class="style-tabs">
      <button
        :class="['style-tab', { active: styleTab === 'my' }]"
        @click="styleTab = 'my'"
      >
        我的
      </button>
      <button
        :class="['style-tab', { active: styleTab === 'learned' }]"
        @click="styleTab = 'learned'; loadLearnedSkillsTab()"
      >
        学习
      </button>
      <button
        :class="['style-tab', { active: styleTab === 'favorites' }]"
        @click="styleTab = 'favorites'; loadFavoriteSkillsTab()"
      >
        收藏
      </button>
      <button
        :class="['style-tab', { active: styleTab === 'system' }]"
        @click="styleTab = 'system'; loadSystemSkillsTab()"
      >
        系统
      </button>
    </div>

    <div class="style-content">
      <!-- 系统预设 -->
      <div v-show="styleTab === 'system'" class="style-tab-pane">
        <div class="style-grid">
          <SkillCard
            v-for="s in systemList"
            :key="s.name"
            :name="s.name"
            :prompt="s.promptSummary"
            :scope="s.scope"
            size="compact"
            :selected="selectedStyleName === s.name"
            clickable
            show-view-btn
            @click="selectStyle(s)"
            @view="openPromptModal(s)"
          >
            <template #meta>{{ s.desc }}</template>
          </SkillCard>
        </div>
        <div v-if="systemTotal > systemPageSize" class="style-pagination">
          <a-pagination
            v-model:current="systemPage"
            v-model:pageSize="systemPageSize"
            :total="systemTotal"
            :page-size-options="pageSizeOptions"
            show-size-changer
            show-quick-jumper
            @change="onSystemPageChange"
            @showSizeChange="onSystemPageSizeChange"
          />
        </div>
      </div>

      <!-- 我的提示词 -->
      <div v-show="styleTab === 'my'" class="style-tab-pane">
        <div class="style-grid">
          <div class="style-add-card" @click="goToSkillsPage">
            <div class="style-add-icon">+</div>
            <div class="style-add-text">新建我的提示词</div>
          </div>
          <SkillCard
            v-for="m in myList"
            :key="m.name"
            :name="m.name"
            :prompt="promptSummary(m.prompt)"
            :scope="m.scope"
            size="compact"
            :selected="selectedStyleName === m.name"
            clickable
            show-view-btn
            @click="selectStyle(m)"
            @view="openPromptModal(m)"
          >
            <template #meta>
              <span>{{ m.desc }}</span>
              <span class="style-card-meta-dot">·</span>
              <span>已用 {{ m.count }} 次</span>
            </template>
          </SkillCard>
        </div>
        <div v-if="myTotal > myPageSize" class="style-pagination">
          <a-pagination
            v-model:current="myPage"
            v-model:pageSize="myPageSize"
            :total="myTotal"
            :page-size-options="pageSizeOptions"
            show-size-changer
            show-quick-jumper
            @change="onMyPageChange"
            @showSizeChange="onMyPageSizeChange"
          />
        </div>
      </div>

      <!-- 学习的提示词 -->
      <div v-show="styleTab === 'learned'" class="style-tab-pane">
        <div class="style-grid">
          <div
            v-if="learnedList.length === 0"
            class="style-empty style-empty-text"
          >
            还没有学习过的提示词，请前往「我的提示词」页面学习。
          </div>
          <SkillCard
            v-for="l in learnedList"
            v-else
            :key="l.name"
            :name="l.name"
            :prompt="promptSummary(l.prompt)"
            :scope="l.scope"
            size="compact"
            avatar-variant="learned"
            :selected="selectedStyleName === l.name"
            clickable
            show-view-btn
            @click="selectStyle(l)"
            @view="openPromptModal(l)"
          >
            <template #meta>学习 · {{ (l.createdAt || '').slice(0, 10) }}</template>
          </SkillCard>
        </div>
        <div v-if="learnedTotal > learnedPageSize" class="style-pagination">
          <a-pagination
            v-model:current="learnedPage"
            v-model:pageSize="learnedPageSize"
            :total="learnedTotal"
            :page-size-options="pageSizeOptions"
            show-size-changer
            show-quick-jumper
            @change="onLearnedPageChange"
            @showSizeChange="onLearnedPageSizeChange"
          />
        </div>
      </div>

      <!-- 收藏的提示词 -->
      <div v-show="styleTab === 'favorites'" class="style-tab-pane">
        <div class="style-grid">
          <div
            v-if="favoriteList.length === 0"
            class="style-empty style-empty-text"
          >
            还没有收藏的提示词，去
            <button class="style-empty-link" @click="goToSkillMarket">提示词市场</button>
            收藏喜欢的提示词吧。
          </div>
          <SkillCard
            v-for="f in favoriteList"
            v-else
            :key="f.id"
            :name="f.name"
            :prompt="promptSummary(f.prompt)"
            :scope="f.scope"
            size="compact"
            :selected="selectedStyleName === f.name"
            :clickable="f.status === 'approved'"
            :class="{ 'favorite-offline': f.status !== 'approved' }"
            show-view-btn
            @click="selectFavoriteStyle(f)"
            @view="openPromptModal(f)"
          >
            <template #meta>
              <span :class="['favorite-status-badge', f.status !== 'approved' ? 'offline' : '']">
                {{ f.status === 'approved' ? 'by ' + f.creatorName : '已下架' }}
              </span>
            </template>
          </SkillCard>
        </div>
        <div v-if="favoriteTotal > favoritePageSize" class="style-pagination">
          <a-pagination
            v-model:current="favoritePage"
            v-model:pageSize="favoritePageSize"
            :total="favoriteTotal"
            :page-size-options="pageSizeOptions"
            show-size-changer
            show-quick-jumper
            @change="onFavoritePageChange"
            @showSizeChange="onFavoritePageSizeChange"
          />
        </div>
      </div>
    </div>

    <div class="style-footer">
      <button
        class="style-apply-btn"
        :disabled="!selectedStyleName"
        @click="applySkillLocal"
      >
        应用
      </button>
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
        <div v-if="viewingSkill.scope" class="skill-prompt-scope-list">
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
import { ref, watch } from 'vue'
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
import SkillCard from '@/components/SkillCard.vue'

const { styleVisible } = useCreateForm()
const router = useRouter()

const styleTab = ref('my')
const selectedStyleName = ref(null)
const promptModalVisible = ref(false)
const viewingSkill = ref(null)

const pageSizeOptions = ['8', '16', '24']

// 各 tab 局部分页状态（不污染全局 ref）
const myPage = ref(1)
const myPageSize = ref(8)
const myTotal = ref(0)
const myList = ref([])

const systemPage = ref(1)
const systemPageSize = ref(8)
const systemTotal = ref(0)
const systemList = ref([])

const learnedPage = ref(1)
const learnedPageSize = ref(8)
const learnedTotal = ref(0)
const learnedList = ref([])

const favoritePage = ref(1)
const favoritePageSize = ref(8)
const favoriteTotal = ref(0)
const favoriteList = ref([])

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

const onMyPageChange = (page) => {
  myPage.value = page
  loadMySkillsTab()
}

const onMyPageSizeChange = (current, size) => {
  myPageSize.value = size
  myPage.value = 1
  loadMySkillsTab()
}

const onSystemPageChange = (page) => {
  systemPage.value = page
  loadSystemSkillsTab()
}

const onSystemPageSizeChange = (current, size) => {
  systemPageSize.value = size
  systemPage.value = 1
  loadSystemSkillsTab()
}

const onLearnedPageChange = (page) => {
  learnedPage.value = page
  loadLearnedSkillsTab()
}

const onLearnedPageSizeChange = (current, size) => {
  learnedPageSize.value = size
  learnedPage.value = 1
  loadLearnedSkillsTab()
}

const onFavoritePageChange = (page) => {
  favoritePage.value = page
  loadFavoriteSkillsTab()
}

const onFavoritePageSizeChange = (current, size) => {
  favoritePageSize.value = size
  favoritePage.value = 1
  loadFavoriteSkillsTab()
}

// 弹框打开时重置并加载我的提示词
watch(styleVisible, async (open) => {
  if (!open) return
  styleTab.value = 'my'
  selectedStyleName.value = null
  viewingSkill.value = null
  promptModalVisible.value = false

  // 重置分页
  myPage.value = 1
  myPageSize.value = 8
  systemPage.value = 1
  systemPageSize.value = 8
  learnedPage.value = 1
  learnedPageSize.value = 8
  favoritePage.value = 1
  favoritePageSize.value = 8

  await Promise.all([
    loadMySkillsTab(),
    loadFavoriteSkillsTab(),
    loadSystemSkillsTab(),
    loadLearnedSkillsTab(),
    // 全量加载到全局 ref，供 findSelectedSkill 跨分页查找
    loadMySkills('', 1, 999),
    loadFavoriteSkills(),
    loadSystemSkills(),
    loadLearnedSkills()
  ])
})

const selectStyle = (s) => {
  selectedStyleName.value = s.name
}

const selectFavoriteStyle = (f) => {
  if (f.status !== 'approved') {
    message.warning('该提示词已下架，无法使用')
    return
  }
  selectStyle(f)
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

const applySkillLocal = () => {
  const s = findSelectedSkill()
  if (!s) return
  if (s.id && favoriteSkills.value.some(x => x.id === s.id)) {
    const favorite = favoriteSkills.value.find(x => x.id === s.id)
    if (favorite && favorite.status !== 'approved') {
      message.warning('该提示词已下架，无法使用')
      return
    }
  }
  applySkill(s)
  styleVisible.value = false
}

const useFromPromptModal = () => {
  if (!viewingSkill.value) return
  if (viewingSkill.value.id) {
    const favorite = favoriteSkills.value.find(x => x.id === viewingSkill.value.id)
    if (favorite && favorite.status !== 'approved') {
      message.warning('该提示词已下架，无法使用')
      return
    }
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

const goToSkillMarket = () => {
  styleVisible.value = false
  router.push('/console/skill-market')
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
</script>

<style scoped>
/* skills 选择 */
.style-tabs {
  display: flex;
  gap: 24px;
  border-bottom: 1px solid #eee;
  margin-bottom: 20px;
}

.style-tab {
  padding: 8px 0;
  font-size: 14px;
  font-weight: 500;
  color: #595959;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
}

.style-tab.active {
  color: var(--color-primary);
  font-weight: 600;
  border-bottom-color: var(--color-primary);
}

.style-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.style-content {
  height: 60vh;
  overflow-y: auto;
  padding: 8px 0;
}

.style-tab-pane {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.style-pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 8px;
}

.style-pagination :deep(.ant-pagination) {
  color: var(--color-text-secondary);
}

.style-pagination :deep(.ant-pagination-item) {
  background: var(--color-bg-card);
  border-color: var(--color-border-default);
  border-radius: var(--radius-md);
  transition: all 0.2s;
}

.style-pagination :deep(.ant-pagination-item a) {
  color: var(--color-text-secondary);
}

.style-pagination :deep(.ant-pagination-item:hover) {
  border-color: var(--color-primary);
}

.style-pagination :deep(.ant-pagination-item:hover a) {
  color: var(--color-primary);
}

.style-pagination :deep(.ant-pagination-item-active) {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.style-pagination :deep(.ant-pagination-item-active a) {
  color: #fff;
}

.style-pagination :deep(.ant-pagination-prev .ant-pagination-item-link) {
  background: var(--color-bg-card);
  border-color: var(--color-border-default);
  color: var(--color-text-secondary);
  border-radius: var(--radius-md);
  transition: all 0.2s;
}

.style-pagination :deep(.ant-pagination-next .ant-pagination-item-link) {
  background: var(--color-bg-card);
  border-color: var(--color-border-default);
  color: var(--color-text-secondary);
  border-radius: var(--radius-md);
  transition: all 0.2s;
}

.style-pagination :deep(.ant-pagination-prev:hover .ant-pagination-item-link) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.style-pagination :deep(.ant-pagination-next:hover .ant-pagination-item-link) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.style-pagination :deep(.ant-pagination-disabled .ant-pagination-item-link) {
  color: var(--color-text-placeholder);
  border-color: var(--color-border-default);
  cursor: not-allowed;
}

.style-pagination :deep(.ant-pagination-disabled:hover .ant-pagination-item-link) {
  color: var(--color-text-placeholder);
  border-color: var(--color-border-default);
}

.style-pagination :deep(.ant-pagination-jump-prev .ant-pagination-item-container .ant-pagination-item-link-icon),
.style-pagination :deep(.ant-pagination-jump-next .ant-pagination-item-container .ant-pagination-item-link-icon) {
  color: var(--color-primary);
}

body[data-theme="dark"] .style-pagination :deep(.ant-pagination-item) {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .style-pagination :deep(.ant-pagination-item a) {
  color: #a6a6a6;
}

body[data-theme="dark"] .style-pagination :deep(.ant-pagination-item:hover) {
  border-color: var(--color-primary);
}

body[data-theme="dark"] .style-pagination :deep(.ant-pagination-item:hover a) {
  color: var(--color-primary);
}

body[data-theme="dark"] .style-pagination :deep(.ant-pagination-item-active) {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

body[data-theme="dark"] .style-pagination :deep(.ant-pagination-item-active a) {
  color: #fff;
}

body[data-theme="dark"] .style-pagination :deep(.ant-pagination-prev .ant-pagination-item-link) {
  background: #1f1f1f;
  border-color: #303030;
  color: #a6a6a6;
}

body[data-theme="dark"] .style-pagination :deep(.ant-pagination-next .ant-pagination-item-link) {
  background: #1f1f1f;
  border-color: #303030;
  color: #a6a6a6;
}

body[data-theme="dark"] .style-pagination :deep(.ant-pagination-prev:hover .ant-pagination-item-link) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .style-pagination :deep(.ant-pagination-next:hover .ant-pagination-item-link) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .style-pagination :deep(.ant-pagination-disabled .ant-pagination-item-link) {
  color: #595959;
  border-color: #303030;
}

body[data-theme="dark"] .style-pagination :deep(.ant-pagination-disabled:hover .ant-pagination-item-link) {
  color: #595959;
  border-color: #303030;
}

.style-card-meta-dot {
  color: #d9d9d9;
  font-weight: 700;
}

.style-add-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  border: 2px dashed #e8e8e8;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.25s ease;
  min-height: 200px;
  box-sizing: border-box;
  background: #fff;
  gap: 8px;
}

.style-add-card:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-bg);
  transform: translateY(-4px);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.06);
}

.style-add-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: #fff0f2;
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
}

.style-add-text {
  font-size: 13px;
  color: #595959;
  font-weight: 500;
}

.style-empty {
  grid-column: 1 / -1;
  text-align: center;
  padding: 32px 0;
}

.style-empty-text {
  color: #8c8c8c;
  font-size: 14px;
}

.style-empty-link {
  background: none;
  border: none;
  padding: 0;
  color: var(--color-primary);
  font-size: 14px;
  cursor: pointer;
  transition: color 0.2s;
}

.style-empty-link:hover {
  color: var(--color-primary-hover);
  text-decoration: underline;
  text-underline-offset: 3px;
}

.style-footer {
  padding: 12px 0 0;
  border-top: 1px solid #f0f0f0;
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.style-apply-btn {
  padding: 8px 24px;
  border-radius: 8px;
  border: none;
  background: #d9d9d9;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: not-allowed;
}

.style-apply-btn:not(:disabled) {
  background: var(--color-primary);
  cursor: pointer;
}

.style-apply-btn:not(:disabled):hover {
  background: var(--color-primary-hover);
}

.favorite-offline {
  opacity: 0.7;
}

.favorite-status-badge {
  font-size: 12px;
  color: #8c8c8c;
}

.favorite-status-badge.offline {
  color: #ff4d4f;
  font-weight: 500;
}

/* 移动端：底部滑上全屏面板 */
@media (max-width: 768px) {
  :global(.style-modal .ant-modal) {
    width: 100% !important;
    max-width: 100%;
    margin: 0;
    top: auto !important;
    bottom: 0;
    transform: none !important;
    padding: 0;
  }

  :global(.style-modal .ant-modal-content) {
    border-radius: 20px 20px 0 0;
    height: 88vh;
    display: flex;
    flex-direction: column;
  }

  :global(.style-modal .ant-modal-header) {
    flex-shrink: 0;
    border-bottom: 1px solid #f0f0f0;
    padding: 16px 18px;
    border-radius: 20px 20px 0 0;
  }

  :global(.style-modal .ant-modal-body) {
    flex: 1;
    overflow: hidden;
    padding: 12px 18px calc(12px + env(safe-area-inset-bottom));
    display: flex;
    flex-direction: column;
  }

  .style-tabs {
    gap: 0;
    margin-bottom: 12px;
  }

  .style-tab {
    flex: 1;
    padding: 10px 0;
    font-size: 13px;
    text-align: center;
    border-bottom-width: 2px;
  }

  .style-content {
    height: auto;
    flex: 1;
    overflow-y: auto;
  }

  .style-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .style-add-card {
    min-height: 120px;
    padding: 16px;
  }

  .style-footer {
    margin-top: 12px;
    padding-top: 12px;
  }

  .style-apply-btn {
    width: 100%;
    padding: 12px 24px;
    border-radius: 12px;
    font-size: 15px;
  }

  /* 提示词详情弹框 */
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

body[data-theme="dark"] .style-tabs {
  border-bottom-color: #303030;
}

body[data-theme="dark"] .style-tab {
  color: #a6a6a6;
}

body[data-theme="dark"] .style-tab:hover {
  color: #f0f0f0;
}

body[data-theme="dark"] .style-tab.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

body[data-theme="dark"] .style-card-meta-dot {
  color: #595959;
}

body[data-theme="dark"] .style-add-card {
  border-color: #434343;
  background: transparent;
}

body[data-theme="dark"] .style-add-card:hover {
  border-color: var(--color-primary);
  background: rgba(255, 36, 66, 0.12);
}

body[data-theme="dark"] .style-add-icon {
  background: rgba(255, 36, 66, 0.12);
  color: #ff6b81;
}

body[data-theme="dark"] .style-add-text {
  color: #d9d9d9;
}

body[data-theme="dark"] .style-empty-text {
  color: #a6a6a6;
}

body[data-theme="dark"] .style-empty-link {
  color: var(--color-primary);
}

body[data-theme="dark"] .style-footer {
  border-top-color: #303030;
}

body[data-theme="dark"] .style-apply-btn {
  background: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .style-apply-btn:not(:disabled) {
  background: var(--color-primary);
  color: #fff;
}

body[data-theme="dark"] .style-apply-btn:not(:disabled):hover {
  background: var(--color-primary-hover);
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

<style>
/* 提示词详情弹框 teleport 到 body，需非 scoped 全局覆盖 */
.skill-prompt-modal .ant-modal-body {
  max-height: 70vh;
  overflow-y: auto;
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
</style>
