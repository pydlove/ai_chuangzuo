<template>
  <div class="create-index">
    <div class="create-layout">
      <!-- 左侧创作区 -->
      <div class="create-card">
        <!-- 顶部：标题 + 额度 -->
        <div class="create-card-header">
          <div>
            <h2 class="create-title">开始创作</h2>
            <p class="create-subtitle">输入你的想法，AI 帮你生成文章</p>
          </div>
          <div class="quota-pill">
            本月剩余 <strong>{{ quotaRemaining }}</strong> / {{ quotaTotal }} 次
          </div>
        </div>

        <!-- 核心输入区 -->
        <div class="hero-input">
          <input
            v-model="customTitle"
            type="text"
            class="hero-title-input"
            placeholder="例如：职场新人快速提升效率的 5 个方法"
          />

          <textarea
            v-model="customRequirement"
            class="hero-textarea"
            placeholder="描述你想写的内容，或点击下方的灵感胶囊快速开始。例如：重点写时间管理技巧，加入具体案例，语气轻松、有干货。"
          ></textarea>
        </div>

        <!-- 智能默认配置 -->
        <div class="smart-defaults">
          <button class="settings-chip" @click="platformVisible = true">
            <span>{{ currentPlatform.name }}</span>
            <span class="chip-caret">▾</span>
          </button>
          <button class="settings-chip" @click="wordCountVisible = true">
            <span>{{ currentWordCount.count }} 字 · {{ currentWordCount.label }}</span>
            <span class="chip-caret">▾</span>
          </button>
          <button class="settings-chip" @click="styleVisible = true">
            <span>{{ currentStyle?.name }}</span>
            <span class="chip-caret">▾</span>
          </button>
          <button class="settings-chip" @click="templateVisible = true">
            <span>{{ currentTemplate?.name }}</span>
            <span class="chip-caret">▾</span>
          </button>
        </div>

        <!-- 主操作行：保存草稿 + 生成文章 -->
        <div class="hero-action-row">
          <div class="hero-action-left">
            <button class="action-link" @click="queueOpen = true">
              📋 队列<template v-if="activeCount > 0">（{{ activeCount }}）</template>
            </button>
            <button class="action-link" @click="handleSaveDraft">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right: 4px; vertical-align: -2px;">
                <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/>
                <polyline points="17 21 17 13 7 13 7 21"/>
                <polyline points="7 3 7 8 15 8"/>
              </svg>
              保存草稿
            </button>
            <button class="action-link" @click="router.push('/console/works?tab=drafts')">
              <FolderOutlined style="margin-right: 4px;" />
              草稿箱
            </button>
          </div>
          <button class="hero-generate-btn" @click="handleGenerate">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/>
            </svg>
            生成文章
          </button>
        </div>

        <!-- 选题灵感胶囊（标题库为空时整体隐藏） -->
        <TopicCapsules ref="topicCapsulesRef" />
      </div>
  </div>

    <QueueDrawer v-model:open="queueOpen" />
    <PlatformModal />
    <WordCountModal />
    <StyleModal />
    <TemplateModal />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import { FolderOutlined } from '@ant-design/icons-vue'
import { useRouter, useRoute } from 'vue-router'
import {
  currentStyle,
  applyStyle as applyStyleShared,
  loadSystemStyles
} from '@/composables/useStyles.js'
import { submitGeneration } from '@/api/generation.js'
import { marketStyles } from '@/composables/useStyleMarket.js'
import { useBenefits } from '@/composables/useBenefits.js'
import { saveDraft } from '@/api/draft.js'
import { useExportTemplates } from '@/composables/useExportTemplates.js'
import { platforms, wordCountPresets, useCreateForm } from './create/useCreateForm.js'
import { useGenerationQueue } from './create/useGenerationQueue.js'
import PlatformModal from './create/modals/PlatformModal.vue'
import WordCountModal from './create/modals/WordCountModal.vue'
import StyleModal from './create/modals/StyleModal.vue'
import TemplateModal from './create/modals/TemplateModal.vue'
import TopicCapsules from './create/TopicCapsules.vue'
import QueueDrawer from './create/QueueDrawer.vue'

const router = useRouter()
const route = useRoute()

