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
        我的提示词
      </button>
      <button
        :class="['style-tab', { active: styleTab === 'learned' }]"
        @click="styleTab = 'learned'; loadLearnedSkills()"
      >
        学习的提示词
      </button>
      <button
        :class="['style-tab', { active: styleTab === 'favorites' }]"
        @click="styleTab = 'favorites'; loadFavoriteIds()"
      >
        收藏的提示词
      </button>
      <button
        :class="['style-tab', { active: styleTab === 'system' }]"
        @click="styleTab = 'system'"
      >
        系统预设提示词
      </button>
    </div>

    <div class="style-content">
      <!-- 系统预设 -->
      <div v-show="styleTab === 'system'" class="style-grid">
        <div
          v-for="s in systemSkills"
          :key="s.name"
          :class="['style-card', { selected: selectedStyleName === s.name }]"
          @click="selectStyle(s)"
        >
          <div class="style-card-head">
            <div class="style-card-avatar">{{ s.name.charAt(0) }}</div>
            <div class="style-card-title-wrap">
              <div class="style-card-title-row">
                <div class="style-card-title">{{ s.name }}</div>
              </div>
              <div class="style-card-meta">{{ s.desc }}</div>
            </div>
          </div>
          <div class="style-card-prompt">{{ s.promptSummary }}</div>
          <div class="style-card-footer">
            <button class="style-action-btn" @click.stop="openPromptModal(s)">
              查看完整提示词
            </button>
          </div>
        </div>
      </div>

      <!-- 我的提示词 -->
      <div v-show="styleTab === 'my'" class="style-grid">
        <div class="style-add-card" @click="goToSkillsPage">
          <div class="style-add-icon">+</div>
          <div class="style-add-text">新建我的提示词</div>
        </div>
        <div
          v-for="m in mySkills"
          :key="m.name"
          :class="['style-card', { selected: selectedStyleName === m.name }]"
          @click="selectStyle(m)"
        >
          <div class="style-card-head">
            <div class="style-card-avatar">{{ m.name.charAt(0) }}</div>
            <div class="style-card-title-wrap">
              <div class="style-card-title-row">
                <div class="style-card-title">{{ m.name }}</div>
              </div>
              <div class="style-card-meta">
                <span>自定义提示词</span>
                <span class="style-card-meta-dot">·</span>
                <span>已用 {{ m.count }} 次</span>
              </div>
            </div>
          </div>
          <div v-if="m.scope" class="style-card-scope-list">
            <span v-for="tag in parseScopeTags(m.scope)" :key="tag" class="style-card-scope">{{ tag }}</span>
          </div>
          <div class="style-card-prompt">{{ promptSummary(m.prompt) }}</div>
          <div class="style-card-footer">
            <button class="style-action-btn" @click.stop="openPromptModal(m)">
              查看完整提示词
            </button>
          </div>
        </div>
      </div>

      <!-- 学习的提示词 -->
      <div v-show="styleTab === 'learned'" class="style-grid">
        <div
          v-if="learnedSkills.length === 0"
          class="style-empty style-empty-text"
        >
          还没有学习过的提示词，请前往「我的提示词」页面学习。
        </div>
        <div
          v-for="l in learnedSkills"
          v-else
          :key="l.name"
          :class="['style-card', { selected: selectedStyleName === l.name }]"
          @click="selectStyle(l)"
        >
          <div class="style-card-head">
            <div class="style-card-avatar learned">{{ l.name.charAt(0) }}</div>
            <div class="style-card-title-wrap">
              <div class="style-card-title-row">
                <div class="style-card-title">{{ l.name }}</div>
              </div>
              <div class="style-card-meta">
                <span>学习 · {{ (l.createdAt || '').slice(0, 10) }}</span>
              </div>
            </div>
          </div>
          <div v-if="l.scope" class="style-card-scope-list">
            <span v-for="tag in parseScopeTags(l.scope)" :key="tag" class="style-card-scope">{{ tag }}</span>
          </div>
          <div class="style-card-prompt">{{ promptSummary(l.prompt) }}</div>
          <div class="style-card-footer">
            <button class="style-action-btn" @click.stop="openPromptModal(l)">
              查看完整提示词
            </button>
          </div>
        </div>
      </div>

      <!-- 收藏的提示词 -->
      <div v-show="styleTab === 'favorites'" class="style-grid">
        <div
          v-if="favoriteSkills.length === 0"
          class="style-empty style-empty-text"
        >
          还没有收藏的提示词，去
          <button class="style-empty-link" @click="goToSkillMarket">提示词市场</button>
          收藏喜欢的提示词吧。
        </div>
        <div
          v-for="f in favoriteSkills"
          v-else
          :key="f.id"
          :class="['style-card', { selected: selectedStyleName === f.name }]"
          @click="selectStyle(f)"
        >
          <div class="style-card-head">
            <div class="style-card-avatar">{{ f.name.charAt(0) }}</div>
            <div class="style-card-title-wrap">
              <div class="style-card-title-row">
                <div class="style-card-title">{{ f.name }}</div>
              </div>
              <div class="style-card-meta">by {{ f.creatorName }}</div>
            </div>
          </div>
          <div v-if="f.scope" class="style-card-scope-list">
            <span v-for="tag in parseScopeTags(f.scope)" :key="tag" class="style-card-scope">{{ tag }}</span>
          </div>
          <div class="style-card-prompt">{{ promptSummary(f.prompt) }}</div>
          <div class="style-card-footer">
            <button class="style-action-btn" @click.stop="openPromptModal(f)">
              查看完整提示词
            </button>
          </div>
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
import {
  systemSkills,
  mySkills,
  applySkill,
  learnedSkills,
  loadMySkills,
  loadLearnedSkills
} from '@/composables/useSkills.js'
import {
  favoriteSkills,
  loadFavoriteIds,
  useMarketSkill
} from '@/composables/useSkillMarket.js'
import { useCreateForm } from '../useCreateForm.js'

