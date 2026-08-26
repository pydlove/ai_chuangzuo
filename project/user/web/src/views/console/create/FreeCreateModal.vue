<template>
  <!-- 弹框模式 -->
  <a-modal
    v-if="!pageMode"
    :open="visible"
    title="自由创作"
    width="760px"
    :footer="null"
    :mask-closable="false"
    centered
    class="free-create-modal"
    @cancel="close"
  >
    <div class="free-create-body">
      <div class="free-create-tips">
        填写标题和核心观点，AI 将完全按照你的想法生成文章。
      </div>

      <div class="free-create-card" :class="{ focused: heroFocused }">
        <input
          v-model="customTitle"
          type="text"
          class="free-create-title"
          placeholder="输入标题或想法，例如：职场新人快速提升效率的 5 个方法"
          @focus="heroFocused = true"
          @blur="heroFocused = false"
        />
        <div class="free-create-textarea-wrap">
          <textarea
            ref="requirementEl"
            v-model="customRequirement"
            class="free-create-textarea"
            rows="4"
            placeholder="补充要求：观点、案例、情节、目标读者..."
            :maxlength="REQUIREMENT_MAX"
            @input="autoGrow"
            @focus="heroFocused = true"
            @blur="heroFocused = false"
          ></textarea>
          <div class="free-create-textarea-meta">
            <span class="free-create-char-count" :class="{ warning: requirementChars >= REQUIREMENT_MAX * 0.9 }">
              {{ requirementChars }}/{{ REQUIREMENT_MAX }}
            </span>
          </div>
        </div>

        <div class="free-create-divider"></div>

        <div class="free-create-chips">
          <button class="settings-chip" @click="wordCountVisible = true">
            <span>{{ currentWordCount.count }} 字 · {{ currentWordCount.label }}</span><span class="chip-caret">▾</span>
          </button>
          <button class="settings-chip" @click="styleVisible = true">
            <span>{{ currentSkill?.name || '选择提示词' }}</span><span class="chip-caret">▾</span>
          </button>
          <button class="settings-chip" @click="templateVisible = true">
            <span>{{ currentTemplate?.name || '选择导出模板' }}</span><span class="chip-caret">▾</span>
          </button>
        </div>

        <div class="free-create-actions">
          <a-button size="large" @click="close">取消</a-button>
          <a-button type="primary" size="large" :disabled="!canGenerate" @click="handleGenerate">
            <ThunderboltOutlined />
            生成文章
          </a-button>
        </div>
      </div>
    </div>

    <SkillModal />
    <WordCountModal />
    <TemplateModal />
  </a-modal>

  <!-- 页面模式（手机端二级页面） -->
  <div v-else class="free-create-page">
    <div class="free-create-page-header">
      <div class="free-create-page-back" @click="close">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6"></polyline>
        </svg>
      </div>
      <div class="free-create-page-title">自由创作</div>
      <div class="free-create-page-header-right">
        <!-- 右侧 icon 占位，后续替换为真实图标 -->
        <div class="free-create-header-icon-placeholder"></div>
      </div>
    </div>

    <div class="free-create-hero">
      <div class="free-create-hero-main">
        <img
          class="free-create-hero-logo"
          src="/assets/images/自由创作宣传页-v1.png"
          alt="自由创作"
        />
        <p class="free-create-hero-desc">
          填写标题和核心观点，AI 将完全按照你的想法生成文章。
        </p>
      </div>
      <div class="free-create-hero-icon-wrap">
        <img
          class="free-create-hero-icon"
          src="/assets/images/自由创作宣传页-v2.png"
          alt="自由创作"
        />
      </div>
    </div>

    <div class="free-create-page-body">
      <div class="free-create-card" :class="{ focused: heroFocused }">
        <input
          v-model="customTitle"
          type="text"
          class="free-create-title"
          placeholder="输入标题或想法，例如：职场新人快速提升效率的 5 个方法"
          @focus="heroFocused = true"
          @blur="heroFocused = false"
        />
        <div class="free-create-textarea-wrap">
          <textarea
            ref="requirementEl"
            v-model="customRequirement"
            class="free-create-textarea"
            rows="4"
            placeholder="补充要求：观点、案例、情节、目标读者..."
            :maxlength="REQUIREMENT_MAX"
            @input="autoGrow"
            @focus="heroFocused = true"
            @blur="heroFocused = false"
          ></textarea>
          <div class="free-create-textarea-meta">
            <span class="free-create-char-count" :class="{ warning: requirementChars >= REQUIREMENT_MAX * 0.9 }">
              {{ requirementChars }}/{{ REQUIREMENT_MAX }}
            </span>
          </div>
        </div>

        <div class="free-create-divider"></div>

        <div class="free-create-chips">
          <button class="settings-chip" @click="wordCountVisible = true">
            <span>{{ currentWordCount.count }} 字 · {{ currentWordCount.label }}</span><span class="chip-caret">▾</span>
          </button>
          <button class="settings-chip" @click="styleVisible = true">
            <span>{{ currentSkill?.name || '选择提示词' }}</span><span class="chip-caret">▾</span>
          </button>
          <button class="settings-chip" @click="templateVisible = true">
            <span>{{ currentTemplate?.name || '选择导出模板' }}</span><span class="chip-caret">▾</span>
          </button>
        </div>

        <div class="free-create-actions">
          <a-button type="primary" size="large" :disabled="!canGenerate" @click="handleGenerate">
            <ThunderboltOutlined />
            生成文章
          </a-button>
        </div>
      </div>
    </div>

    <SkillModal />
    <WordCountModal />
    <TemplateModal />
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { ThunderboltOutlined } from '@ant-design/icons-vue'
import { useCreateForm } from './useCreateForm.js'
import { platforms } from '@/composables/usePlatforms.js'
import { useGenerationQueue } from './useGenerationQueue.js'
import { currentSkill } from '@/composables/useSkills.js'
import { marketSkills } from '@/composables/useSkillMarket.js'
import { useExportTemplates } from '@/composables/useExportTemplates.js'
import { useBenefits } from '@/composables/useBenefits.js'
import { submitGeneration } from '@/api/generation.js'
import SkillModal from './modals/SkillModal.vue'
import WordCountModal from './modals/WordCountModal.vue'
import TemplateModal from './modals/TemplateModal.vue'