// 导出模板（从 API 加载）
const { templates: apiTemplates, load: loadExportTemplates } = useExportTemplates()
const allTemplates = computed(() => apiTemplates.value)

// 创作表单共享状态（composable 单例）
const {
  customTitle, customRequirement,
  currentPlatform, currentWordCount, selectedTemplateKey,
  platformVisible, wordCountVisible, styleVisible, templateVisible
} = useCreateForm()

// 生成队列（composable 单例：抽屉 + 轮询）
const { activeCount, queueOpen, loadQueue, startPolling, stopPolling } = useGenerationQueue()

// 恢复草稿（加载最新一个或从作品页继续编辑）
onMounted(async () => {
  await loadSystemStyles()
  await loadExportTemplates()

  const resume = localStorage.getItem('aichuangzuo_current_article')
  if (resume) {
    try {
      const data = JSON.parse(resume)
      if (data.fromDraft) {
        customTitle.value = data.customTitle || ''
        customRequirement.value = data.customRequirement || ''
        if (data.platform) {
          const p = platforms.find(x => x.key === data.platform)
          if (p) currentPlatform.value = p
        }
        if (data.wordCount) {
          const wc = wordCountPresets.tier.find(x => x.count === data.wordCount)
            || wordCountPresets.scenario.find(x => x.count === data.wordCount)
            || Object.values(wordCountPresets.platform).flat().find(x => x.count === data.wordCount)
            || { count: data.wordCount, label: '自定义' }
          currentWordCount.value = wc
        }
        if (data.style) {
          currentStyle.value = { name: data.style }
        }
        if (data.template) {
          const t = allTemplates.value.find(x => x.name === data.template)
          if (t) selectedTemplateKey.value = t.key
        }
        localStorage.removeItem('aichuangzuo_current_article')
      }
    } catch (e) {
      console.warn('恢复草稿失败', e)
    }
  } else {
    const drafts = JSON.parse(localStorage.getItem('aichuangzuo_drafts') || '[]')
    if (drafts.length > 0) {
      const data = drafts[0]
      restoreDraft(data)
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

  // 灵感胶囊：从标题库随机拉取（不阻塞草稿恢复）
  topicCapsulesRef.value?.loadTopics()
})

onUnmounted(stopPolling)

// 额度（来自会员权益 ai_article_quota，ConsoleLayout 登录时已加载）
const { benefits, loadBenefits } = useBenefits()
const quotaTotal = computed(() => Number(benefits.value['ai_article_quota']?.value) || 0)
const quotaRemaining = computed(() => benefits.value['ai_article_quota']?.remaining ?? 0)

const topicCapsulesRef = ref(null)

const currentTemplate = computed(() => allTemplates.value.find(t => t.key === selectedTemplateKey.value) || allTemplates.value[0])

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

// 操作
const handleSaveDraft = async () => {
  try {
    const data = {
      customTitle: customTitle.value,
      customRequirement: customRequirement.value,
      platform: currentPlatform.value?.name,
      wordCount: currentWordCount.value?.count,
      style: currentStyle.value?.name,
      template: currentTemplate.value?.name
    }
    await saveDraft(data)
    message.success('草稿已保存')
  } catch (e) {
    console.warn('保存草稿失败', e)
  }
}

const handleGenerate = async () => {
  if (!customTitle.value.trim()) {
    message.warning('请输入文章标题')
    return
  }
  if (!customRequirement.value.trim()) {
    message.warning('请补充你的核心观点和要求')
    return
  }

  const platformValue = typeof currentPlatform.value === 'object'
    ? (currentPlatform.value?.key || '')
    : (currentPlatform.value || '')
  const styleValue = typeof currentStyle.value === 'object'
    ? (currentStyle.value?.id || currentStyle.value?.name || '')
    : (currentStyle.value || '')
  const wordCountValue = typeof currentWordCount.value === 'object'
    ? (currentWordCount.value?.count || 800)
    : (Number(currentWordCount.value) || 800)

  // 额度前置校验：免费用户 / 额度用完都引导开会员
  if (quotaTotal.value <= 0) {
    message.warning('开通会员后才能使用 AI 生成文章')
    router.push('/pricing')
    return
  }
  if (quotaRemaining.value <= 0) {
    message.warning('本月额度已用完，升级会员可获得更多额度')
    router.push('/pricing')
    return
  }

  try {
    await submitGeneration({
      title: customTitle.value,
      description: customRequirement.value,
      platform: platformValue,
      styleRef: styleValue,
      wordCount: wordCountValue,
      template: currentTemplate.value?.key || 'wechat'
    })
    message.success('已加入生成队列')
    clearForm()
    loadQueue()
    queueOpen.value = true
    loadBenefits() // 刷新本月剩余额度
  } catch (e) {
    message.error(e?.message || '提交失败，请稍后重试')
  }
}

const clearForm = () => {
  customTitle.value = ''
  customRequirement.value = ''
}
</script>

<style scoped>
.create-index {
  display: flex;
  flex-direction: column;
}

.create-layout {
  display: flex;
  gap: 24px;
}

.create-card {
  background: var(--color-bg-card);
  border-radius: 16px;
  padding: 20px;
  flex: 1;
  min-width: 0;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.create-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 8px;
}

.create-subtitle {
  color: var(--color-text-secondary);
  font-size: 14px;
  margin-bottom: 20px;
}

.settings-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 16px;
  color: #262626;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}

.settings-chip:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: #fff0f2;
}

.chip-caret {
  font-size: 10px;
  color: #8c8c8c;
}

.action-link {
  display: inline-flex;
  align-items: center;
  background: none;
  border: none;
  color: #595959;
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

/* ===== 单屏启动器新样式 ===== */

.create-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  flex-shrink: 0;
}

.create-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 4px;
}

