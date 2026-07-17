<template>
  <div class="create-index">
    <div class="topbar-right">
      <span class="quota-text">本月剩余 <strong>{{ quotaRemaining }}</strong> / {{ quotaTotal }} 次</span>
      <button class="topbar-btn" @click="queueOpen = true">
        队列<template v-if="activeCount > 0">（{{ activeCount }}）</template>
      </button>
      <div class="mode-switch">
        <button :class="['mode-tab', { active: createMode === 'guided' }]" @click="setCreateMode('guided')">引导模式</button>
        <button :class="['mode-tab', { active: createMode === 'minimal' }]" @click="setCreateMode('minimal')">熟手模式</button>
      </div>
    </div>

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
  loadSystemStyles,
  loadMyStyles,
  loadLearnedStyles
} from '@/composables/useStyles.js'
import { marketStyles, favoriteStyles, loadMarketStyles } from '@/composables/useStyleMarket.js'
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
  createMode, setCreateMode, customTitle, customRequirement,
  currentPlatform, currentWordCount, selectedTemplateKey
} = useCreateForm()

// 生成队列（composable 单例：抽屉 + 轮询）
const { queueOpen, activeCount, startPolling, stopPolling } = useGenerationQueue()

// 额度（顶部统一显示：两种模式都能看到）
const { benefits, loadBenefits } = useBenefits()
const quotaTotal = computed(() => Number(benefits.value['ai_article_quota']?.value) || 0)
const quotaRemaining = computed(() => benefits.value['ai_article_quota']?.remaining ?? 0)

// 恢复草稿（加载最新一个或从作品页继续编辑）
onMounted(async () => {
  await loadSystemStyles()
  await loadExportTemplates()
  loadBenefits()
  // 加载我的/学习/收藏风格（引导模式"想要什么风格？"步骤可选到）
  Promise.all([
    loadMyStyles().catch(() => {}),
    loadLearnedStyles().catch(() => {}),
    loadMarketStyles().catch(() => {})
  ])

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
  // 草稿从哪儿保存的，恢复到对应模式；默认走引导模式
  createMode.value = draft.createMode === 'minimal' ? 'minimal' : 'guided'
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
  position: relative;
}

body[data-theme="dark"] .create-index {
  background:
    radial-gradient(600px 300px at 50% -80px, rgba(255, 36, 66, 0.08), transparent 70%);
}

/* 内容区右上角控件组：额度 + 队列 + 模式切换（两模式统一可见） */
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

/* 模式切换 tab（互斥） */
.mode-switch {
  display: inline-flex;
  gap: 4px;
  background: var(--color-bg-card);
  border-radius: 22px;
  padding: 4px;
  pointer-events: auto;
}

.mode-tab {
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 13px;
  padding: 6px 14px;
  border-radius: 18px;
  cursor: pointer;
  transition: all 0.2s;
}

.mode-tab:hover {
  color: var(--color-primary);
}

.mode-tab.active {
  background: var(--color-primary);
  color: #fff;
}

@media (max-width: 768px) {
  .topbar-right {
    top: 4px;
    right: 4px;
    gap: 8px;
  }
  .topbar-right .quota-text { font-size: 11px; }
  .topbar-right .topbar-btn { padding: 4px 10px; font-size: 12px; }
  .mode-tab { padding: 4px 10px; font-size: 12px; }
}
</style>