const { styleVisible } = useCreateForm()
const router = useRouter()

const styleTab = ref('my')
const selectedStyleName = ref(null)
const promptModalVisible = ref(false)
const viewingSkill = ref(null)

// 弹框打开时重置并加载我的提示词
watch(styleVisible, async (open) => {
  if (!open) return
  styleTab.value = 'my'
  selectedStyleName.value = null
  viewingSkill.value = null
  promptModalVisible.value = false
  await Promise.all([
    loadMySkills(),
    loadFavoriteIds()
  ])
})

const selectStyle = (s) => {
  selectedStyleName.value = s.name
}

const findSelectedSkill = () => {
  const name = selectedStyleName.value
  if (!name) return null
  return systemSkills.value.find(x => x.name === name)
    || mySkills.value.find(x => x.name === name)
    || learnedSkills.value.find(x => x.name === name)
    || favoriteSkills.value.find(x => x.name === name)
    || null
}

const applySkillLocal = () => {
  const s = findSelectedSkill()
  if (!s) return
  if (s.id && favoriteSkills.value.some(x => x.id === s.id)) {
    // 收藏的市场提示词：先记录使用 + 创作者收益，再应用到当前任务
    try { useMarketSkill(s.id) } catch (e) { console.warn('[useMarketSkill]', e?.message || e) }
  }
  applySkill(s)
  styleVisible.value = false
}

const useFromPromptModal = () => {
  if (!viewingSkill.value) return
  selectedStyleName.value = viewingSkill.value.name
  if (viewingSkill.value.id) {
    try { useMarketSkill(viewingSkill.value.id) } catch (e) { console.warn('[useMarketSkill]', e?.message || e) }
  }
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

.style-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 16px;
  padding: 16px;
  min-height: 200px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.2s;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  cursor: pointer;
}

.style-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.06);
  border-color: var(--color-primary);
}

.style-card.selected {
  border-color: var(--color-primary);
  background: var(--color-primary-bg);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.06);
}

.style-card-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.style-card-avatar {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: #fff0f2;
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
}

.style-card-avatar.learned {
  background: #fff5f7;
  color: var(--color-primary);
}

.style-card-title-wrap {
  flex: 1;
  min-width: 0;
}

.style-card-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.style-card-title {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1.35;
  word-break: break-all;
}

.style-card-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #8c8c8c;
}

.style-card-meta-dot {
  color: #d9d9d9;
  font-weight: 700;
}

.style-card-scope-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.style-card-scope {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  width: fit-content;
  font-size: 11px;
  color: var(--color-primary);
  background: #fff5f7;
  border: 1px solid #ffd1d9;
  padding: 2px 8px;
  border-radius: 6px;
}

.style-card-scope::before {
  content: '#';
  opacity: 0.8;
}

.style-card-prompt {
  font-size: 13px;
  color: #262626;
  line-height: 1.7;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1 0 auto;
  white-space: pre-line;
}

.style-card-footer {
  margin-top: 0;
  padding-top: 0;
  border-top: none;
}

.style-card-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.style-action-btn {
  padding: 4px 8px;
  background: transparent;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  color: #8c8c8c;
  cursor: pointer;
  transition: all 0.2s;
}

.style-action-btn:hover {
  color: var(--color-primary);
  background: var(--color-primary-bg);
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

body[data-theme="dark"] .style-card {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .style-card:hover {
  border-color: var(--color-primary);
}

body[data-theme="dark"] .style-card.selected {
  background: rgba(255, 36, 66, 0.12);
  border-color: var(--color-primary);
}

body[data-theme="dark"] .style-card-avatar {
  background: rgba(255, 36, 66, 0.12);
  color: #ff6b81;
}

body[data-theme="dark"] .style-card-avatar.learned {
  background: rgba(255, 36, 66, 0.12);
  color: #ff6b81;
}

body[data-theme="dark"] .style-card-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .style-card-meta {
  color: #a6a6a6;
}

body[data-theme="dark"] .style-card-meta-dot {
  color: #595959;
}

body[data-theme="dark"] .style-card-scope {
  background: rgba(255, 36, 66, 0.12);
  border-color: rgba(255, 36, 66, 0.25);
  color: #ff6b81;
}

body[data-theme="dark"] .style-card-prompt {
  background: transparent;
  color: #d9d9d9;
}

body[data-theme="dark"] .style-action-btn {
  background: transparent;
  border-color: transparent;
  color: #a6a6a6;
}

body[data-theme="dark"] .style-action-btn:hover {
  border-color: transparent;
  color: var(--color-primary);
  background: rgba(255, 36, 66, 0.12);
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
