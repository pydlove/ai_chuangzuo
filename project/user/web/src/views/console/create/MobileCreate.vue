<template>
  <div class="mobile-create">
    <header class="mc-header">
      <div class="mc-header__brand">
        <img
          src="https://foruda.gitee.com/images/1782986808430461164/e0ab39dc_8060302.png"
          alt="爱创作工坊"
          class="mc-header__logo"
        />
        <span class="mc-header__name">爱创作工坊</span>
      </div>
      <button class="mc-icon-btn" @click="queueOpen = true">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/>
        </svg>
        <span v-if="activeCount > 0" class="mc-icon-btn__badge">{{ activeCount }}</span>
      </button>
    </header>

    <section class="mc-hero">
      <h1 class="mc-hero__title">今天想写什么？</h1>
      <p class="mc-hero__subtitle">输入标题和想法，AI 3分钟帮你生成高质量文章</p>
    </section>

    <section class="mc-card" :class="{ focused: heroFocused }">
      <input
        v-model="customTitle"
        type="text"
        class="mc-title-input"
        placeholder="输入标题或想法…"
        @focus="heroFocused = true"
        @blur="heroFocused = false"
      />
      <div class="mc-textarea-wrap">
        <textarea
          ref="requirementEl"
          v-model="customRequirement"
          class="mc-textarea"
          rows="4"
          placeholder="补充要求：观点、案例、情节..."
          :maxlength="REQUIREMENT_MAX"
          @input="autoGrow"
          @focus="heroFocused = true"
          @blur="heroFocused = false"
        />
      </div>
      <div class="mc-card__footer">
        <button class="mc-expand-btn" type="button" @click="openRequirementFullscreen">
          扩写
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M8 3H5a2 2 0 0 0-2 2v3"/>
            <path d="M21 8V5a2 2 0 0 0-2-2h-3"/>
            <path d="M3 16v3a2 2 0 0 0 2 2h3"/>
            <path d="M16 21h3a2 2 0 0 0 2-2v-3"/>
          </svg>
        </button>
        <span class="mc-char-count" :class="{ warning: requirementChars >= REQUIREMENT_MAX * 0.9 }">
          {{ requirementChars }}/{{ REQUIREMENT_MAX }}
        </span>
      </div>
    </section>

    <section class="mc-quick-settings">
      <button class="mc-quick-item" @click="platformVisible = true">
        <span class="mc-quick-item__icon" :style="{ background: platformColor }">{{ currentPlatform.name[0] }}</span>
        <span class="mc-quick-item__label">{{ currentPlatform.name }}</span>
        <span class="mc-quick-item__caret">›</span>
      </button>
      <button class="mc-quick-item" @click="wordCountVisible = true">
        <span class="mc-quick-item__icon mc-quick-item__icon--blue">字</span>
        <span class="mc-quick-item__label">{{ currentWordCount.count }}字</span>
        <span class="mc-quick-item__caret">›</span>
      </button>
      <button class="mc-quick-item" @click="styleVisible = true">
        <span class="mc-quick-item__icon mc-quick-item__icon--purple">技</span>
        <span class="mc-quick-item__label truncate">{{ currentSkill?.name || '提示词' }}</span>
        <span class="mc-quick-item__caret">›</span>
      </button>
      <button class="mc-quick-item" @click="templateVisible = true">
        <span class="mc-quick-item__icon mc-quick-item__icon--orange">版</span>
        <span class="mc-quick-item__label truncate">{{ currentTemplate?.name || '模板' }}</span>
        <span class="mc-quick-item__caret">›</span>
      </button>
    </section>

    <section class="mc-inspiration">
      <TopicCapsules ref="topicCapsulesRef" />
    </section>

    <div class="mc-action-bar">
      <button class="mc-draft-btn" @click="handleSaveDraft">保存草稿</button>
      <button class="mc-generate-btn" :disabled="submitting" @click="handleGenerate">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/>
        </svg>
        {{ submitting ? '提交中…' : '生成文章' }}
      </button>
    </div>

    <transition name="slide-up">
      <div v-show="requirementFullVisible" class="mc-fullscreen">
        <div class="mc-fullscreen__header">
          <button class="mc-fullscreen__back" @click="closeRequirementFullscreen">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M19 12H5"/>
              <path d="M12 19l-7-7 7-7"/>
            </svg>
            返回
          </button>
          <span class="mc-fullscreen__title">编辑观点</span>
          <button class="mc-fullscreen__save" @click="saveRequirementFullscreen">保存</button>
        </div>

        <div class="mc-fullscreen__body">
          <textarea
            ref="fullRequirementEl"
            v-model="fullRequirement"
            class="mc-fullscreen__textarea"
            placeholder="补充要求：观点、案例、情节..."
            :maxlength="REQUIREMENT_MAX"
          />
        </div>

        <div class="mc-fullscreen__footer">
          <span class="mc-char-count" :class="{ warning: fullRequirementChars >= REQUIREMENT_MAX * 0.9 }">
            {{ fullRequirementChars }}/{{ REQUIREMENT_MAX }}
          </span>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import TopicCapsules from './TopicCapsules.vue'
