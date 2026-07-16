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
          <button class="settings-chip" @click="openPlatformModal">
            <span>{{ currentPlatform.name }}</span>
            <span class="chip-caret">▾</span>
          </button>
          <button class="settings-chip" @click="openWordCountModal">
            <span>{{ currentWordCount.count }} 字 · {{ currentWordCount.label }}</span>
            <span class="chip-caret">▾</span>
          </button>
          <button class="settings-chip" @click="openStyleModal">
            <span>{{ currentStyle?.name }}</span>
            <span class="chip-caret">▾</span>
          </button>
          <button class="settings-chip" @click="openTemplateModal">
            <span>{{ currentTemplate.name }}</span>
            <span class="chip-caret">▾</span>
          </button>
        </div>

        <!-- 主操作行：保存草稿 + 生成文章 -->
        <div class="hero-action-row">
          <div class="hero-action-left">
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
        <div v-if="topics.length > 0" class="topic-capsules">
          <span class="topic-capsules-label">没灵感？点一个快速开始：</span>
          <div class="topic-capsules-grid">
            <a-tooltip
              v-for="topic in topics"
              :key="topic.id"
              :title="topic.title"
              placement="top"
            >
              <button
                :class="['topic-capsule', { used: topic.used }]"
                :disabled="topic.used"
                :title="topic.title"
                @click="topic.used ? null : applyTopic(topic)"
              >
                {{ topic.title }}
              </button>
            </a-tooltip>
          </div>
          <button class="refresh-capsule" @click="refreshTopics">换一批</button>
        </div>
      </div>

    <!-- 右侧生成队列 -->
    <div class="queue-panel">
      <div class="queue-panel-header">
        <h3 class="queue-panel-title">生成队列</h3>
        <button class="queue-more-btn" @click="router.push('/console/works')">查看更多 →</button>
      </div>
      <div v-if="miniQueueList.length === 0" class="queue-panel-empty">
        <InboxOutlined class="empty-icon" />
        <div class="empty-text">暂无生成任务</div>
        <div class="empty-hint">点击「生成文章」开始创作</div>
      </div>
      <div v-else class="queue-panel-list">
        <div
          v-for="item in miniQueueList.slice(0, 5)"
          :key="item.id"
          :class="['queue-panel-item', item.status]"
          :style="item.status === 'completed' ? 'cursor: pointer' : ''"
          @click="item.status === 'completed' && router.push('/console/works')"
        >
          <div class="queue-item-top">
            <div class="queue-item-icon">
              <LoadingOutlined v-if="item.status === 'generating'" :spin="true" />
              <CheckCircleOutlined v-else-if="item.status === 'completed'" />
              <ClockCircleOutlined v-else-if="item.status === 'queued'" />
              <CloseCircleOutlined v-else />
            </div>
            <div class="queue-item-info">
              <a-tooltip :title="item.title" placement="top">
                <span class="queue-item-title">{{ item.title }}</span>
              </a-tooltip>
              <div class="queue-item-meta">
                <span class="queue-item-status-badge" :class="item.status">
                  {{ item.status === 'generating' ? `生成中 ${Math.min(100, Math.round(item.progress))}%` : miniStatusText(item.status) }}
                </span>
              </div>
            </div>
          </div>

          <!-- 生成中进度条 -->
          <div v-if="item.status === 'generating'" class="queue-item-progress">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: Math.min(100, Math.round(item.progress)) + '%' }"></div>
            </div>
            <div class="progress-hint">已完成 {{ Math.min(100, Math.round(item.progress)) }}%，共 12 个阶段</div>
          </div>
        </div>
        <div v-if="miniQueueList.length > 5" class="queue-panel-more">
          还有 {{ miniQueueList.length - 5 }} 个任务，<button class="queue-panel-more-link" @click="router.push('/console/works')">去我的作品查看 →</button>
        </div>
      </div>
    </div>
  </div>

    <!-- 发布平台选择弹框 -->
    <a-modal
      v-model:open="platformVisible"
      :footer="null"
      :width="560"
      centered
      :closable="true"
      class="platform-modal"
    >
      <template #title>
        <div class="modal-title-wrap">
          <div class="modal-title">选择发布平台</div>
          <div class="modal-subtitle">选择目标平台，AI 将按平台规则推荐模板、字数和标签</div>
        </div>
      </template>
      <div class="platform-grid">
        <div
          v-for="p in platforms"
          :key="p.key"
          :class="['platform-item', { selected: currentPlatform.key === p.key }]"
          @click="selectPlatform(p)"
        >
          <div class="platform-name">{{ p.name }}</div>
          <div class="platform-desc">{{ p.desc }}</div>
        </div>
      </div>
    </a-modal>

    <!-- 字数选择弹框 -->
    <a-modal
      v-model:open="wordCountVisible"
      :footer="null"
      :width="640"
      centered
      class="word-count-modal"
    >
      <template #title>
        <div class="modal-title-wrap">
          <div class="modal-title">设置文章字数</div>
          <div class="modal-subtitle">选择合适的字数，让 AI 写出更精准的内容</div>
        </div>
      </template>

      <div class="wc-tabs">
        <button
          v-for="tab in wordCountTabs"
          :key="tab.key"
          :class="['wc-tab', { active: wordCountTab === tab.key }]"
          @click="wordCountTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>

      <div class="wc-content">
        <!-- 按平台 -->
        <div v-if="wordCountTab === 'platform'" class="wc-grid">
          <div
            v-for="wc in platformWordCounts"
            :key="wc.count"
            :class="['wc-item', { selected: currentWordCount.count === wc.count }]"
            @click="selectWordCount(wc)"
          >
            <div class="wc-count">{{ wc.count }} 字</div>
            <div class="wc-label">{{ wc.label }}</div>
          </div>
        </div>

        <!-- 按场景 -->
        <div v-else-if="wordCountTab === 'scenario'" class="wc-list">
          <div
            v-for="s in wordCountPresets.scenario"
            :key="s.count"
            :class="['wc-item-wide', { selected: currentWordCount.count === s.count }]"
            @click="selectWordCount(s)"
          >
            <div class="wc-item-left">
              <div class="wc-count">{{ s.count }} 字</div>
              <div class="wc-label">{{ s.label }}</div>
            </div>
            <div class="wc-desc">{{ s.desc }}</div>
          </div>
        </div>

        <!-- 按档位 -->
        <div v-else-if="wordCountTab === 'tier'" class="wc-list">
          <div
            v-for="t in wordCountPresets.tier"
            :key="t.count"
            :class="['wc-item-wide', { selected: currentWordCount.count === t.count }]"
            @click="selectWordCount(t)"
          >
            <div class="wc-item-left">
              <div class="wc-count">{{ t.count }} 字</div>
              <div class="wc-label">{{ t.label }}</div>
            </div>
            <div class="wc-desc">{{ t.desc }}</div>
          </div>
        </div>

        <!-- 自定义 -->
        <div v-else class="wc-custom">
          <div class="wc-custom-display">{{ customWordCount }} 字</div>
          <input
            v-model="customWordCount"
            type="number"
            class="wc-custom-input"
            min="1"
            max="3000"
            placeholder="输入 1-3000 字"
          />
          <input
            v-model="customWordCount"
            type="range"
            class="wc-slider"
            min="1"
            max="3000"
          />
          <div class="wc-custom-hint">AI 将生成约 {{ customWordCount }} 字的文章</div>
        </div>
      </div>
    </a-modal>

    <!-- 风格选择弹框 -->
    <a-modal
      v-model:open="styleVisible"
      :footer="null"
      :width="720"
      centered
      class="style-modal"
    >
      <template #title>
        <div class="modal-title-wrap">
          <div class="modal-title">风格库</div>
          <div class="modal-subtitle">选择一套预设风格，让 AI 写出你想要的调性</div>
        </div>
      </template>

      <!-- 创建/编辑风格 -->
      <div v-if="createStyleMode" class="style-editor">
        <div class="style-editor-header">
          <button class="style-editor-back" @click="goBackToList">← 返回</button>
          <div class="style-editor-title">{{ editingStyle.name ? '编辑提示词' : '新建我的风格' }}</div>
        </div>
        <div class="style-editor-form">
          <div class="style-editor-field">
            <label class="style-editor-label">风格名称 <span class="required">*</span></label>
            <input
              v-model="editingStyle.name"
              type="text"
              class="style-editor-input"
              placeholder="例如：我的小红书风"
              maxlength="20"
            />
          </div>
          <div class="style-editor-field">
            <label class="style-editor-label">风格提示词 <span class="required">*</span></label>
            <textarea
              v-model="editingStyle.prompt"
              class="style-editor-textarea"
              placeholder="描述你希望 AI 采用的语气、结构、用词习惯等..."
              rows="5"
            ></textarea>
            <div class="style-editor-hint">提示词会作为系统提示的一部分影响生成结果。</div>
          </div>
          <div class="style-editor-field">
            <label class="style-editor-label">适用范围 <span class="required">*</span></label>
            <input
              v-model="editingStyle.scope"
              type="text"
              class="style-editor-input"
              placeholder="例：公众号情感文 / 产品评测 / 小红书种草"
              maxlength="50"
            />
          </div>
          <div class="style-editor-presets">
            <div class="style-editor-preset-label">快速填充模板：</div>
            <div class="style-editor-preset-list">
              <div
                v-for="preset in stylePresets"
                :key="preset.name"
                class="style-preset-card"
                @click="editingStyle.prompt = preset.prompt"
              >
                <div class="style-preset-title">{{ preset.name }}</div>
                <div class="style-preset-desc">{{ preset.desc }}</div>
              </div>
            </div>
          </div>
          <button class="save-style-btn" @click="saveStyle">保存</button>
        </div>
      </div>

      <!-- 风格列表 -->
      <template v-else>
        <div class="style-tabs">
          <button
            :class="['style-tab', { active: styleTab === 'my' }]"
            @click="styleTab = 'my'; createStyleMode = false"
          >
            我的风格
          </button>
          <button
            :class="['style-tab', { active: styleTab === 'learned' }]"
            @click="styleTab = 'learned'; createStyleMode = false; loadLearnedStyles()"
          >
            学习的风格
          </button>
          <button
            :class="['style-tab', { active: styleTab === 'system' }]"
            @click="styleTab = 'system'; createStyleMode = false"
          >
            系统预设风格
          </button>
        </div>

        <div class="style-content">
          <!-- 系统预设 -->
          <div v-show="styleTab === 'system'" class="style-grid">
            <div
              v-for="s in systemStyles"
              :key="s.name"
              :class="['style-card', { selected: selectedStyleName === s.name }]"
              @click="selectStyle(s)"
            >
              <div class="style-card-title">{{ s.name }}</div>
              <div class="style-card-desc">{{ s.desc }}</div>
              <div class="style-card-prompt">{{ s.promptSummary }}</div>
            </div>
          </div>

          <!-- 我的风格 -->
          <div v-show="styleTab === 'my'" class="style-grid">
            <div class="style-add-card" @click="goToCreateStyle">
              <div class="style-add-icon">+</div>
              <div class="style-add-text">新建我的风格</div>
            </div>
            <div
              v-for="(m, idx) in myStyles"
              :key="m.name"
              :class="['style-card', { selected: selectedStyleName === m.name }]"
              @click="selectStyle(m)"
            >
              <div class="style-card-title">{{ m.name }}</div>
              <div class="style-card-desc">{{ m.desc }} · 已用 {{ m.count }} 次</div>
              <div v-if="m.scope" class="style-card-scope">适用：{{ m.scope }}</div>
              <div class="style-prompt-toggle" @click.stop="togglePrompt(idx)">
                {{ expandedPromptIdx === idx ? '收起 ▴' : '查看完整提示词 ▾' }}
              </div>
              <div v-show="expandedPromptIdx === idx" class="style-prompt-full">
                {{ m.prompt }}
              </div>
              <div v-show="expandedPromptIdx === idx" class="style-prompt-actions">
                <button class="style-action-btn" @click.stop="goToEditStyle(m)">编辑提示词</button>
                <button class="style-action-btn style-del-btn" @click.stop="deleteStyle(m.name)">删除</button>
              </div>
            </div>
          </div>

          <!-- 学习的风格 -->
          <div v-show="styleTab === 'learned'" class="style-grid">
            <div
              v-if="learnedStyles.length === 0"
              class="style-empty style-empty-text"
            >
              还没有学习过的风格，请前往「我的风格」页面学习。
            </div>
            <div
              v-for="(l, idx) in learnedStyles"
              v-else
              :key="l.name"
              :class="['style-card', { selected: selectedStyleName === l.name }]"
              @click="selectStyle(l)"
            >
              <div class="style-card-title">{{ l.name }}</div>
              <div v-if="l.scope" class="style-card-scope">适用：{{ l.scope }}</div>
              <div class="style-prompt-toggle" @click.stop="toggleLearnedPrompt(idx)">
                {{ expandedLearnedIdx === idx ? '收起 ▴' : '查看完整提示词 ▾' }}
              </div>
              <div v-show="expandedLearnedIdx === idx" class="style-prompt-full">
                {{ l.prompt }}
              </div>
            </div>
          </div>
        </div>

        <div class="style-footer">
          <button
            class="style-apply-btn"
            :disabled="!selectedStyleName"
            @click="applyStyle"
          >
            应用
          </button>
        </div>
      </template>
    </a-modal>

    <!-- 草稿箱弹框 -->
    <a-modal
      v-model:open="draftBoxVisible"
      title="草稿箱"
      :footer="null"
      :width="480"
      centered
      class="draft-box-modal"
    >
      <div :key="draftBoxKey">
        <div v-if="draftList.length === 0" class="draft-box-empty">
          <a-empty description="草稿箱是空的" />
        </div>
        <div v-else class="draft-box-list">
          <div
            v-for="draft in draftList"
            :key="draft.id"
            class="draft-item"
            @click="restoreDraft(draft)"
          >
            <div class="draft-info">
              <div class="draft-title">{{ draft.customTitle || '未命名草稿' }}</div>
              <div class="draft-desc">{{ draft.customRequirement || '未填写要求' }}</div>
              <div class="draft-meta">
                <span>{{ draft.platform?.name || '未选择平台' }}</span>
                <span>·</span>
                <span>{{ draft.wordCount?.count || 0 }}字</span>
                <span>·</span>
                <span>{{ formatDate(draft.savedAt) }}</span>
              </div>
            </div>
            <button class="draft-del-btn" @click="deleteDraft(draft.id, $event)">删除</button>
          </div>
        </div>
      </div>
    </a-modal>

    <!-- 模板选择弹框 -->
    <a-modal
      v-model:open="templateVisible"
      :footer="null"
      :width="960"
      centered
      :closable="true"
      class="template-modal template-lib-modal"
      wrap-class-name="template-modal-wrap"
    >
      <template #title>
        <div class="modal-title-wrap">
          <div class="modal-title">导出模板库</div>
          <div class="modal-subtitle">共 {{ filteredTemplates.length }} 个模板 · 左侧实时预览 · 右侧选择模板</div>
        </div>
      </template>

      <!-- 平台标签 -->
      <div class="template-tabs">
        <button
          v-for="tab in templatePlatformTabs"
          :key="tab.key"
          :class="['template-tab', { active: templatePlatformTab === tab.key }]"
          @click="templatePlatformTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>

      <!-- 2列布局 -->
      <div class="template-body">
        <!-- 左侧：大预览 -->
        <div class="template-preview-pane" v-html="currentTemplatePreview"></div>

        <!-- 右侧：模板列表 -->
        <div class="template-list-pane">
          <div
            v-for="t in filteredTemplates"
            :key="t.key"
            :class="['template-row', { selected: selectedTemplateKey === t.key }]"
            @click="selectTemplate(t)"
          >
            <div class="template-row-info">
              <div class="template-row-name">{{ t.name }}</div>
              <div class="template-row-desc">{{ t.desc }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="template-footer">
        <button class="template-apply-btn" @click="applyTemplate">应用</button>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import { FolderOutlined, LoadingOutlined, CheckCircleOutlined, ClockCircleOutlined, InboxOutlined, CloseCircleOutlined } from '@ant-design/icons-vue'
import { useRouter, useRoute } from 'vue-router'
import {
  systemStyles,
  myStyles,
  currentStyle,
  applyStyle as applyStyleShared,
  addCustomStyle,
  updateCustomStyle,
  removeCustomStyle,
  learnedStyles,
  loadMyStyles,
  loadLearnedStyles,
  loadSystemStyles
} from '@/composables/useStyles.js'
import { listGenerationTasks, submitGeneration } from '@/api/generation.js'
import { fetchRandomTopics, markTopicUsed } from '@/api/topic.js'
import { marketStyles } from '@/composables/useStyleMarket.js'
import { useIsMobile } from '@/composables/useMobile.js'
import { useBenefits } from '@/composables/useBenefits.js'
import { saveCurrentArticle } from '@/utils/articleStorage.js'
import { saveDraft } from '@/api/draft.js'
import { getQueueLimit, getCurrentPlanName } from '@/utils/membershipLimits.js'
import { allTemplates, buildLargePreview } from '@/utils/articleTemplates.js'

const router = useRouter()
const route = useRoute()
const isMobile = useIsMobile()

// 恢复草稿（加载最新一个或从作品页继续编辑）
onMounted(async () => {
  await loadSystemStyles()

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
          const t = allTemplates.find(x => x.name === data.template)
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
      selectedStyleName.value = s.name
      router.replace({ path: route.path })
    }
  }

  loadMiniQueue()
  // 定时刷新队列
  setInterval(loadMiniQueue, 5000)

  // 灵感胶囊：从标题库随机拉取（不阻塞草稿恢复）
  loadTopics()
})