.create-subtitle {
  font-size: 14px;
  margin-bottom: 0;
}

.quota-pill {
  font-size: 12px;
  color: #595959;
  background: #f5f5f5;
  padding: 4px 10px;
  border-radius: 12px;
  white-space: nowrap;
}

.quota-pill strong {
  color: var(--color-primary);
}

.hero-input {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
  flex-shrink: 0;
}

.hero-title-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #c4c4c4;
  border-radius: 10px;
  font-size: 15px;
  color: #1a1a1a;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
}

.hero-title-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}

.hero-title-input::placeholder {
  color: #999;
}

.hero-textarea {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid #c4c4c4;
  border-radius: 12px;
  font-size: 15px;
  min-height: 350px;
  resize: vertical;
  color: #1a1a1a;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
  font-family: inherit;
  line-height: 1.6;
}

.hero-textarea:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}

.hero-textarea::placeholder {
  color: #999;
}

.hero-generate-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: auto;
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

.hero-action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  flex-shrink: 0;
}

.hero-action-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.smart-defaults {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .create-layout {
    flex-direction: column;
  }

  .create-card {
    padding: 16px;
  }

  .create-card-header {
    flex-direction: column;
    gap: 8px;
  }

  .create-title {
    font-size: 20px;
  }

  .hero-textarea {
    min-height: 100px;
  }

  .smart-defaults {
    flex-wrap: wrap;
    overflow: visible;
    padding-bottom: 0;
  }

  /* 让单个 chip 可以收缩到一行一半，避免文字溢出 */
  .settings-chip {
    flex: 0 1 auto;
    max-width: 100%;
    min-width: 0;
  }

  .hero-action-row {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .hero-action-left {
    justify-content: flex-start;
  }

  .hero-generate-btn {
    width: 100%;
  }
}

/* 深色模式 */
body[data-theme="dark"] .hero-title-input,
body[data-theme="dark"] .hero-textarea {
  background: #2a2a2a;
  border-color: #434343;
  color: #f0f0f0;
}

body[data-theme="dark"] .hero-title-input::placeholder,
body[data-theme="dark"] .hero-textarea::placeholder {
  color: #737373;
}

body[data-theme="dark"] .hero-title-input:focus,
body[data-theme="dark"] .hero-textarea:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.15);
}

body[data-theme="dark"] .settings-chip {
  background: #2a2a2a;
  border-color: #434343;
  color: #d9d9d9;
}

body[data-theme="dark"] .quota-pill {
  background: #2a2a2a;
  color: #a6a6a6;
}
</style>
