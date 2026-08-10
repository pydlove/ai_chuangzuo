<template>
  <div class="minimal-panel">
    <!-- 顶部行（额度/队列/模式切换统一在 CreateIndex 顶部行） -->
    <div class="minimal-topbar">
      <h2 class="create-title">开始创作</h2>
    </div>

    <!-- 一体化输入卡片 -->
    <div class="hero-card" :class="{ focused: heroFocused }">
      <input
        v-model="customTitle"
        type="text"
        class="hero-title-input"
        placeholder="输入标题或想法，例如：职场新人快速提升效率的 5 个方法"
        @focus="heroFocused = true"
        @blur="heroFocused = false"
      />
      <div class="hero-textarea-wrap">
        <textarea
          ref="requirementEl"
          v-model="customRequirement"
          class="hero-textarea"
          rows="4"
          placeholder="补充要求：观点、案例、情节..."
          :maxlength="REQUIREMENT_MAX"
          @input="autoGrow"
          @focus="heroFocused = true"
          @blur="heroFocused = false"
        ></textarea>
        <div class="hero-textarea-meta">
          <button class="hero-textarea-fullscreen" type="button" title="全屏编辑" @click="openRequirementFullscreen">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M8 3H5a2 2 0 0 0-2 2v3"/>
              <path d="M21 8V5a2 2 0 0 0-2-2h-3"/>
              <path d="M3 16v3a2 2 0 0 0 2 2h3"/>
              <path d="M16 21h3a2 2 0 0 0 2-2v-3"/>
            </svg>
          </button>
          <span class="hero-char-count" :class="{ warning: requirementChars >= REQUIREMENT_MAX * 0.9 }">
            {{ requirementChars }}/{{ REQUIREMENT_MAX }}
          </span>
        </div>
      </div>

      <div class="hero-divider"></div>

      <div class="hero-chips">
        <button class="settings-chip" @click="platformVisible = true">
          <span>{{ currentPlatform.name }}</span><span class="chip-caret">▾</span>
        </button>
        <button class="settings-chip" @click="wordCountVisible = true">
          <span>{{ currentWordCount.count }} 字 · {{ currentWordCount.label }}</span><span class="chip-caret">▾</span>
        </button>
        <button class="settings-chip" @click="styleVisible = true">
          <span>{{ currentSkill?.name || '选择提示词' }}</span><span class="chip-caret">▾</span>
        </button>
        <button class="settings-chip" @click="templateVisible = true">
          <span>{{ currentTemplate?.name }}</span><span class="chip-caret">▾</span>
        </button>
      </div>

      <div class="hero-action-row">
        <div class="hero-action-left">
          <button class="action-link" @click="handleSaveDraft">保存草稿</button>
          <button class="action-link" @click="router.push('/console/works?tab=drafts')">草稿箱</button>
        </div>
        <button class="hero-generate-btn" @click="handleGenerate">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/>
          </svg>
          生成文章
        </button>
      </div>
    </div>

    <!-- 灵感胶囊 -->
    <TopicCapsules ref="topicCapsulesRef" />

    <!-- 观点全屏编辑弹框 -->
    <a-modal
      v-model:open="requirementFullVisible"
      title="编辑观点"
      :width="720"
      :footer="null"
      :mask-closable="false"
      centered
      wrap-class-name="requirement-fullscreen-modal"
      @cancel="closeRequirementFullscreen"
    >
      <div class="requirement-fullscreen-body">
        <textarea
          ref="fullRequirementEl"
          v-model="fullRequirement"
          class="requirement-fullscreen-textarea"
          placeholder="补充要求：观点、案例、情节..."
          :maxlength="REQUIREMENT_MAX"
        ></textarea>
        <div class="requirement-fullscreen-footer">
          <span class="hero-char-count" :class="{ warning: fullRequirementChars >= REQUIREMENT_MAX * 0.9 }">
            {{ fullRequirementChars }}/{{ REQUIREMENT_MAX }}
          </span>
          <div class="requirement-fullscreen-actions">
            <button class="action-link" @click="closeRequirementFullscreen">取消</button>
            <button class="hero-generate-btn" @click="saveRequirementFullscreen">保存</button>
          </div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import TopicCapsules from './TopicCapsules.vue'
import { useCreateForm } from './useCreateForm.js'
import { useGenerationQueue } from './useGenerationQueue.js'
import { currentSkill } from '@/composables/useSkills.js'
import { marketSkills } from '@/composables/useSkillMarket.js'
import { useExportTemplates } from '@/composables/useExportTemplates.js'
import { useBenefits } from '@/composables/useBenefits.js'
import { submitGeneration } from '@/api/generation.js'
import { saveDraft } from '@/api/draft.js'