// 加载简化队列数据（从后端按当前用户查询）
const miniQueueList = ref([])

const mapStatus = (code) => {
  return code === 0 ? 'queued' : code === 1 ? 'generating' : code === 2 ? 'completed' : code === 3 ? 'failed' : 'queued'
}

const loadMiniQueue = async () => {
  try {
    const data = await listGenerationTasks({ page: 1, pageSize: 5 })
    miniQueueList.value = (data.list || []).map(t => ({
      id: t.id,
      title: t.title || t.inputParam?.title || '未命名',
      platform: t.inputParam?.platform || '未选择',
      wordCount: t.wordLimitTarget || 0,
      status: mapStatus(t.status),
      progress: t.progressPct || 0,
      createdAt: t.createdAt,
      completedAt: t.completedAt
    }))
  } catch (e) {
    miniQueueList.value = []
  }
}

const miniStatusText = (status) => {
  const map = { generating: '生成中', queued: '排队中', completed: '已完成', failed: '失败' }
  return map[status] || status
}

// 额度（来自会员权益 ai_article_quota，ConsoleLayout 登录时已加载）
const { benefits, loadBenefits } = useBenefits()
const quotaTotal = computed(() => Number(benefits.value['ai_article_quota']?.value) || 0)
const quotaRemaining = computed(() => benefits.value['ai_article_quota']?.remaining ?? 0)
const quotaUsed = computed(() => quotaTotal.value - quotaRemaining.value)
const usedPercent = computed(() => Math.round((quotaUsed.value / quotaTotal.value) * 100))
const quotaColor = computed(() => {
  const used = quotaUsed.value / quotaTotal.value
  if (used < 0.6) return '#07c160'  // 绿色
  if (used < 0.9) return '#faad14'  // 黄色
  return '#ff4d4f'  // 红色
})

