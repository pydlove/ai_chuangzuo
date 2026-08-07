<template>
  <div class="create-index">
    <div v-if="!isMobile" class="topbar-right">
      <span class="quota-text">本月剩余 <strong>{{ quotaRemaining }}</strong> / {{ quotaTotal }} 次</span>
      <button class="topbar-btn" @click="queueOpen = true">
        队列<template v-if="activeCount > 0">（{{ activeCount }}）</template>
      </button>
    </div>

    <MobileCreate v-if="isMobile" />
    <MinimalPanel v-else />

    <QueueDrawer v-model:open="queueOpen" />
    <PlatformModal />
    <WordCountModal />
    <SkillModal />
    <TemplateModal />
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useRouter, useRoute } from 'vue-router'
import { useDevice } from '@/composables/useDevice.js'
import {
  currentSkill,
  applySkill as applySkillShared,
  loadSystemSkills,
  loadMySkills,
  loadLearnedSkills,
  restoreLastSkill
} from '@/composables/useSkills.js'
import { marketSkills, loadMarketSkills, loadFavoriteSkills } from '@/composables/useSkillMarket.js'
import { useBenefits } from '@/composables/useBenefits.js'
import { useExportTemplates } from '@/composables/useExportTemplates.js'
import { platforms, wordCountPresets, useCreateForm } from './create/useCreateForm.js'
import { useGenerationQueue } from './create/useGenerationQueue.js'
import MinimalPanel from './create/MinimalPanel.vue'
import MobileCreate from './create/MobileCreate.vue'
import QueueDrawer from './create/QueueDrawer.vue'
import PlatformModal from './create/modals/PlatformModal.vue'
import WordCountModal from './create/modals/WordCountModal.vue'
import SkillModal from './create/modals/SkillModal.vue'
import TemplateModal from './create/modals/TemplateModal.vue'

const router = useRouter()
const route = useRoute()
const { isMobile } = useDevice()

// 导出模板（从 API 加载）
const { templates: apiTemplates, load: loadExportTemplates } = useExportTemplates()
const allTemplates = computed(() => apiTemplates.value)

// 创作表单共享状态（composable 单例）
const {
  customTitle, customRequirement,
  currentPlatform, currentWordCount, selectedTemplateKey
} = useCreateForm()

// 生成队列（composable 单例：抽屉 + 轮询）
const { queueOpen, activeCount, startPolling, stopPolling } = useGenerationQueue()

// 额度（顶部统一显示）
const { benefits, loadBenefits } = useBenefits()
const quotaTotal = computed(() => Number(benefits.value['ai_article_quota']?.value) || 0)
const quotaRemaining = computed(() => benefits.value['ai_article_quota']?.remaining ?? 0)

// 记住最近一次选择的 skill（watch + 持久化逻辑已下沉到 useSkills.js 模块层）

// 恢复草稿（加载最新一个或从作品页继续编辑）
onMounted(async () => {
  await loadSystemSkills()
  await loadExportTemplates()
  loadBenefits()
  // 加载我的/学习/收藏 skills（熟手模式也会用到）
  await Promise.all([
    loadMySkills().catch(() => {}),
    loadLearnedSkills().catch(() => {}),
    loadMarketSkills().catch(() => {}),
    loadFavoriteSkills().catch(() => {})
  ])

  // 所有 skills 加载完成后，恢复上次本地记住的 skill
  restoreLastSkill(marketSkills.value)

  const resume = localStorage.getItem('aichuangzuo_current_article')
  if (resume) {
    try {
      const data = JSON.parse(resume)
      if (data.fromDraft) {
        restoreDraft(data)
        localStorage.removeItem('aichuangzuo_current_article')
      }
    } catch (e) {
      console.warn('恢复草稿失败', e)
    }
  } else {
    const drafts = JSON.parse(localStorage.getItem('aichuangzuo_drafts') || '[]')
    if (drafts.length > 0) {
      restoreDraft(drafts[0])
    }
  }

  // 从提示词市场跳转过来时自动应用 skill
  const marketSkillId = route.query.marketSkillId
  if (marketSkillId) {
    const s = marketSkills.value.find(x => x.id === marketSkillId)
    if (s) {
      if (s.status !== 'approved') {
        message.warning('该提示词已下架或不可用，已为你取消自动选用')
      } else {
        applySkillShared({
          id: s.id,
          name: s.name,
          prompt: s.prompt,
          scope: s.scope,
          status: s.status
        })
      }
      router.replace({ path: route.path })
    }
  }

  // 从约稿任务跳转过来时自动填充表单
  const commissionTaskId = route.query.commissionTaskId
  if (commissionTaskId) {
    customTitle.value = String(route.query.title || '')
    const parts = []
    if (route.query.requirement) parts.push(String(route.query.requirement))
    const min = route.query.minWordCount != null ? Number(route.query.minWordCount) : null
    const max = route.query.maxWordCount != null ? Number(route.query.maxWordCount) : null
    if (min != null || max != null) {
      let rangeText = '字数要求：'
      if (min != null && max != null) rangeText += `${min}-${max} 字`
      else if (min != null) rangeText += `≥ ${min} 字`
      else if (max != null) rangeText += `≤ ${max} 字`
      parts.push(rangeText)
    }
    customRequirement.value = parts.join('\n').slice(0, 200)
    if (min != null || max != null) {
      const target = min != null ? min : max
      const wc = wordCountPresets.tier.find(x => x.count === target)
        || wordCountPresets.scenario.find(x => x.count === target)
        || Object.values(wordCountPresets.platform).flat().find(x => x.count === target)
        || { count: target, label: '自定义' }
      currentWordCount.value = wc
    }
    Modal.warning({
      title: '提示',
      content: '已根据任务要求填充标题和创作要求，建议你结合自身思路修改标题和观点，避免与其他投稿同质化，影响评审结果。',
      centered: true,
      okText: '知道了'
    })
    router.replace({ path: route.path })
  }

  startPolling()
})