const router = useRouter()
const {
  createMode, customTitle, customRequirement,
  currentPlatform, currentWordCount, selectedTemplateKey,
  platformVisible, wordCountVisible, styleVisible, templateVisible,
  clearForm
} = useCreateForm()
const { queueOpen, activeCount, loadQueue } = useGenerationQueue()
const { templates: apiTemplates } = useExportTemplates()
const { benefits, planKey, loadBenefits } = useBenefits()

const quotaRemaining = computed(() => benefits.value['ai_article_quota']?.remaining ?? 0)

const currentTemplate = computed(() => apiTemplates.value.find(t => t.key === selectedTemplateKey.value) || apiTemplates.value[0])

const heroFocused = ref(false)
const requirementEl = ref(null)
const topicCapsulesRef = ref(null)
const REQUIREMENT_MAX = 200
const requirementFullVisible = ref(false)
const fullRequirement = ref('')
const fullRequirementEl = ref(null)

const requirementChars = computed(() => customRequirement.value?.length || 0)
const fullRequirementChars = computed(() => fullRequirement.value?.length || 0)

const isCurrentSkillAvailable = () => {
  const skill = currentSkill.value
  if (!skill) return true
  // 非市场提示词（没有 id）不在这里拦截
  if (!skill.id) return true
  // 以提示词市场实时列表为准：找不到或状态不是 approved 视为不可用
  const live = marketSkills.value.find(s => s.id === skill.id)
  if (!live) return false
  return live.status === 'approved'
}

const autoGrow = () => {
  const el = requirementEl.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 300) + 'px'
}

const openRequirementFullscreen = () => {
  fullRequirement.value = customRequirement.value || ''
  requirementFullVisible.value = true
  nextTick(() => {
    const el = fullRequirementEl.value
    if (el) {
      el.focus()
      el.setSelectionRange(fullRequirement.value.length, fullRequirement.value.length)
    }
  })
}

const closeRequirementFullscreen = () => {
  requirementFullVisible.value = false
}

const saveRequirementFullscreen = () => {
  customRequirement.value = fullRequirement.value
  requirementFullVisible.value = false
  nextTick(autoGrow)
}

const handleSaveDraft = async () => {
  try {
    await saveDraft({
      customTitle: customTitle.value,
      customRequirement: customRequirement.value,
      platform: currentPlatform.value?.name,
      wordCount: currentWordCount.value?.count,
      style: currentSkill.value?.name,
      template: currentTemplate.value?.name,
      createMode: createMode.value
    })
    message.success('草稿已保存')
  } catch (e) {
    console.warn('保存草稿失败', e)
  }
}

const handleGenerate = async () => {
  if (planKey.value === 'free') {
    Modal.confirm({
      title: '需要订阅套餐',
      content: '订阅套餐后即可使用 AI 生成文章，是否去订阅？',
      okText: '去订阅',
      cancelText: '取消',
      centered: true,
      wrapClassName: 'membership-confirm-modal',
      onOk: () => window.open('/pricing', '_blank')
    })
    return
  }
  if (!customTitle.value.trim()) {
    message.warning('请输入文章标题')
    return
  }
  if (!customRequirement.value.trim()) {
    message.warning('请补充你的核心观点和要求')
    return
  }
  if (quotaRemaining.value <= 0) {
    Modal.confirm({
      title: '额度已用完',
      content: '本月额度已用完，升级会员可获得更多额度，是否去升级？',
      okText: '去升级',
      cancelText: '取消',
      centered: true,
      onOk: () => window.open('/pricing', '_blank')
    })
    return
  }
  if (!isCurrentSkillAvailable()) {
    message.warning('该提示词已下架或不可用，请重新选择')
    return
  }
  try {
    const task = await submitGeneration({
      title: customTitle.value,
      description: customRequirement.value,
      platform: currentPlatform.value?.key || '',
      skillRef: currentSkill.value?.id || currentSkill.value?.name || '',
      wordCount: currentWordCount.value?.count || 800,
      template: currentTemplate.value?.key || 'wechat'
    })
    message.success('已加入生成队列')
    topicCapsulesRef.value?.markUsed(task?.id)
    clearForm()
    requirementEl.value && (requirementEl.value.style.height = '')
    loadQueue()
    queueOpen.value = true
    loadBenefits() // 刷新本月剩余额度
  } catch (e) {
    message.error(e?.message || '提交失败，请稍后重试')
  }
}
</script>

<style scoped>
.minimal-panel {
  max-width: 760px;
  margin: 0 auto;
  width: 100%;
}

.minimal-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.create-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--color-text-primary);
}