// 灵感选题（来自标题库：管理端 AI 生成入库，按用户隔离已用）
const topics = ref([])

const loadTopics = async () => {
  try {
    const list = await fetchRandomTopics(6)
    topics.value = (list || []).map(t => ({ id: t.id, title: t.title, summary: t.summary, used: false }))
  } catch (e) {
    // 拉取失败静默降级：胶囊区保持空并整体隐藏
    topics.value = []
  }
}

const applyTopic = (topic) => {
  // 标题填入标题框、概要填入要求框，同时上报使用（该标题之后不再出现）
  customTitle.value = topic.title
  customRequirement.value = topic.summary
  topic.used = true
  markTopicUsed(topic.id).catch(() => {})
}

const refreshTopics = () => {
  loadTopics()
}

// 创作内容
const customTitle = ref('')
const customRequirement = ref('')

// 平台
const platforms = [
  { key: 'wechat', name: '公众号', desc: '深度长文，适合专业内容输出' },
  { key: 'xiaohongshu', name: '小红书', desc: '轻松图文，种草安利效果好' },
  { key: 'toutiao', name: '今日头条', desc: '算法分发，热点资讯类内容' },
  { key: 'baijiahao', name: '百家号', desc: '多平台分发，SEO友好' },
  { key: 'douyin', name: '抖音图文', desc: '短视频+图文，流量大' },
  { key: 'zhihu', name: '知乎', desc: '深度问答，专业知识分享' }
]
const currentPlatform = ref(platforms[0])
const platformVisible = ref(false)

const openPlatformModal = () => {
  platformVisible.value = true
}

const selectPlatform = (p) => {
  currentPlatform.value = p
  platformVisible.value = false
}

// 字数
const wordCountTab = ref('tier')
const wordCountPresets = {
  platform: {
    wechat: [
      { count: 800, label: '早报 / 简评' },
      { count: 1500, label: '标准深度文' },
      { count: 2500, label: '专题报道' },
      { count: 3000, label: '行业研究（上限）' }
    ],
    xiaohongshu: [
      { count: 300, label: '标题种草' },
      { count: 500, label: '图文分享' },
      { count: 800, label: '详细测评' },
      { count: 1200, label: '步骤拆解教程' }
    ],
    toutiao: [
      { count: 400, label: '热点快讯' },
      { count: 800, label: '事件报道' },
      { count: 1500, label: '专题分析' },
      { count: 2000, label: '观点长文' }
    ],
    baijiahao: [
      { count: 1000, label: '知识科普' },
      { count: 1500, label: '生活攻略' },
      { count: 2000, label: '人文叙事' },
      { count: 2500, label: '行业洞察' }
    ],
    douyin: [
      { count: 150, label: '封面金句' },
      { count: 300, label: '图配文' },
      { count: 600, label: '情感短篇' }
    ],
    general: [
      { count: 500, label: '短文' },
      { count: 1000, label: '中等' },
      { count: 1500, label: '标准' },
      { count: 2500, label: '长文' }
    ]
  },
  scenario: [
    { count: 1200, label: '教程 / 步骤', desc: '操作步骤详细说明，适合图文对照' },
    { count: 1000, label: '测评 / 对比', desc: '优缺点详细对比，附评分' },
    { count: 500, label: '清单 / 种草', desc: '快速清单 + 标签，重点突出' },
    { count: 1800, label: '故事 / 叙事', desc: '沉浸式叙事，节奏完整' }
  ],
  tier: [
    { count: 500, label: '短文', desc: '速读，3 分钟读完' },
    { count: 1000, label: '中等', desc: '快速浏览，5 分钟读完' },
    { count: 1500, label: '标准', desc: '深度阅读，8 分钟读完' },
    { count: 2500, label: '长文', desc: '完整报告，12 分钟读完' }
  ]
}
const customWordCount = ref(1500)
const currentWordCount = ref({ count: 1500, label: '标准', desc: '深度阅读，8 分钟读完' })
const wordCountVisible = ref(false)

const wordCountTabs = [
  { key: 'platform', label: '按平台推荐' },
  { key: 'scenario', label: '按内容场景' },
  { key: 'tier', label: '按字数档位' },
  { key: 'custom', label: '自定义字数' }
]

const platformWordCounts = computed(() => {
  const platform = currentPlatform.value?.key || 'wechat'
  return wordCountPresets.platform[platform] || wordCountPresets.platform.general
})

const openWordCountModal = () => {
  wordCountVisible.value = true
}

const selectWordCount = (wc) => {
  currentWordCount.value = wc
  wordCountVisible.value = false
}

// 风格
const styleTab = ref('my')
const styleVisible = ref(false)
const selectedStyleName = ref(null)
const expandedPromptIdx = ref(null)
const expandedLearnedIdx = ref(null)
const createStyleMode = ref(false)
const editingStyle = reactive({ originalName: '', name: '', prompt: '', scope: '', isEdit: false })

const openStyleModal = async () => {
  styleTab.value = 'my'
  selectedStyleName.value = null
  expandedPromptIdx.value = null
  createStyleMode.value = false
  styleVisible.value = true
  await loadMyStyles()
}

const selectStyle = (s) => {
  selectedStyleName.value = s.name
}

const applyStyle = () => {
  if (!selectedStyleName.value) return
  const s = systemStyles.value.find(x => x.name === selectedStyleName.value) ||
            myStyles.value.find(x => x.name === selectedStyleName.value)
  if (s) {
    applyStyleShared(s)
    styleVisible.value = false
  }
}

const goToCreateStyle = () => {
  createStyleMode.value = true
  editingStyle.isEdit = false
  editingStyle.originalName = ''
  editingStyle.name = ''
  editingStyle.prompt = ''
  editingStyle.scope = ''
}

const goToEditStyle = (style) => {
  createStyleMode.value = true
  editingStyle.isEdit = true
  editingStyle.originalName = style.name
  editingStyle.name = style.name
  editingStyle.prompt = style.prompt
  editingStyle.scope = style.scope || ''
}

const goBackToList = () => {
  createStyleMode.value = false
}

const saveStyle = async () => {
  const name = editingStyle.name.trim()
  const prompt = editingStyle.prompt.trim()
  const scope = editingStyle.scope.trim()
  if (!name || !prompt || !scope) return
  if (name.length > 20 || prompt.length > 1000 || scope.length > 50) return
  try {
    if (editingStyle.isEdit) {
      await updateCustomStyle(editingStyle.originalName, {
        name,
        prompt,
        scope
      })
    } else {
      await addCustomStyle({
        name,
        prompt,
        scope
      })
    }
    createStyleMode.value = false
  } catch {
    // composable 已 message.error
  }
}