const props = defineProps({
  visible: Boolean,
  plan: Object,
  pageMode: Boolean
})
const emit = defineEmits(['update:visible', 'success'])

const router = useRouter()

const {
  customTitle, customRequirement,
  currentWordCount, selectedTemplateKey,
  styleVisible, wordCountVisible, templateVisible,
  clearForm
} = useCreateForm()

const { queueOpen, activeCount, loadQueue } = useGenerationQueue()
const { templates: apiTemplates, load: loadExportTemplates } = useExportTemplates()
const { benefits, planKey, loadBenefits } = useBenefits()

const REQUIREMENT_MAX = 200
const heroFocused = ref(false)
const requirementEl = ref(null)
const isReady = ref(false)

const quotaRemaining = computed(() => benefits.value['ai_article_quota']?.remaining ?? 0)
const currentTemplate = computed(() => apiTemplates.value.find(t => t.key === selectedTemplateKey.value) || apiTemplates.value[0])
const requirementChars = computed(() => customRequirement.value?.length || 0)
const canGenerate = computed(() => customTitle.value.trim() && customRequirement.value.trim())

const planPlatformKey = computed(() => {
  const planPlatform = props.plan?.platform
  if (!planPlatform) return 'wechat'
  const found = platforms.value.find(p => p.name === planPlatform)
  return found?.key || 'wechat'
})

onMounted(async () => {
  loadBenefits()
  await loadExportTemplates().catch(() => {})
  isReady.value = true
})

const autoGrow = () => {
  const el = requirementEl.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 300) + 'px'
}

const isCurrentSkillAvailable = () => {
  const skill = currentSkill.value
  if (!skill) return true
  if (!skill.id) return true
  const live = marketSkills.value.find(s => s.id === skill.id)
  if (!live) return false
  return live.status === 'approved'
}

const close = () => {
  if (props.pageMode) {
    router.back()
    return
  }
  emit('update:visible', false)
}

const handleGenerate = async () => {
  if (!props.pageMode && !props.visible) return
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
      platform: planPlatformKey.value,
      skillRef: currentSkill.value?.id || currentSkill.value?.name || '',
      wordCount: currentWordCount.value?.count || 800,
      template: currentTemplate.value?.key || 'wechat'
    })
    message.success('已加入生成队列')
    clearForm()
    requirementEl.value && (requirementEl.value.style.height = '')
    loadQueue()
    queueOpen.value = true
    loadBenefits()
    if (!props.pageMode) {
      emit('update:visible', false)
    } else {
      router.back()
    }
    emit('success', task)
  } catch (e) {
    message.error(e?.message || '提交失败，请稍后重试')
  }
}
</script>

