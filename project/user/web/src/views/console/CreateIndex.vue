<template>
  <div class="create-index">
    <GuidedChat v-if="createMode === 'guided'" />
    <MinimalPanel v-else />

    <QueueDrawer v-model:open="queueOpen" />
    <PlatformModal />
    <WordCountModal />
    <StyleModal />
    <TemplateModal />
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter, useRoute } from 'vue-router'
import {
  currentStyle,
  applyStyle as applyStyleShared,
  loadSystemStyles
} from '@/composables/useStyles.js'
import { marketStyles } from '@/composables/useStyleMarket.js'
import { useBenefits } from '@/composables/useBenefits.js'
import { useExportTemplates } from '@/composables/useExportTemplates.js'
import { platforms, wordCountPresets, useCreateForm } from './create/useCreateForm.js'
import { useGenerationQueue } from './create/useGenerationQueue.js'
import MinimalPanel from './create/MinimalPanel.vue'
import GuidedChat from './create/GuidedChat.vue'
import QueueDrawer from './create/QueueDrawer.vue'
import PlatformModal from './create/modals/PlatformModal.vue'
import WordCountModal from './create/modals/WordCountModal.vue'
import StyleModal from './create/modals/StyleModal.vue'
import TemplateModal from './create/modals/TemplateModal.vue'

const router = useRouter()
const route = useRoute()

// 导出模板（从 API 加载）
const { templates: apiTemplates, load: loadExportTemplates } = useExportTemplates()
const allTemplates = computed(() => apiTemplates.value)

// 创作表单共享状态（composable 单例）
const {
  createMode, customTitle, customRequirement,
  currentPlatform, currentWordCount, selectedTemplateKey
} = useCreateForm()

// 生成队列（composable 单例：抽屉 + 轮询）
const { queueOpen, startPolling, stopPolling } = useGenerationQueue()

// 额度（ConsoleLayout 登录时已加载，这里仅确保最新）
const { loadBenefits } = useBenefits()

// 恢复草稿（加载最新一个或从作品页继续编辑）
onMounted(async () => {
  await loadSystemStyles()
  await loadExportTemplates()
  loadBenefits()

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

  // 从风格市场跳转过来时自动应用风格
  const marketStyleId = route.query.marketStyleId
  if (marketStyleId) {
    const s = marketStyles.value.find(x => x.id === marketStyleId)
    if (s) {
      applyStyleShared({
        name: s.name,
        prompt: s.prompt,
        scope: s.scope
      })
      router.replace({ path: route.path })
    }
  }

  startPolling()
})

onUnmounted(stopPolling)

const restoreDraft = (draft) => {
  customTitle.value = draft.customTitle || ''
  customRequirement.value = draft.customRequirement || ''
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
    currentStyle.value = typeof draft.style === 'object' ? draft.style : { name: draft.style }
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
}

body[data-theme="dark"] .create-index {
  background:
    radial-gradient(600px 300px at 50% -80px, rgba(255, 36, 66, 0.08), transparent 70%);
}
</style>