const deleteStyle = async (name) => {
  try {
    await removeCustomStyle(name)
    if (selectedStyleName.value === name) selectedStyleName.value = null
  } catch {
    // composable 已 message.error
  }
}

const togglePrompt = (idx) => {
  expandedPromptIdx.value = expandedPromptIdx.value === idx ? null : idx
}

const toggleLearnedPrompt = (idx) => {
  expandedLearnedIdx.value = expandedLearnedIdx.value === idx ? null : idx
}

const stylePresets = [
  { name: '产品评测', desc: '客观中立、参数对比', prompt: '你是客观的产品评测人：\n- 语气客观中立、有理有据\n- 结构：外观设计 → 核心性能 → 实际体验 → 优缺点总结\n- 必带参数对比表\n- 给出明确购买建议' },
  { name: '情感散文', desc: '细腻温暖、意象留白', prompt: '你是细腻的散文家：\n- 语气细腻、温暖，共情\n- 大量使用比喻、意象、留白\n- 第一人称叙述\n- 段落短而精，不要说教' },
  { name: '职场干货', desc: '专业务实、可执行', prompt: '你是资深职场导师：\n- 语气专业务实\n- 结构：行业痛点 → 核心方案 → 具体步骤\n- 必带可执行的 checklist\n- 避免假大空、避免鸡汤' },
  { name: '营销文案', desc: '紧迫感 + 利益点', prompt: '你是营销高手：\n- 开头制造紧迫感 / 共鸣痛点\n- 突出 3 个核心利益点\n- 必带强 CTA\n- 语气坚定、有说服力' }
]

// 模板
const templatePlatformTabs = [
  { key: 'all', label: '全部' },
  { key: 'wechat', label: '公众号' },
  { key: 'xiaohongshu', label: '小红书' },
  { key: 'toutiao', label: '今日头条' },
  { key: 'baijiahao', label: '百家号' },
  { key: 'zhihu', label: '知乎' },
  { key: 'douyin', label: '抖音图文' },
  { key: 'general', label: '通用风格' }
]
const templatePlatformTab = ref('all')

const filteredTemplates = computed(() => {
  if (templatePlatformTab.value === 'all') return allTemplates
  if (templatePlatformTab.value === 'general') return allTemplates.filter(t => ['business', 'marketing', 'academic', 'story', 'magazine', 'card', 'checklist', 'dark'].includes(t.key))
  return allTemplates.filter(t => t.key.startsWith(templatePlatformTab.value))
})

const selectedTemplateKey = ref('wechat')
const currentTemplate = computed(() => allTemplates.find(t => t.key === selectedTemplateKey.value) || allTemplates[0])
const templateVisible = ref(false)
const draftBoxVisible = ref(false)
const draftBoxKey = ref(0)

watch(draftBoxVisible, (val) => {
  if (val) draftBoxKey.value++
})

// 草稿列表
const draftList = computed(() => {
  const list = JSON.parse(localStorage.getItem('aichuangzuo_drafts') || '[]')
  console.log('draftList computed:', list)
  return list
})

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
      const t = allTemplates.find(x => x.key === draft.template.key)
      if (t) selectedTemplateKey.value = t.key
    } else {
      const t = allTemplates.find(x => x.name === draft.template)
      if (t) selectedTemplateKey.value = t.key
    }
  }
  draftBoxVisible.value = false
  message.success('已恢复草稿')
}

const deleteDraft = (id, e) => {
  e.stopPropagation()
  const drafts = JSON.parse(localStorage.getItem('aichuangzuo_drafts') || '[]')
  const idx = drafts.findIndex(d => d.id === id)
  if (idx > -1) {
    drafts.splice(idx, 1)
    localStorage.setItem('aichuangzuo_drafts', JSON.stringify(drafts))
  }
  draftBoxKey.value++
}

const currentTemplatePreview = computed(() => buildLargePreview(currentTemplate.value))

const openTemplateModal = () => {
  selectedTemplateKey.value = currentTemplate.value.key
  templateVisible.value = true
}

const selectTemplate = (t) => {
  selectedTemplateKey.value = t.key
}

const applyTemplate = () => {
  currentTemplate.value = allTemplates.find(t => t.key === selectedTemplateKey.value) || allTemplates[0]
  templateVisible.value = false
}

const formatDate = (dateStr) => {
  const d = new Date(dateStr)
  const month = d.getMonth() + 1
  const day = d.getDate()
  const hour = d.getHours().toString().padStart(2, '0')
  const min = d.getMinutes().toString().padStart(2, '0')
  return `${month}月${day}日 ${hour}:${min}`
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

const handlePreview = () => {
  router.push('/console/preview')
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
    loadMiniQueue()
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

/* 生成队列面板 */
.queue-panel {
  width: 320px;
  flex-shrink: 0;
  background: #fafafa;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
}

.queue-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-shrink: 0;
}

.queue-panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.queue-more-btn {
  background: none;
  border: none;
  color: #8c8c8c;
  font-size: 12px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s;
}

.queue-more-btn:hover {
  color: var(--color-primary);
  background: #fff0f2;
}

.queue-panel-more {
  margin-top: 12px;
  padding: 10px 12px;
  background: #fff;
  border: 1px dashed #ffd1d9;
  border-radius: 10px;
  font-size: 13px;
  color: #595959;
  text-align: center;
  line-height: 1.6;
}

.queue-panel-more-link {
  display: inline;
  padding: 0;
  border: none;
  background: transparent;
  color: #ff2442;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: color 0.2s;
}

.queue-panel-more-link:hover {
  color: #e61e3a;
  text-decoration: underline;
}

.queue-panel-empty {
  text-align: center;
  padding: 24px 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.queue-panel-empty .empty-icon {
  font-size: 32px;
  color: #d9d9d9;
  margin-bottom: 8px;
}

.queue-panel-empty .empty-text {
  color: #8c8c8c;
  font-size: 14px;
  margin-bottom: 4px;
}

.queue-panel-empty .empty-hint {
  color: #bfbfbf;
  font-size: 12px;
}

.queue-panel-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-x: hidden;
  flex: 1;
  padding-right: 4px;
}

.queue-panel-item {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 14px;
  transition: all 0.2s;
}

.queue-panel-item:hover {
  border-color: #ffd1d9;
  box-shadow: 0 2px 12px rgba(255, 36, 66, 0.08);
}

.queue-panel-item.generating {
  background: #fff;
  border-color: #ffd1d9;
}

.queue-panel-item.completed {
  background: #fff;
  border-color: #d9f7be;
}

.queue-panel-item.completed:hover {
  border-color: #b7eb8f;
  box-shadow: 0 2px 8px rgba(7, 193, 96, 0.08);
}

.queue-panel-item.queued {
  background: #fff;
  border-color: #e8e8e8;
}

.queue-panel-item.failed {
  background: #fff;
  border-color: #ffccc7;
}

.queue-item-top {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.queue-item-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.queue-panel-item.generating .queue-item-icon {
  background: #fff0f2;
  color: #ff2442;
}

.queue-panel-item.completed .queue-item-icon {
  background: #f0fff2;
  color: #07c160;
}

.queue-panel-item.queued .queue-item-icon {
  background: #f5f5f5;
  color: #8c8c8c;
}

.queue-panel-item.failed .queue-item-icon {
  background: #fff0f0;
  color: #ff4d4f;
}

.queue-item-info {
  flex: 1;
  min-width: 0;
}

.queue-item-title {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #1a1a1a;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 6px;
}

.queue-item-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.queue-item-status-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.queue-item-status-badge.generating {
  background: #ffeaea;
  color: #ff2442;
}

.queue-item-status-badge.completed {
  background: #e6fff2;
  color: #07c160;
}

.queue-item-status-badge.queued {
  background: #f5f5f5;
  color: #595959;
}

.queue-item-status-badge.failed {
  background: #fff0f0;
  color: #ff4d4f;
}

.queue-item-progress {
  margin-top: 12px;
}

.queue-item-progress .progress-bar {
  height: 6px;
  background: rgba(255, 36, 66, 0.15);
  border-radius: 3px;
  overflow: hidden;
}

.queue-item-progress .progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #ff2442, #ff6b81);
  border-radius: 3px;
  transition: width 0.3s;
}

.queue-item-progress .progress-hint {
  margin-top: 6px;
  font-size: 11px;
  color: #8c8c8c;
  line-height: 1.4;
}

.queue-item-footer {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #e8e8e8;
  display: flex;
  justify-content: flex-end;
}

.queue-export-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #fff;
  color: #ff2442;
  border: 1.5px solid #ff2442;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.queue-export-btn:hover {
  background: #fff0f2;
}