/* 一体化输入卡片 */
.hero-card {
  background: var(--color-bg-card);
  border-radius: 16px;
  padding: 20px 20px 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  border: 1.5px solid transparent;
  transition: border-color 0.25s, box-shadow 0.25s;
}

.hero-card.focused {
  border-color: var(--color-primary-light);
  box-shadow: 0 4px 24px rgba(255, 36, 66, 0.10);
}

.hero-title-input {
  width: 100%;
  border: none;
  outline: none;
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
  background: transparent;
  padding: 4px 0 10px;
  box-sizing: border-box;
}

.hero-title-input::placeholder {
  color: var(--color-text-placeholder);
  font-weight: 400;
}

.hero-textarea-wrap {
  position: relative;
}

.hero-textarea {
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  font-size: 14px;
  line-height: 1.7;
  color: var(--color-text-regular);
  background: transparent;
  min-height: 112px;
  max-height: 300px;
  overflow-y: auto;
  font-family: inherit;
  box-sizing: border-box;
}

.hero-textarea::placeholder {
  color: var(--color-text-placeholder);
}

.hero-textarea-meta {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  padding-top: 6px;
}

.hero-textarea-fullscreen {
  width: 26px;
  height: 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: var(--color-text-placeholder);
  cursor: pointer;
  transition: all 0.15s;
}

.hero-textarea-fullscreen:hover {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.hero-char-count {
  font-size: 12px;
  color: var(--color-text-placeholder);
  transition: color 0.15s;
  min-width: 42px;
  text-align: right;
}

.hero-char-count.warning {
  color: var(--color-error);
  font-weight: 500;
}

.hero-divider {
  height: 1px;
  background: var(--color-border-light);
  margin: 12px 0;
}

.hero-chips {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.settings-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: 16px;
  color: var(--color-text-regular);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}

.settings-chip:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-bg);
}

.chip-caret {
  font-size: 10px;
  color: var(--color-text-placeholder);
}

.hero-action-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.hero-action-left {
  display: flex;
  gap: 8px;
}

.action-link {
  display: inline-flex;
  align-items: center;
  background: none;
  border: none;
  color: var(--color-text-secondary);
  font-size: 14px;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
}

.action-link:hover {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.hero-generate-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 36px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 14px rgba(255, 36, 66, 0.3);
  flex-shrink: 0;
}

.hero-generate-btn:hover {
  background: var(--color-primary-hover);
  box-shadow: 0 6px 20px rgba(255, 36, 66, 0.4);
  transform: translateY(-1px);
}

.hero-generate-btn:active {
  transform: translateY(0);
}

/* 全屏编辑弹框 */
.requirement-fullscreen-modal .ant-modal-content {
  border-radius: 16px;
  overflow: hidden;
}

.requirement-fullscreen-modal .ant-modal-header {
  border-bottom: 1px solid var(--color-border-light);
  padding: 16px 24px;
}

.requirement-fullscreen-modal .ant-modal-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.requirement-fullscreen-modal .ant-modal-close {
  top: 14px;
  right: 16px;
}

.requirement-fullscreen-modal .ant-modal-body {
  padding: 0;
}

.requirement-fullscreen-body {
  height: 420px;
  display: flex;
  flex-direction: column;
  padding: 20px 24px 16px;
  box-sizing: border-box;
}

.requirement-fullscreen-textarea {
  flex: 1;
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  font-size: 15px;
  line-height: 1.8;
  color: var(--color-text-regular);
  background: transparent;
  font-family: inherit;
  box-sizing: border-box;
}

.requirement-fullscreen-textarea::placeholder {
  color: var(--color-text-placeholder);
}

.requirement-fullscreen-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 14px;
  border-top: 1px solid var(--color-border-light);
  gap: 16px;
}

.requirement-fullscreen-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.requirement-fullscreen-actions .hero-generate-btn {
  padding: 8px 24px;
  font-size: 14px;
}

@media (max-width: 768px) {
  .minimal-panel { max-width: 100%; }

  .create-title { font-size: 20px; }

  .minimal-topbar-right { gap: 6px; }

  .minimal-topbar-right .topbar-btn { font-size: 12px; padding: 5px 10px; }

  .hero-chips { flex-wrap: nowrap; overflow-x: auto; padding-bottom: 4px; }

  .hero-chips .settings-chip { flex-shrink: 0; }

  .quota-text { display: none; }  /* 移动端额度并入抽屉顶部 */

  .hero-action-row { flex-direction: column; align-items: stretch; gap: 12px; }

  .hero-action-left { justify-content: flex-start; }

  .hero-generate-btn { width: 100%; }
}
</style>