<style scoped>
.free-create-modal :deep(.ant-modal-content) {
  border-radius: 16px;
  overflow: hidden;
}
.free-create-modal :deep(.ant-modal-body) {
  padding: 24px;
}
.free-create-body {
  padding: 8px 4px;
}
.free-create-tips {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: var(--space-md);
  line-height: 1.5;
}
.free-create-card {
  background: var(--color-bg-card);
  border-radius: 16px;
  padding: 20px 20px 16px;
  border: 1.5px solid transparent;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  transition: border-color 0.25s, box-shadow 0.25s;
}
.free-create-card.focused {
  border-color: var(--color-primary-light);
  box-shadow: 0 4px 24px rgba(7, 193, 96, 0.12);
}
.free-create-title {
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
.free-create-title::placeholder {
  color: var(--color-text-placeholder);
  font-weight: 400;
}
.free-create-textarea-wrap {
  position: relative;
}
.free-create-textarea {
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
.free-create-textarea::placeholder {
  color: var(--color-text-placeholder);
}
.free-create-textarea-meta {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding-top: 6px;
}
.free-create-char-count {
  font-size: 12px;
  color: var(--color-text-placeholder);
  transition: color 0.15s;
  min-width: 42px;
  text-align: right;
}
.free-create-char-count.warning {
  color: var(--color-error);
  font-weight: 500;
}
.free-create-divider {
  height: 1px;
  background: var(--color-border-light);
  margin: var(--space-md) 0;
}
.free-create-chips {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: var(--space-lg);
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
.free-create-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: var(--space-sm);
}
.free-create-actions .ant-btn-primary {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0 28px;
  font-weight: 600;
  box-shadow: 0 4px 14px rgba(7, 193, 96, 0.3);
}
.free-create-actions .ant-btn-primary :deep(.anticon) {
  color: #fff;
}
.free-create-actions .ant-btn-primary:hover,
.free-create-actions .ant-btn-primary:focus {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
  color: #fff;
  box-shadow: 0 6px 20px rgba(7, 193, 96, 0.4);
}
.free-create-actions .ant-btn-primary:disabled {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
  opacity: 0.5;
  box-shadow: none;
}

/* 页面模式 */
.free-create-page {
  min-height: 100%;
  background: var(--color-bg-page);
}
.free-create-page-header {
  display: none;
}
.free-create-hero {
  display: none;
}
.free-create-page-body {
  padding: 16px;
}

@media (max-width: 768px) {
  .free-create-chips {
    flex-wrap: nowrap;
    overflow-x: auto;
    padding-bottom: 4px;
  }
  .free-create-chips .settings-chip {
    flex-shrink: 0;
  }
  .free-create-actions {
    flex-direction: column;
    align-items: stretch;
  }
  .free-create-actions .ant-btn {
    width: 100%;
  }

  .free-create-page {
    background: var(--color-bg-card);
  }
  .free-create-page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    position: relative;
    height: 56px;
    padding: 0;
    margin: 0;
    background: var(--color-bg-card);
    border-bottom: 1px solid var(--color-border-light);
  }
  .free-create-page-back {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 56px;
    height: 56px;
    font-size: 14px;
    color: var(--color-text-secondary);
    cursor: pointer;
  }
  .free-create-page-back svg {
    width: 24px;
    height: 24px;
  }
  .free-create-page-title {
    position: absolute;
    left: 50%;
    transform: translateX(-50%);
    font-size: 17px;
    font-weight: 600;
    color: var(--color-text-primary);
    line-height: 56px;
  }
  .free-create-page-header-right {
    width: 56px;
    height: 56px;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  .free-create-header-icon-placeholder {
    width: 24px;
    height: 24px;
    border-radius: 6px;
    background: var(--color-bg-page);
  }

  .free-create-hero {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin: 0;
    padding: 16px 12px;
    background: linear-gradient(135deg, #fff5f7 0%, #ffe8ed 50%, #fff0f3 100%);
    border-bottom: 1px solid var(--color-border-light);
    position: relative;
    overflow: hidden;
  }
  .free-create-hero::before,
  .free-create-hero::after {
    content: '';
    position: absolute;
    border-radius: 50%;
    border: 16px solid rgba(255, 36, 66, 0.08);
    pointer-events: none;
  }
  .free-create-hero::before {
    width: 120px;
    height: 120px;
    bottom: -40px;
    left: -30px;
  }
  .free-create-hero::after {
    width: 90px;
    height: 90px;
    top: -30px;
    left: 50%;
    border-width: 14px;
    border-color: rgba(255, 36, 66, 0.06);
  }
  .free-create-hero-main {
    flex: 1;
    min-width: 0;
  }
  .free-create-hero-logo {
    height: 36px;
    width: auto;
    display: block;
  }
  .free-create-hero-desc {
    margin: 8px 0 0;
    font-size: 12px;
    line-height: 1.5;
    color: var(--color-text-secondary);
  }
  .free-create-hero-icon-wrap {
    width: 110px;
    height: 110px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }
  .free-create-hero-icon {
    width: 110px;
    height: 110px;
    object-fit: contain;
    flex-shrink: 0;
  }
  .free-create-page-body {
    padding: 16px 12px calc(16px + env(safe-area-inset-bottom));
  }
  .free-create-page-body .free-create-tips {
    display: none;
  }
}
</style>