.queue-export-btn:active {
  background: #ffeaea;
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

/* 额度卡片 */
.quota-card {
  background: #fff0f2;
  border: 1px solid #ffbdc5;
  border-radius: 8px;
  padding: 10px 16px;
  margin-bottom: 16px;
  flex-shrink: 0;
}

.quota-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  margin-bottom: 8px;
}

.quota-text {
  color: #595959;
}

.quota-used {
  color: #ff4d4f;
}

.quota-remaining {
  color: #ff4d4f;
  font-weight: 600;
}

.quota-link {
  color: #ff4d4f;
  cursor: pointer;
  font-size: 12px;
}

.quota-link:hover {
  text-decoration: underline;
}

.quota-progress {
  height: 4px;
  background: #fff;
  border-radius: 2px;
  overflow: hidden;
}

.quota-progress-bar {
  height: 100%;
  border-radius: 2px;
  transition: width 0.3s, background 0.3s;
}

/* 模式切换 */
.mode-tabs {
  display: flex;
  gap: 8px;
  background: #f5f5f5;
  padding: 4px;
  border-radius: 10px;
  margin-bottom: 16px;
  flex-shrink: 0;
}

.mode-tab {
  flex: 1;
  padding: 10px;
  border: none;
  background: transparent;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.mode-tab.active {
  background: #fff;
  color: #1a1a1a;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

/* 选题 */
.topic-section {
  display: flex;
  flex-direction: column;
}

.topic-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 8px;
}

.topic-card {
  padding: 8px 10px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.topic-card:hover {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.topic-card.selected {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.topic-card.used {
  opacity: 0.5;
  cursor: not-allowed;
}

.topic-card.used:hover {
  border-color: #f0f0f0;
  background: transparent;
}

.topic-title {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 13px;
  line-height: 1.3;
  margin-bottom: 4px;
}

.topic-meta {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.topic-tag {
  font-size: 11px;
  color: var(--color-primary);
  background: #fff0f2;
  padding: 2px 6px;
  border-radius: 4px;
}

.topic-heat {
  font-size: 11px;
  color: #8c8c8c;
}

.topic-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.topic-hint {
  font-size: 13px;
  color: #8c8c8c;
}

.refresh-btn {
  padding: 6px 12px;
  background: #fff;
  color: var(--color-primary);
  border: 1px solid var(--color-primary);
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.refresh-btn:hover {
  background: var(--color-primary);
  color: #fff;
}

/* 输入区域 */
.input-section {
  margin-bottom: 12px;
}

.create-mode {
  display: flex;
  flex-direction: column;
}

.form-label {
  display: block;
  font-weight: 500;
  color: #262626;
  margin-bottom: 8px;
}

.required {
  color: #ff4d4f;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 15px;
  color: #1a1a1a;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}

.form-input::placeholder {
  color: #bfbfbf;
}

.form-textarea {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 15px;
  min-height: 100px;
  resize: vertical;
  color: #1a1a1a;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
  font-family: inherit;
}

.form-textarea:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}

.form-textarea::placeholder {
  color: #bfbfbf;
}

.input-hint {
  font-size: 13px;
  color: #8c8c8c;
  margin-top: 6px;
}

/* 设置工具栏 */
.settings-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  margin-bottom: 16px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  font-size: 14px;
  flex-shrink: 0;
  color: #595959;
}

.settings-label {
  color: #8c8c8c;
  font-size: 13px;
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

.settings-sep {
  color: #d9d9d9;
  font-size: 12px;
  user-select: none;
}

/* 操作栏 */
.action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.action-bar-left {
  display: flex;
  align-items: center;
  gap: 4px;
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

.action-primary {
  display: inline-flex;
  align-items: center;
  padding: 12px 28px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
  box-shadow: 0 2px 8px rgba(255, 36, 66, 0.25);
}

.action-primary:hover {
  background: var(--color-primary-hover);
  box-shadow: 0 4px 12px rgba(255, 36, 66, 0.35);
}

/* 平台选择 */
.platform-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  padding: 8px 0;
}

.platform-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px;
  border: 2px solid #e8e8e8;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.platform-item:hover {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.platform-item.selected {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.platform-name {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 15px;
}

.platform-desc {
  color: #8c8c8c;
  font-size: 12px;
  line-height: 1.5;
}

.modal-title-wrap {
  padding-right: 28px;
}

.modal-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.modal-subtitle {
  font-size: 13px;
  color: #8c8c8c;
}

/* 字数选择 */
.wc-tabs {
  display: flex;
  gap: 6px;
  padding: 0 0 12px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
  overflow-x: auto;
}

.wc-tab {
  padding: 8px 16px;
  border: 1px solid #d9d9d9;
  background: #fff;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.wc-tab.active {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: #fff0f2;
}

.wc-content {
  height: 300px;
  overflow-y: auto;
}

.wc-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.wc-item {
  padding: 14px;
  border: 2px solid #e8e8e8;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.wc-item:hover {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.wc-item.selected {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.wc-count {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.wc-label {
  font-size: 12px;
  color: #8c8c8c;
}

.wc-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.wc-item-wide {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px;
  border: 2px solid #e8e8e8;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.wc-item-wide:hover {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.wc-item-wide.selected {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.wc-item-left {
  display: flex;
  gap: 12px;
  align-items: center;
}

.wc-desc {
  font-size: 12px;
  color: #8c8c8c;
}

.wc-custom {
  padding: 8px 4px;
}

.wc-custom-display {
  font-size: 36px;
  font-weight: 700;
  color: var(--color-primary);
  text-align: center;
  margin: 16px 0;
}

.wc-custom-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 18px;
  text-align: center;
  box-sizing: border-box;
}

.wc-slider {
  width: 100%;
  margin-top: 16px;
  accent-color: var(--color-primary);
}

.wc-slider::-webkit-slider-thumb {
  appearance: none;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--color-primary);
  cursor: pointer;
  border: none;
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
}

.wc-slider::-moz-range-thumb {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--color-primary);
  cursor: pointer;
  border: none;
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
}

.wc-custom-hint {
  color: #8c8c8c;
  font-size: 12px;
  margin-top: 12px;
  text-align: center;
}

/* 风格选择 */
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
  gap: 12px;
}

.style-content {
  height: 60vh;
  overflow-y: auto;
}

.style-editor {
  height: 60vh;
  overflow-y: auto;
}

.style-card {
  padding: 16px;
  border: 2px solid #e8e8e8;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.style-card:hover {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.style-card.selected {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.style-card-title {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 15px;
  margin-bottom: 4px;
}

.style-card-desc {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 8px;
}

.style-card-scope {
  font-size: 12px;
  color: #1890ff;
  background: #e6f7ff;
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  margin-bottom: 8px;
}

.style-card-prompt {
  font-size: 12px;
  color: #595959;
  line-height: 1.6;
  white-space: pre-line;
}

.style-card-count {
  font-size: 12px;
  color: var(--color-primary);
}

.style-add-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  border: 2px dashed #d9d9d9;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  min-height: 100px;
}

.style-add-card:hover {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.style-add-icon {
  font-size: 24px;
  color: #8c8c8c;
  margin-bottom: 4px;
}

.style-add-text {
  font-size: 13px;
  color: #8c8c8c;
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

.style-prompt-toggle {
  font-size: 12px;
  color: var(--color-primary);
  cursor: pointer;
  margin-top: 8px;
}

.style-prompt-full {
  font-size: 12px;
  color: #595959;
  line-height: 1.6;
  margin-top: 8px;
  padding: 8px;
  background: #fafafa;
  border-radius: 6px;
  white-space: pre-wrap;
}

.style-prompt-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.style-action-btn {
  font-size: 12px;
  color: var(--color-primary);
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
}

.style-action-btn:hover {
  text-decoration: underline;
}

.style-del-btn {
  color: #ff4d4f;
}

.style-editor-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.style-editor-back {
  background: none;
  border: none;
  color: var(--color-primary);
  cursor: pointer;
  font-size: 14px;
}

.style-editor-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.style-editor-form {
  padding: 0;
}

.style-editor-field {
  margin-bottom: 16px;
}

.style-editor-label {
  display: block;
  font-size: 13px;
  color: #595959;
  margin-bottom: 8px;
  font-weight: 500;
}

.style-editor-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #1a1a1a;
  box-sizing: border-box;
}

.style-editor-input:focus {
  outline: none;
  border-color: var(--color-primary);
}

.style-editor-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #1a1a1a;
  resize: vertical;
  box-sizing: border-box;
  font-family: inherit;
}

.style-editor-textarea:focus {
  outline: none;
  border-color: var(--color-primary);
}

.style-editor-hint {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 6px;
}

.style-editor-presets {
  margin-bottom: 16px;
}