import { useCreateForm } from './useCreateForm.js'
import { useGenerationQueue } from './useGenerationQueue.js'
import { currentSkill } from '@/composables/useSkills.js'
import { useExportTemplates } from '@/composables/useExportTemplates.js'
import { useBenefits } from '@/composables/useBenefits.js'
import { useConfirm } from '@/composables/useConfirm.js'
import { submitGeneration } from '@/api/generation.js'
import { saveDraft } from '@/api/draft.js'
import { getAsyncSubmittedText } from '@/constants/aiMessages.js'

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
const { confirm } = useConfirm()

const quotaRemaining = computed(() => benefits.value['ai_article_quota']?.remaining ?? 0)

const currentTemplate = computed(() => apiTemplates.value.find(t => t.key === selectedTemplateKey.value) || apiTemplates.value[0])

const platformColorMap = {
  wechat: '#07c160',
  xiaohongshu: '#ff2442',
  toutiao: '#f04142',
  baijiahao: '#389e0d',
  douyin: '#161823',
  kuaishou: '#ff6600',
  zhihu: '#0066ff',
  bilibili: '#00a1d6'
}
const platformColor = computed(() => platformColorMap[currentPlatform.value?.key] || '#ff2442')

const heroFocused = ref(false)
const requirementEl = ref(null)
const topicCapsulesRef = ref(null)
const REQUIREMENT_MAX = 200
const submitting = ref(false)
const requirementFullVisible = ref(false)
const fullRequirement = ref('')
const fullRequirementEl = ref(null)

const requirementChars = computed(() => customRequirement.value?.length || 0)
const fullRequirementChars = computed(() => fullRequirement.value?.length || 0)

const autoGrow = () => {
  const el = requirementEl.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 240) + 'px'
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
    // 保存失败时保持编辑状态，由用户重试
  }
}