onUnmounted(stopPolling)

const restoreDraft = (draft) => {
  customTitle.value = draft.customTitle || ''
  customRequirement.value = draft.customRequirement || ''
  // 当前仅保留熟手模式，不再恢复引导模式
  if (draft.platform) {
    const platformKey = typeof draft.platform === 'object' ? draft.platform.key : draft.platform
    const p = platforms.find(x => x.key === platformKey)
    if (p) currentPlatform.value = p
  }
  if (draft.wordCount) {
    const count = typeof draft.wordCount === 'object' ? draft.wordCount.count : draft.wordCount
    const wc = wordCountPresets.tier.find(x => x.count === count)
      || wordCountPresets.scenario.find(x => x.count === count)
      || Object.values(wordCountPresets.platform).flat().find(x => x.count === count)
      || { count, label: '自定义' }
    currentWordCount.value = wc
  }
  if (draft.style) {
    currentSkill.value = typeof draft.style === 'object' ? draft.style : { name: draft.style }
  }
  if (draft.template) {
    if (typeof draft.template === 'object') {
      const t = allTemplates.value.find(x => x.key === draft.template.key)
      if (t) selectedTemplateKey.value = t.key
    } else {
      const t = allTemplates.value.find(x => x.name === draft.template)
      if (t) selectedTemplateKey.value = t.key
    }
  }
  message.success('已恢复草稿')
}
</script>

<style scoped>
.create-index {
  min-height: 100%;
  padding: 24px 24px 40px;
  background:
    radial-gradient(600px 300px at 50% -80px, rgba(255, 36, 66, 0.05), transparent 70%);
  position: relative;
}

body[data-theme="dark"] .create-index {
  background:
    radial-gradient(600px 300px at 50% -80px, rgba(255, 36, 66, 0.08), transparent 70%);
}

/* 内容区右上角控件组：额度 + 队列 */
.topbar-right {
  position: absolute;
  top: 0;
  right: 0;
  display: inline-flex;
  align-items: center;
  gap: 14px;
  z-index: 10;
  max-width: calc(100vw - 32px);
  pointer-events: none;
}

.topbar-right > * {
  pointer-events: auto;
}

.topbar-right .quota-text {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.topbar-right .quota-text strong {
  color: var(--color-primary);
  margin: 0 2px;
}

.topbar-right .topbar-btn {
  border: none;
  background: var(--color-bg-card);
  color: var(--color-text-secondary);
  font-size: 13px;
  padding: 6px 14px;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.topbar-right .topbar-btn:hover {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

@media (max-width: 768px) {
  .create-index {
    padding: 0;
  }

  .topbar-right {
    top: 4px;
    right: 4px;
    gap: 8px;
  }
  .topbar-right .quota-text { font-size: 11px; }
  .topbar-right .topbar-btn { padding: 4px 10px; font-size: 12px; }
}
</style>