.style-editor-preset-label {
  font-size: 13px;
  color: #595959;
  margin-bottom: 8px;
}

.style-editor-preset-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.style-preset-card {
  padding: 8px 12px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.style-preset-card:hover {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.style-preset-title {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
}

.style-preset-desc {
  font-size: 11px;
  color: #8c8c8c;
}

.save-style-btn {
  width: 100%;
  padding: 10px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.save-style-btn:hover {
  background: var(--color-primary-hover);
}

/* 模板选择 */
:deep(.template-lib-modal .ant-modal-content) {
  width: 960px;
}

.template-tabs {
  display: flex;
  gap: 8px;
  padding: 0 0 14px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
  overflow-x: auto;
}

.template-tab {
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

.template-tab.active {
  border-color: #ff2442;
  background: #fff0f2;
  color: #ff2442;
  font-weight: 600;
}

.template-body {
  display: flex;
  gap: 16px;
}

.template-preview-pane {
  flex: 0 0 420px;
  background: #f5f5f5;
  border-radius: 10px;
  overflow: hidden;
  height: 480px;
  box-shadow: inset 0 0 0 1px rgba(0,0,0,0.05);
}

.template-list-pane {
  flex: 1;
  min-width: 0;
  height: 480px;
  overflow-y: auto;
  padding-right: 4px;
}

.template-row {
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

.template-row:hover {
  border-color: #ff2442;
  background: #fff0f2;
}

.template-row.selected {
  border-color: #ff2442;
  background: #fff0f2;
  box-shadow: 0 0 0 2px rgba(255, 36, 66, 0.25);
}

.template-row-name {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 14px;
  margin-bottom: 2px;
}

.template-row-desc {
  font-size: 12px;
  color: #8c8c8c;
}

.template-footer {
  padding: 12px 0 0;
  border-top: 1px solid #f0f0f0;
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.template-apply-btn {
  padding: 8px 24px;
  border-radius: 8px;
  border: none;
  background: #ff2442;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.template-apply-btn:hover {
  background: #e61e3a;
}

/* 草稿箱弹框 */
.draft-box-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
}

.draft-empty-icon {
  font-size: 40px;
  margin-bottom: 12px;
}

.draft-empty-text {
  color: #8c8c8c;
  font-size: 14px;
}

.draft-box-list {
  height: 400px;
  overflow-y: auto;
}

.draft-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 8px;
}

.draft-item:hover {
  border-color: #ff2442;
  background: #fff0f2;
}

.draft-title {
  font-weight: 600;
  font-size: 14px;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.draft-desc {
  font-size: 12px;
  color: #595959;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 320px;
}

.draft-meta {
  font-size: 12px;
  color: #8c8c8c;
}

.draft-del-btn {
  padding: 4px 8px;
  border: none;
  background: none;
  color: #8c8c8c;
  font-size: 12px;
  cursor: pointer;
}

.draft-del-btn:hover {
  color: #ff4d4f;
}

/* 导出弹框 */
.modal-title-text {
  font-size: 18px;
  font-weight: 600;
}

.export-modal-content {
  max-height: 70vh;
  overflow-y: auto;
  padding-bottom: 80px;
}

.export-article {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 24px;
}

.export-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 12px;
  line-height: 1.4;
}

.export-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee;
  flex-wrap: wrap;
}

.export-style-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  background: #fff0f3;
  color: #ff2442;
  border: 1px solid #ffd1d9;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
}

.export-body {
  font-size: 15px;
  line-height: 1.8;
  color: #262626;
}

.export-body h2 {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 24px 0 12px;
}

.export-publish-meta {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
  position: relative;
}

.export-meta-title {
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
  font-size: 15px;
}

.export-desc-input {
  width: 100%;
  min-height: 80px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  padding: 12px;
  font-size: 14px;
  line-height: 1.6;
  color: #262626;
  resize: vertical;
  box-sizing: border-box;
  font-family: inherit;
}

.export-desc-input:focus {
  outline: none;
  border-color: #ff2442;
}

.export-meta-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 10px;
}

.export-meta-btn {
  padding: 6px 14px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.export-meta-btn:hover {
  border-color: #ff2442;
  color: #ff2442;
}

.export-meta-btn.primary {
  background: #fff;
  border-color: #ff2442;
  color: #ff2442;
}

.export-meta-btn.primary:hover {
  background: #fff0f2;
}

.export-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.export-tag {
  padding: 4px 12px;
  background: #f5f5f5;
  border-radius: 14px;
  font-size: 13px;
  color: #595959;
}

.export-floating-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  border-top: 1px solid #eee;
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.06);
  padding: 10px 16px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  margin-top: 20px;
}

.export-float-btn {
  padding: 8px 14px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.15s;
}

.export-float-btn:hover {
  border-color: #ff2442;
  color: #ff2442;
}

.export-float-btn.primary {
  background: #ff2442;
  border-color: #ff2442;
  color: #fff;
}

.export-float-btn.primary:hover {
  background: #e61e3a;
}

.export-float-btn.outline {
  background: #fff;
  border-color: #ff2442;
  color: #ff2442;
}

.export-float-btn.outline:hover {
  background: #fff0f2;
}

.export-float-btn.danger {
  background: #fff;
  border-color: #ff4d4f;
  color: #ff4d4f;
}

.export-float-btn.danger:hover {
  background: #fff2f0;
}

/* ===== AI 标题优化弹框 ===== */

.title-opt-content {
  padding: 8px 4px 0;
}

.title-opt-original {
  background: #f5f5f5;
  border-radius: 8px;
  padding: 12px 14px;
  font-size: 14px;
  color: #1a1a1a;
  line-height: 1.5;
  margin-bottom: 20px;
}

.title-opt-original span {
  display: block;
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 4px;
}

.title-opt-section-label {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 10px;
}

.title-opt-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
}

.title-opt-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s;
}

.title-opt-item:hover {
  border-color: #ff2442;
  background: #fff8f9;
}

.title-opt-item.selected {
  border-color: #ff2442;
  background: #fff0f2;
}

.title-opt-radio {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 1px solid #d9d9d9;
  flex-shrink: 0;
  margin-top: 2px;
  transition: all 0.15s;
}

.title-opt-item.selected .title-opt-radio {
  border-color: #ff2442;
  background: #ff2442;
  box-shadow: inset 0 0 0 3px #fff;
}

.title-opt-item-text {
  font-size: 14px;
  color: #1a1a1a;
  line-height: 1.5;
}

.title-opt-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.title-opt-btn-cancel {
  padding: 8px 18px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  transition: all 0.15s;
}

.title-opt-btn-cancel:hover {
  border-color: #ff2442;
  color: #ff2442;
}

.title-opt-btn-confirm {
  padding: 8px 18px;
  background: #ff2442;
  border: 1px solid #ff2442;
  border-radius: 6px;
  font-size: 13px;
  color: #fff;
  cursor: pointer;
  transition: all 0.15s;
}

.title-opt-btn-confirm:hover {
  background: #e61e3a;
}

.title-opt-btn-confirm:disabled {
  background: #ffd1d9;
  border-color: #ffd1d9;
  cursor: not-allowed;
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

.topic-capsules {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
  flex-shrink: 0;
  min-width: 0;
}

.topic-capsules-label {
  font-size: 13px;
  color: #595959;
  flex-shrink: 0;
  line-height: 20px;
}

.topic-capsules-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.topic-capsules-grid > * {
  display: flex;
  min-width: 0;
}

.topic-capsule {
  width: 100%;
  padding: 8px 12px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 16px;
  font-size: 12px;
  color: #595959;
  cursor: pointer;
  transition: all 0.15s;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  box-sizing: border-box;
  text-align: left;
}

.topic-capsule:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: #fff0f2;
}

.topic-capsule.used {
  opacity: 0.5;
  cursor: not-allowed;
}

.refresh-capsule {
  align-self: flex-start;
  padding: 6px 16px;
  background: none;
  border: 1px solid #e8e8e8;
  border-radius: 16px;
  font-size: 12px;
  color: #8c8c8c;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
  margin-top: 4px;
}