const handleGenerate = async () => {
  if (planKey.value === 'free') {
    confirm({
      title: '需要订阅套餐',
      content: '订阅套餐后即可使用 AI 生成文章，是否去订阅？',
      okText: '去订阅',
      cancelText: '取消',
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
    confirm({
      title: '额度已用完',
      content: '本月额度已用完，升级会员可获得更多额度，是否去升级？',
      okText: '去升级',
      cancelText: '取消',
      onOk: () => window.open('/pricing', '_blank')
    })
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    const task = await submitGeneration({
      title: customTitle.value,
      description: customRequirement.value,
      platform: currentPlatform.value?.key || '',
      skillRef: currentSkill.value?.id || currentSkill.value?.name || '',
      wordCount: currentWordCount.value?.count || 800,
      template: currentTemplate.value?.key || 'wechat'
    })
    message.success(getAsyncSubmittedText('已加入生成队列'))
    topicCapsulesRef.value?.markUsed(task?.id)
    clearForm()
    if (requirementEl.value) requirementEl.value.style.height = ''
    loadQueue()
    queueOpen.value = true
    loadBenefits()
  } catch (e) {
    message.error(e?.message || '提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.mobile-create {
  min-height: 100%;
  padding: 12px 18px calc(140px + env(safe-area-inset-bottom));
  background:
    radial-gradient(600px 320px at 20% -80px, rgba(255, 36, 66, 0.10), transparent 70%),
    radial-gradient(500px 280px at 110% 10%, rgba(0, 103, 255, 0.06), transparent 70%),
    linear-gradient(180deg, #fff8f9 0%, #ffffff 35%, #f7f7f8 100%);
  box-sizing: border-box;
}

body[data-theme="dark"] .mobile-create {
  background:
    radial-gradient(600px 320px at 20% -80px, rgba(255, 36, 66, 0.14), transparent 70%),
    radial-gradient(500px 280px at 110% 10%, rgba(0, 103, 255, 0.08), transparent 70%),
    linear-gradient(180deg, #1a1214 0%, #141414 35%, #0f0f0f 100%);
}

/* 顶部栏 */
.mc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
  min-height: 40px;
}

.mc-header__brand {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mc-header__logo {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  object-fit: contain;
}

.mc-header__name {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.mc-icon-btn {
  position: relative;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.85);
  color: var(--color-text-secondary);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.2s;
}

.mc-icon-btn:active {
  transform: scale(0.94);
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.mc-icon-btn__badge {
  position: absolute;
  top: -2px;
  right: -2px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: var(--color-primary);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(255, 36, 66, 0.35);
}

/* 标题区 */
.mc-hero {
  margin-bottom: 22px;
}

.mc-hero__title {
  font-size: 28px;
  font-weight: 800;
  color: var(--color-text-primary);
  line-height: 1.2;
  margin: 0 0 6px;
  letter-spacing: -0.5px;
}

.mc-hero__subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0;
}

/* 输入卡片 */
.mc-card {
  background: #fff;
  border-radius: 24px;
  padding: 20px 20px 14px;
  box-shadow:
    0 10px 40px rgba(0, 0, 0, 0.06),
    0 2px 8px rgba(0, 0, 0, 0.03);
  border: 1.5px solid transparent;
  transition: all 0.25s ease;
}

.mc-card.focused {
  border-color: rgba(255, 36, 66, 0.25);
  box-shadow:
    0 12px 48px rgba(255, 36, 66, 0.12),
    0 2px 8px rgba(0, 0, 0, 0.03);
  transform: translateY(-1px);
}

body[data-theme="dark"] .mc-card {
  background: #1f1f1f;
  box-shadow:
    0 10px 40px rgba(0, 0, 0, 0.25),
    0 2px 8px rgba(0, 0, 0, 0.15);
}

.mc-title-input {
  width: 100%;
  border: none;
  outline: none;
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text-primary);
  background: transparent;
  padding: 6px 0 14px;
  box-sizing: border-box;
  line-height: 1.35;
}

.mc-title-input::placeholder {
  color: var(--color-text-placeholder);
  font-weight: 400;
}

.mc-textarea-wrap {
  position: relative;
}

.mc-textarea {
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  font-size: 15px;
  line-height: 1.75;
  color: var(--color-text-regular);
  background: transparent;
  min-height: 110px;
  max-height: 240px;
  overflow-y: auto;
  font-family: inherit;
  box-sizing: border-box;
  padding: 0;
}

.mc-textarea::placeholder {
  color: var(--color-text-placeholder);
}

.mc-card__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed var(--color-border-light);
}

.mc-expand-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  color: var(--color-text-secondary);
  font-size: 13px;
  cursor: pointer;
  padding: 4px 0;
}

.mc-expand-btn:active {
  color: var(--color-primary);
}

.mc-char-count {
  font-size: 12px;
  color: var(--color-text-placeholder);
  min-width: 42px;
  text-align: right;
}

.mc-char-count.warning {
  color: var(--color-error);
  font-weight: 500;
}

/* 快捷设置 */
.mc-quick-settings {
  display: flex;
  gap: 8px;
  padding: 20px 0 4px;
}

.mc-quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 0;
  padding: 0 2px;
  background: none;
  border: none;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.mc-quick-item__icon {
  width: 46px;
  height: 46px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transition: transform 0.15s;
}

.mc-quick-item:active .mc-quick-item__icon {
  transform: scale(0.94);
}

.mc-quick-item__icon--blue { background: linear-gradient(135deg, #448aff, #2979ff); }
.mc-quick-item__icon--purple { background: linear-gradient(135deg, #ab47bc, #8e24aa); }
.mc-quick-item__icon--orange { background: linear-gradient(135deg, #ff9800, #f57c00); }

.mc-quick-item__label {
  width: 100%;
  font-size: 12px;
  color: var(--color-text-regular);
  text-align: center;
}

.mc-quick-item__caret {
  font-size: 12px;
  color: var(--color-text-placeholder);
  transform: rotate(90deg);
}

.truncate {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 灵感胶囊 */
.mc-inspiration {
  margin-top: 10px;
}

/* 底部操作栏 */
.mc-action-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: calc(60px + env(safe-area-inset-bottom));
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 18px calc(12px + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(12px);
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 -6px 24px rgba(0, 0, 0, 0.04);
  z-index: 40;
  box-sizing: border-box;
}

body[data-theme="dark"] .mc-action-bar {
  background: rgba(24, 24, 24, 0.96);
  border-top-color: rgba(255, 255, 255, 0.06);
  box-shadow: 0 -6px 24px rgba(0, 0, 0, 0.35);
}

.mc-draft-btn {
  flex-shrink: 0;
  padding: 12px 16px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: 14px;
  color: var(--color-text-secondary);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.mc-draft-btn:active {
  background: var(--color-primary-light);
  color: var(--color-primary);
  border-color: var(--color-primary-light);
}

.mc-generate-btn {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px 20px;
  background: linear-gradient(135deg, #ff2442 0%, #ff5c7a 100%);
  color: #fff;
  border: none;
  border-radius: 16px;
  font-size: 16px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 8px 22px rgba(255, 36, 66, 0.38);
}

.mc-generate-btn.small {
  flex: 0 0 auto;
  padding: 8px 22px;
  font-size: 14px;
  border-radius: 12px;
  box-shadow: 0 4px 14px rgba(255, 36, 66, 0.3);
}

.mc-generate-btn:active {
  transform: translateY(1px);
  box-shadow: 0 4px 14px rgba(255, 36, 66, 0.3);
}

/* 全屏编辑（手机端底部滑上） */
.slide-up-enter-active,
.slide-up-leave-active {
  transition: transform 0.28s cubic-bezier(0.32, 0.72, 0, 1);
}

.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(100%);
}

.mc-fullscreen {
  position: fixed;
  inset: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  z-index: 100;
}

body[data-theme="dark"] .mc-fullscreen {
  background: #141414;
}

.mc-fullscreen__header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 52px;
  padding: 0 14px;
  border-bottom: 1px solid var(--color-border-light);
  background: #fff;
}

body[data-theme="dark"] .mc-fullscreen__header {
  background: #141414;
  border-bottom-color: #303030;
}

.mc-fullscreen__back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  color: var(--color-text-secondary);
  font-size: 15px;
  cursor: pointer;
  padding: 6px 0;
}

.mc-fullscreen__back:active {
  color: var(--color-primary);
}

.mc-fullscreen__title {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.mc-fullscreen__save {
  background: none;
  border: none;
  color: var(--color-primary);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  padding: 6px 4px;
}

.mc-fullscreen__save:active {
  opacity: 0.7;
}

.mc-fullscreen__body {
  flex: 1;
  overflow: hidden;
  padding: 16px 18px;
}

.mc-fullscreen__textarea {
  width: 100%;
  height: 100%;
  border: none;
  outline: none;
  resize: none;
  font-size: 17px;
  line-height: 1.85;
  color: var(--color-text-regular);
  background: transparent;
  font-family: inherit;
  box-sizing: border-box;
}

.mc-fullscreen__textarea::placeholder {
  color: var(--color-text-placeholder);
}

.mc-fullscreen__footer {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 10px 18px calc(10px + env(safe-area-inset-bottom));
  border-top: 1px solid var(--color-border-light);
  background: #fff;
}

body[data-theme="dark"] .mc-fullscreen__footer {
  background: #141414;
  border-top-color: #303030;
}

body[data-theme="dark"] .mc-icon-btn {
  background: rgba(40, 40, 40, 0.9);
  color: #a6a6a6;
}

body[data-theme="dark"] .mc-hero__title {
  color: #f0f0f0;
}

body[data-theme="dark"] .mc-quick-item__label {
  color: #d9d9d9;
}

body[data-theme="dark"] .mc-quick-item {
  background: transparent;
}

body[data-theme="dark"] .mc-quick-item__icon {
  background: transparent !important;
  box-shadow: none;
}

body[data-theme="dark"] .mc-expand-btn,
body[data-theme="dark"] .mc-char-count {
  color: #8c8c8c;
}
</style>