.refresh-capsule:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.create-card-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .create-layout {
    flex-direction: column;
  }

  .queue-panel {
    width: 100%;
    box-sizing: border-box;
  }

  .queue-panel-empty {
    padding: 16px 0;
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

  .topic-capsules-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .refresh-capsule {
    align-self: center;
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

body[data-theme="dark"] .topic-capsule {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .topic-capsule:hover {
  background: #333;
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .queue-panel {
  background: #181818;
  box-shadow: none;
}

body[data-theme="dark"] .queue-panel-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .queue-panel-item {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .queue-panel-item:hover {
  border-color: #434343;
  box-shadow: none;
}

body[data-theme="dark"] .queue-panel-item.generating {
  border-color: rgba(255, 36, 66, 0.35);
}

body[data-theme="dark"] .queue-panel-item.completed {
  border-color: rgba(7, 193, 96, 0.35);
}

body[data-theme="dark"] .queue-panel-item.queued {
  border-color: #303030;
}

body[data-theme="dark"] .queue-panel-item.failed {
  border-color: rgba(255, 77, 79, 0.35);
}

body[data-theme="dark"] .queue-panel-item.generating .queue-item-icon {
  background: rgba(255, 36, 66, 0.15);
  color: #ff4d6a;
}

body[data-theme="dark"] .queue-panel-item.completed .queue-item-icon {
  background: rgba(7, 193, 96, 0.15);
  color: #4ade80;
}

body[data-theme="dark"] .queue-panel-item.queued .queue-item-icon {
  background: #2a2a2a;
  color: #a6a6a6;
}

body[data-theme="dark"] .queue-panel-item.failed .queue-item-icon {
  background: rgba(255, 77, 79, 0.15);
  color: #ff7875;
}

body[data-theme="dark"] .queue-item-status-badge.generating {
  background: rgba(255, 36, 66, 0.15);
  color: #ff4d6a;
}

body[data-theme="dark"] .queue-item-status-badge.completed {
  background: rgba(7, 193, 96, 0.15);
  color: #4ade80;
}

body[data-theme="dark"] .queue-item-status-badge.queued {
  background: #2a2a2a;
  color: #a6a6a6;
}

body[data-theme="dark"] .queue-item-status-badge.failed {
  background: rgba(255, 77, 79, 0.15);
  color: #ff7875;
}

body[data-theme="dark"] .queue-item-progress .progress-hint {
  color: #6a6a6a;
}

body[data-theme="dark"] .queue-item-footer {
  border-top-color: #303030;
}

body[data-theme="dark"] .queue-export-btn {
  background: transparent;
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .queue-export-btn:hover {
  background: rgba(255, 36, 66, 0.15);
}

body[data-theme="dark"] .queue-panel-more {
  background: transparent;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .queue-more-btn:hover {
  background: #2a2a2a;
}

body[data-theme="dark"] .quota-pill {
  background: #2a2a2a;
  color: #a6a6a6;
}

/* 导出/预览弹框深色 */
body[data-theme="dark"] :deep(.export-modal .ant-modal-content),
body[data-theme="dark"] :deep(.export-modal .ant-modal-header) {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] :deep(.export-modal .ant-modal-close) {
  color: #a6a6a6;
}

body[data-theme="dark"] :deep(.export-modal .ant-modal-close:hover) {
  background: #2a2a2a;
  color: #f0f0f0;
}

body[data-theme="dark"] .modal-title-text {
  color: #f0f0f0;
}

body[data-theme="dark"] .export-article {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .export-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .export-meta {
  color: #a6a6a6;
  border-bottom-color: #303030;
}

body[data-theme="dark"] .export-style-badge {
  background: rgba(255, 36, 66, 0.15);
  border-color: rgba(255, 36, 66, 0.35);
  color: #ff6b81;
}

body[data-theme="dark"] .export-body {
  color: #d9d9d9;
}

body[data-theme="dark"] .export-body h2 {
  color: #f0f0f0;
}

body[data-theme="dark"] .export-publish-meta {
  border-top-color: #303030;
}

body[data-theme="dark"] .export-meta-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .export-desc-input {
  background: #2a2a2a;
  border-color: #434343;
  color: #f0f0f0;
}

body[data-theme="dark"] .export-desc-input:focus {
  border-color: var(--color-primary);
}

body[data-theme="dark"] .export-meta-btn {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .export-meta-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .export-meta-btn.primary {
  background: transparent;
  border-color: var(--color-primary);
  color: var(--color-primary);
}

/* 发布平台 / 字数 / 模板弹框内元素（仍在 teleport 后保留 data-v） */
body[data-theme="dark"] .modal-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .modal-subtitle {
  color: #a6a6a6;
}

body[data-theme="dark"] .platform-item {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .platform-item:hover,
body[data-theme="dark"] .platform-item.selected {
  background: rgba(255, 36, 66, 0.15);
  border-color: var(--color-primary);
}

body[data-theme="dark"] .platform-name {
  color: #f0f0f0;
}

body[data-theme="dark"] .platform-desc {
  color: #a6a6a6;
}

body[data-theme="dark"] .wc-tabs {
  border-bottom-color: #303030;
}

body[data-theme="dark"] .wc-tab {
  background: #2a2a2a;
  border-color: #434343;
  color: #d9d9d9;
}

body[data-theme="dark"] .wc-tab.active {
  background: rgba(255, 36, 66, 0.15);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .wc-item,
body[data-theme="dark"] .wc-item-wide {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .wc-item:hover,
body[data-theme="dark"] .wc-item-wide:hover,
body[data-theme="dark"] .wc-item.selected,
body[data-theme="dark"] .wc-item-wide.selected {
  background: rgba(255, 36, 66, 0.15);
  border-color: var(--color-primary);
}

body[data-theme="dark"] .wc-count {
  color: #f0f0f0;
}

body[data-theme="dark"] .wc-label,
body[data-theme="dark"] .wc-desc {
  color: #a6a6a6;
}

body[data-theme="dark"] .wc-custom-input {
  background: #2a2a2a;
  border-color: #434343;
  color: #f0f0f0;
}

body[data-theme="dark"] .wc-custom-input:focus {
  border-color: var(--color-primary);
  outline: none;
}

/* 风格编辑内联表单 */
body[data-theme="dark"] .style-editor-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .style-editor-label {
  color: #f0f0f0;
}

body[data-theme="dark"] .style-editor-hint {
  color: #a6a6a6;
}

body[data-theme="dark"] .style-editor-input,
body[data-theme="dark"] .style-editor-textarea {
  background: #2a2a2a;
  border-color: #434343;
  color: #f0f0f0;
}

body[data-theme="dark"] .style-editor-input::placeholder,
body[data-theme="dark"] .style-editor-textarea::placeholder {
  color: #737373;
}

body[data-theme="dark"] .style-editor-input:focus,
body[data-theme="dark"] .style-editor-textarea:focus {
  border-color: var(--color-primary);
  outline: none;
}

body[data-theme="dark"] .style-editor-preset {
  background: #2a2a2a;
  border-color: #434343;
  color: #d9d9d9;
}

body[data-theme="dark"] .style-editor-preset:hover {
  background: rgba(255, 36, 66, 0.15);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .style-preset-card {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .style-preset-card:hover {
  background: rgba(255, 36, 66, 0.12);
  border-color: var(--color-primary);
}

body[data-theme="dark"] .style-preset-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .style-preset-desc {
  color: #a6a6a6;
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

body[data-theme="dark"] .style-card:hover,
body[data-theme="dark"] .style-card.selected {
  background: rgba(255, 36, 66, 0.12);
  border-color: var(--color-primary);
}

body[data-theme="dark"] .style-card-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .style-card-desc {
  color: #a6a6a6;
}

body[data-theme="dark"] .style-card-scope {
  color: #4dabf7;
  background: rgba(24, 144, 255, 0.15);
}

body[data-theme="dark"] .style-card-prompt {
  color: #d9d9d9;
}

body[data-theme="dark"] .style-add-card {
  border-color: #434343;
  background: transparent;
}

body[data-theme="dark"] .style-add-card:hover {
  border-color: var(--color-primary);
  background: rgba(255, 36, 66, 0.08);
}

body[data-theme="dark"] .style-add-icon,
body[data-theme="dark"] .style-add-text {
  color: #a6a6a6;
}

body[data-theme="dark"] .style-empty-text {
  color: #a6a6a6;
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

body[data-theme="dark"] .style-prompt-toggle {
  color: var(--color-primary);
}

/* 模板弹框内行 */
body[data-theme="dark"] .template-tabs {
  border-bottom-color: #303030;
}

body[data-theme="dark"] .template-tab {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .template-tab:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .template-tab.active {
  background: rgba(255, 36, 66, 0.15);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .template-preview-pane {
  background: #141414;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.05);
}

body[data-theme="dark"] .template-row {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .template-row:hover,
body[data-theme="dark"] .template-row.selected {
  background: rgba(255, 36, 66, 0.15);
  border-color: var(--color-primary);
  box-shadow: none;
}

body[data-theme="dark"] .template-row-name {
  color: #f0f0f0;
}

body[data-theme="dark"] .template-row-desc {
  color: #a6a6a6;
}

body[data-theme="dark"] .template-group-title {
  color: #a6a6a6;
}

body[data-theme="dark"] .template-footer {
  border-top-color: #303030;
}

/* 草稿箱 */
body[data-theme="dark"] .draft-empty-text {
  color: #a6a6a6;
}

body[data-theme="dark"] .draft-item {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .draft-item:hover {
  background: #2a2a2a;
  border-color: var(--color-primary);
}

body[data-theme="dark"] .draft-item-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .draft-item-time {
  color: #a6a6a6;
}

body[data-theme="dark"] .export-meta-btn.primary:hover {
  background: rgba(255, 36, 66, 0.15);
}

body[data-theme="dark"] .export-tag {
  background: #2a2a2a;
  color: #a6a6a6;
}

body[data-theme="dark"] .export-floating-bar {
  background: #1f1f1f;
  border-top-color: #303030;
  box-shadow: none;
}

body[data-theme="dark"] .export-float-btn {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .export-float-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .export-float-btn.primary {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}

body[data-theme="dark"] .export-float-btn.primary:hover {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
  color: #fff;
}

body[data-theme="dark"] .export-float-btn.outline {
  background: transparent;
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .export-float-btn.outline:hover {
  background: rgba(255, 36, 66, 0.15);
}

body[data-theme="dark"] .export-float-btn.danger {
  background: transparent;
  border-color: #ff4d4f;
  color: #ff4d4f;
}

body[data-theme="dark"] .export-float-btn.danger:hover {
  background: rgba(255, 77, 79, 0.15);
}

/* AI 标题优化弹框深色 */
body[data-theme="dark"] .title-opt-original {
  background: #2a2a2a;
  color: #f0f0f0;
}

body[data-theme="dark"] .title-opt-original span {
  color: #a6a6a6;
}

body[data-theme="dark"] .title-opt-section-label {
  color: #f0f0f0;
}

body[data-theme="dark"] .title-opt-item {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .title-opt-item:hover {
  background: #2a2a2a;
  border-color: var(--color-primary);
}

body[data-theme="dark"] .title-opt-item.selected {
  background: rgba(255, 36, 66, 0.15);
  border-color: var(--color-primary);
}

body[data-theme="dark"] .title-opt-radio {
  border-color: #434343;
}

body[data-theme="dark"] .title-opt-item.selected .title-opt-radio {
  border-color: var(--color-primary);
  background: var(--color-primary);
  box-shadow: inset 0 0 0 3px #1f1f1f;
}

body[data-theme="dark"] .title-opt-item-text {
  color: #f0f0f0;
}

body[data-theme="dark"] .title-opt-footer {
  border-top-color: #303030;
}

body[data-theme="dark"] .title-opt-btn-cancel {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .title-opt-btn-cancel:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .title-opt-btn-confirm:disabled {
  background: #434343;
  border-color: #434343;
  color: #737373;
}
</style>

<style>
/* 创作页导出预览弹框：暗色全局覆盖（弹框被 teleport 到 body，需非 scoped） */
body[data-theme="dark"] .export-modal .ant-modal-content,
body[data-theme="dark"] .export-modal .ant-modal-header,
body[data-theme="dark"] .export-modal .ant-modal-body {
  background: #1f1f1f !important;
  border-color: #303030 !important;
}

body[data-theme="dark"] .export-modal .ant-modal-close-x {
  color: #a6a6a6 !important;
}

body[data-theme="dark"] .export-modal .ant-modal-close:hover {
  background: #2a2a2a !important;
  color: #f0f0f0 !important;
}

body[data-theme="dark"] .export-modal .ant-modal-title,
body[data-theme="dark"] .export-modal .modal-title-text {
  color: #f0f0f0 !important;
}

body[data-theme="dark"] .export-modal .ant-modal-footer {
  background: #1f1f1f !important;
  border-top-color: #303030 !important;
}

/* 创作页 AI 标题优化弹框：暗色全局覆盖 */
body[data-theme="dark"] .title-opt-modal .ant-modal-content,
body[data-theme="dark"] .title-opt-modal .ant-modal-header,
body[data-theme="dark"] .title-opt-modal .ant-modal-body {
  background: #1f1f1f !important;
  border-color: #303030 !important;
}

body[data-theme="dark"] .title-opt-modal .ant-modal-close-x {
  color: #a6a6a6 !important;
}

body[data-theme="dark"] .title-opt-modal .ant-modal-close:hover {
  background: #2a2a2a !important;
  color: #f0f0f0 !important;
}

body[data-theme="dark"] .title-opt-modal .ant-modal-title {
  color: #f0f0f0 !important;
}

/* 创作页发布平台 / 字数 / 风格 / 草稿箱 / 模板弹框：暗色全局覆盖 */
body[data-theme="dark"] .platform-modal .ant-modal-content,
body[data-theme="dark"] .platform-modal .ant-modal-header,
body[data-theme="dark"] .word-count-modal .ant-modal-content,
body[data-theme="dark"] .word-count-modal .ant-modal-header,
body[data-theme="dark"] .style-modal .ant-modal-content,
body[data-theme="dark"] .style-modal .ant-modal-header,
body[data-theme="dark"] .draft-box-modal .ant-modal-content,
body[data-theme="dark"] .draft-box-modal .ant-modal-header,
body[data-theme="dark"] .template-modal .ant-modal-content,
body[data-theme="dark"] .template-modal .ant-modal-header {
  background: #1f1f1f !important;
  border-color: #303030 !important;
}

body[data-theme="dark"] .platform-modal .ant-modal-close-x,
body[data-theme="dark"] .word-count-modal .ant-modal-close-x,
body[data-theme="dark"] .style-modal .ant-modal-close-x,
body[data-theme="dark"] .draft-box-modal .ant-modal-close-x,
body[data-theme="dark"] .template-modal .ant-modal-close-x {
  color: #a6a6a6 !important;
}

body[data-theme="dark"] .platform-modal .ant-modal-close:hover,
body[data-theme="dark"] .word-count-modal .ant-modal-close:hover,
body[data-theme="dark"] .style-modal .ant-modal-close:hover,
body[data-theme="dark"] .draft-box-modal .ant-modal-close:hover,
body[data-theme="dark"] .template-modal .ant-modal-close:hover {
  background: #2a2a2a !important;
  color: #f0f0f0 !important;
}

body[data-theme="dark"] .platform-modal .ant-modal-title,
body[data-theme="dark"] .word-count-modal .ant-modal-title,
body[data-theme="dark"] .style-modal .ant-modal-title,
body[data-theme="dark"] .draft-box-modal .ant-modal-title,
body[data-theme="dark"] .template-modal .ant-modal-title {
  color: #f0f0f0 !important;
}

@media (max-width: 768px) {
  .template-modal-wrap {
    display: flex !important;
    flex-direction: column !important;
    align-items: flex-start !important;
    justify-content: flex-start !important;
  }

  .template-modal-wrap .template-modal,
  .template-modal-wrap.ant-modal-centered .template-modal {
    top: 0 !important;
    margin: 0 auto !important;
    width: 100% !important;
    max-width: 100vw !important;
    height: 100vh !important;
    max-height: 100vh !important;
    display: flex !important;
    flex-direction: column !important;
  }

  .template-modal .ant-modal-content {
    border-radius: 0;
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  .template-modal .ant-modal-body {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    padding: 16px;
  }

  .template-modal .template-tabs {
    flex-shrink: 0;
    padding-bottom: 10px;
    margin-bottom: 12px;
  }

  .template-modal .template-body {
    flex: 1;
    flex-direction: column;
    gap: 12px;
    overflow: hidden;
    min-height: 0;
  }

  /* 选择器放在上方，横向滚动 */
  .template-modal .template-list-pane {
    order: 1;
    display: flex;
    gap: 10px;
    height: auto;
    max-height: 130px;
    overflow-x: auto;
    overflow-y: hidden;
    padding-right: 0;
    padding-bottom: 4px;
    scrollbar-width: none;
  }

  .template-modal .template-list-pane::-webkit-scrollbar {
    display: none;
  }

  .template-modal .template-row {
    flex: 0 0 132px;
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
    padding: 12px;
    margin-bottom: 0;
  }

  .template-modal .template-row-name {
    font-size: 13px;
  }

  .template-modal .template-row-desc {
    font-size: 11px;
    line-height: 1.5;
  }

  /* 预览区放在下方，占据剩余空间 */
  .template-modal .template-preview-pane {
    order: 2;
    flex: 1;
    width: 100%;
    height: auto;
    max-height: 45vh;
    min-height: 200px;
    overflow-y: auto;
  }

  .template-modal .template-footer {
    order: 3;
    flex-shrink: 0;
    margin-top: 12px;
    padding: 12px 0 0;
    border-top: 1px solid #f0f0f0;
  }

  .template-modal .template-apply-btn {
    width: 100%;
  }
}

body[data-theme="dark"] .template-modal .template-footer {
  border-top-color: #303030;
}
</style>